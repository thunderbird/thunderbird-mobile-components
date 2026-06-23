package net.thunderbird.components.core.outcome

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import de.infix.testBalloon.framework.core.testSuite

val outcomeTest by testSuite("Outcome") {

    testSuite("factory functions") {
        test("success creates a successful outcome") {
            // Arrange
            val outcome: Outcome<Int, String> = Outcome.success(42)

            // Act
            val success = outcome as Outcome.Success

            // Assert
            assertThat(outcome.isSuccess).isTrue()
            assertThat(outcome.isFailure).isFalse()
            assertThat(success.data).isEqualTo(42)
        }

        test("failure creates a failed outcome") {
            // Arrange
            val outcome: Outcome<Int, String> = Outcome.failure("error")

            // Act
            val failure = outcome as Outcome.Failure

            // Assert
            assertThat(outcome.isFailure).isTrue()
            assertThat(outcome.isSuccess).isFalse()
            assertThat(failure.error).isEqualTo("error")
        }

        test("failure factory creates a failure without cause") {
            // Arrange
            val outcome: Outcome<Int, String> = Outcome.failure("error")

            // Act
            val failure = outcome as Outcome.Failure

            // Assert
            assertThat(failure.cause).isNull()
        }

        test("failure data class keeps cause") {
            // Arrange
            val cause = IllegalArgumentException("cause")

            // Act
            val failure = Outcome.Failure("error", cause)

            // Assert
            assertThat(failure.cause === cause).isTrue()
        }
    }

    testSuite("map") {
        test("transforms success value") {
            // Arrange
            val outcome: Outcome<Int, String> = Outcome.Success(7)

            // Act
            val mapped = outcome.map(
                transformSuccess = { value -> value * 2 },
                transformFailure = { error, _ -> "$error!" },
            )

            // Assert
            assertThat((mapped as Outcome.Success).data).isEqualTo(14)
        }

        test("transforms failure value and passes cause") {
            // Arrange
            val cause = IllegalStateException("cause")
            val outcome: Outcome<Int, String> = Outcome.Failure("error", cause)

            // Act
            val mapped = outcome.map(
                transformSuccess = { value -> value * 2 },
                transformFailure = { error, receivedCause ->
                    assertThat(receivedCause).isEqualTo(cause)
                    "$error-transformed"
                },
            )

            // Assert
            assertThat((mapped as Outcome.Failure).error).isEqualTo("error-transformed")
        }
    }

    testSuite("mapSuccess") {
        test("transforms success value") {
            // Arrange
            val outcome = Outcome.Success(3)

            // Act
            val mapped = outcome.mapSuccess { value -> value + 1 }

            // Assert
            assertThat((mapped as Outcome.Success).data).isEqualTo(4)
        }

        test("passes failure through unchanged") {
            // Arrange
            val cause = IllegalStateException("cause")
            val outcome = Outcome.Failure("failure", cause)

            // Act
            val mapped = outcome.mapSuccess { 999 }

            // Assert
            assertThat(mapped === outcome).isTrue()
            assertThat((mapped as Outcome.Failure).error).isEqualTo("failure")
            assertThat(mapped.cause === cause).isTrue()
        }
    }

    testSuite("flatMapSuccess") {
        test("flat maps success value") {
            // Arrange
            val outcome = Outcome.Success(10)

            // Act
            val mapped = outcome.flatMapSuccess { value ->
                if (value > 5) Outcome.Success("success") else Outcome.Failure("failure")
            }

            // Assert
            assertThat((mapped as Outcome.Success).data).isEqualTo("success")
        }

        test("flat maps success value to failure") {
            // Arrange
            val cause = IllegalArgumentException("cause")
            val outcome = Outcome.Success(1)

            // Act
            val mapped = outcome.flatMapSuccess {
                Outcome.Failure("failure", cause)
            }

            // Assert
            assertThat((mapped as Outcome.Failure).error).isEqualTo("failure")
            assertThat(mapped.cause === cause).isTrue()
        }

        test("passes failure through unchanged") {
            // Arrange
            val cause = IllegalStateException("cause")
            val outcome: Outcome<Int, String> = Outcome.Failure("failure", cause)

            // Act
            val mapped = outcome.flatMapSuccess { Outcome.Success("unused") }

            // Assert
            assertThat(mapped === outcome).isTrue()
            assertThat((mapped as Outcome.Failure).error).isEqualTo("failure")
            assertThat(mapped.cause === cause).isTrue()
        }
    }

    testSuite("mapFailure") {
        test("passes success through unchanged") {
            // Arrange
            val outcome = Outcome.Success("success")

            // Act
            val mapped = outcome.mapFailure { error: String, _ -> "$error?" }

            // Assert
            assertThat(mapped === outcome).isTrue()
            assertThat((mapped as Outcome.Success).data).isEqualTo("success")
        }

        test("transforms failure value and passes cause") {
            // Arrange
            val cause = RuntimeException("cause")
            val outcome = Outcome.Failure("fail", cause)

            // Act
            val mapped = outcome.mapFailure { error, receivedCause ->
                assertThat(receivedCause).isEqualTo(cause)
                error.length
            }

            // Assert
            assertThat((mapped as Outcome.Failure).error).isEqualTo(4)
        }
    }

    testSuite("handle") {
        test("calls only success callback") {
            // Arrange
            var successCalledWith: Int? = null
            var failureCalledWith: String? = null
            val outcome = Outcome.Success(5)

            // Act
            outcome.handle(
                onSuccess = { successCalledWith = it },
                onFailure = { failureCalledWith = it },
            )

            // Assert
            assertThat(successCalledWith).isEqualTo(5)
            assertThat(failureCalledWith).isNull()
        }

        test("calls only failure callback") {
            // Arrange
            var successCalledWith: Int? = null
            var failureCalledWith: String? = null
            val outcome: Outcome<Int, String> = Outcome.Failure("failure")

            // Act
            outcome.handle(
                onSuccess = { successCalledWith = it },
                onFailure = { failureCalledWith = it },
            )

            // Assert
            assertThat(successCalledWith).isNull()
            assertThat(failureCalledWith).isEqualTo("failure")
        }
    }

    testSuite("handleAsync") {
        test("calls only suspending success callback") {
            // Arrange
            var successCalledWith: Int? = null
            var failureCalledWith: String? = null
            val outcome = Outcome.Success(1)

            // Act
            outcome.handleAsync(
                onSuccess = { successCalledWith = it },
                onFailure = { failureCalledWith = it },
            )

            // Assert
            assertThat(successCalledWith).isEqualTo(1)
            assertThat(failureCalledWith).isNull()
        }

        test("calls only suspending failure callback") {
            // Arrange
            var successCalledWith: Int? = null
            var failureCalledWith: String? = null
            val outcome: Outcome<Int, String> = Outcome.Failure("failure")

            // Act
            outcome.handleAsync(
                onSuccess = { successCalledWith = it },
                onFailure = { failureCalledWith = it },
            )

            // Assert
            assertThat(successCalledWith).isNull()
            assertThat(failureCalledWith).isEqualTo("failure")
        }
    }

    testSuite("fold") {
        test("returns success result") {
            // Arrange
            val outcome: Outcome<Int, String> = Outcome.Success(10)

            // Act
            val result = outcome.fold(
                onSuccess = { value -> value * 3 },
                onFailure = { -1 },
            )

            // Assert
            assertThat(result).isEqualTo(30)
        }

        test("returns failure result") {
            // Arrange
            val outcome: Outcome<Int, String> = Outcome.Failure("oops")

            // Act
            val result = outcome.fold(
                onSuccess = { value -> value * 3 },
                onFailure = { error -> "$error!" },
            )

            // Assert
            assertThat(result).isEqualTo("oops!")
        }
    }
}
