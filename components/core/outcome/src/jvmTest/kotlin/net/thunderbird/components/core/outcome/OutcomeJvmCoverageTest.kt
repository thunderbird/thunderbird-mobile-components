package net.thunderbird.components.core.outcome

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import de.infix.testBalloon.framework.core.testSuite

// Kover does not attribute normal Kotlin call-site coverage to inline function bodies.
// These JVM-only tests invoke the generated static methods directly so Gradle coverage sees both branches.
// Related issue: https://github.com/Kotlin/kotlinx-kover/issues/753
val outcomeJvmCoverageTest by testSuite("Outcome JVM coverage") {

    test("reflectively covers inline map branches") {
        // Arrange
        val success = Outcome.Success(7)
        val cause = IllegalStateException("cause")
        val failure: Outcome<Int, String> = Outcome.Failure("error", cause)

        // Act
        val mappedSuccess = invokeMap(
            outcome = success,
            transformSuccess = { value: Int -> value * 2 },
            transformFailure = { error: String, _ -> "$error!" },
        )
        val mappedFailure = invokeMap(
            outcome = failure,
            transformSuccess = { value: Int -> value * 2 },
            transformFailure = { error: String, receivedCause ->
                assertThat(receivedCause === cause).isTrue()
                "$error-transformed"
            },
        )

        // Assert
        assertThat((mappedSuccess as Outcome.Success).data).isEqualTo(14)
        assertThat((mappedFailure as Outcome.Failure).error).isEqualTo("error-transformed")
    }

    test("reflectively covers inline mapSuccess branches") {
        // Arrange
        val success = Outcome.Success(3)
        val failure = Outcome.Failure("failure")

        // Act
        val mappedSuccess = invokeMapSuccess(
            outcome = success,
            transformSuccess = { value: Int -> value + 1 },
        )
        val mappedFailure = invokeMapSuccess(
            outcome = failure,
            transformSuccess = { 999 },
        )

        // Assert
        assertThat((mappedSuccess as Outcome.Success).data).isEqualTo(4)
        assertThat(mappedFailure === failure).isTrue()
    }

    test("reflectively covers inline flatMapSuccess branches") {
        // Arrange
        val success = Outcome.Success(10)
        val failure: Outcome<Int, String> = Outcome.Failure("failure")

        // Act
        val mappedSuccess = invokeFlatMapSuccess(
            outcome = success,
            transformSuccess = { Outcome.Success("success") },
        )
        val mappedFailure = invokeFlatMapSuccess(
            outcome = failure,
            transformSuccess = { Outcome.Success("unused") },
        )

        // Assert
        assertThat((mappedSuccess as Outcome.Success).data).isEqualTo("success")
        assertThat(mappedFailure === failure).isTrue()
    }

    test("reflectively covers inline mapFailure branches") {
        // Arrange
        val success = Outcome.Success("success")
        val cause = RuntimeException("cause")
        val failure = Outcome.Failure("fail", cause)

        // Act
        val mappedSuccess = invokeMapFailure(
            outcome = success,
            transformFailure = { error: String, _ -> "$error?" },
        )
        val mappedFailure = invokeMapFailure(
            outcome = failure,
            transformFailure = { error: String, receivedCause ->
                assertThat(receivedCause === cause).isTrue()
                error.length
            },
        )

        // Assert
        assertThat(mappedSuccess === success).isTrue()
        assertThat((mappedFailure as Outcome.Failure).error).isEqualTo(4)
    }

    test("reflectively covers inline fold branches") {
        // Arrange
        val success = Outcome.Success(10)
        val failure = Outcome.Failure("oops")

        // Act
        val successResult = invokeFold(
            outcome = success,
            onSuccess = { value: Int -> value * 3 },
            onFailure = { -1 },
        )
        val failureResult = invokeFold(
            outcome = failure,
            onSuccess = { value: Int -> value * 3 },
            onFailure = { error: String -> "$error!" },
        )

        // Assert
        assertThat(successResult).isEqualTo(30)
        assertThat(failureResult).isEqualTo("oops!")
    }
}

@Suppress("UNCHECKED_CAST")
private fun <IN_SUCCESS, IN_FAILURE, OUT_SUCCESS, OUT_FAILURE> invokeMap(
    outcome: Outcome<IN_SUCCESS, IN_FAILURE>,
    transformSuccess: (IN_SUCCESS) -> OUT_SUCCESS,
    transformFailure: (IN_FAILURE, Any?) -> OUT_FAILURE,
): Outcome<OUT_SUCCESS, OUT_FAILURE> {
    return invokeOutcomeKt("map", outcome, transformSuccess, transformFailure) as Outcome<OUT_SUCCESS, OUT_FAILURE>
}

@Suppress("UNCHECKED_CAST")
private fun <IN_SUCCESS, OUT_SUCCESS, FAILURE> invokeMapSuccess(
    outcome: Outcome<IN_SUCCESS, FAILURE>,
    transformSuccess: (IN_SUCCESS) -> OUT_SUCCESS,
): Outcome<OUT_SUCCESS, FAILURE> {
    return invokeOutcomeKt("mapSuccess", outcome, transformSuccess) as Outcome<OUT_SUCCESS, FAILURE>
}

@Suppress("UNCHECKED_CAST")
private fun <IN_SUCCESS, FAILURE, OUT_SUCCESS> invokeFlatMapSuccess(
    outcome: Outcome<IN_SUCCESS, FAILURE>,
    transformSuccess: (IN_SUCCESS) -> Outcome<OUT_SUCCESS, FAILURE>,
): Outcome<OUT_SUCCESS, FAILURE> {
    return invokeOutcomeKt("flatMapSuccess", outcome, transformSuccess) as Outcome<OUT_SUCCESS, FAILURE>
}

@Suppress("UNCHECKED_CAST")
private fun <SUCCESS, IN_FAILURE, OUT_FAILURE> invokeMapFailure(
    outcome: Outcome<SUCCESS, IN_FAILURE>,
    transformFailure: (IN_FAILURE, Any?) -> OUT_FAILURE,
): Outcome<SUCCESS, OUT_FAILURE> {
    return invokeOutcomeKt("mapFailure", outcome, transformFailure) as Outcome<SUCCESS, OUT_FAILURE>
}

@Suppress("UNCHECKED_CAST")
private fun <SUCCESS, FAILURE, R> invokeFold(
    outcome: Outcome<SUCCESS, FAILURE>,
    onSuccess: (SUCCESS) -> R,
    onFailure: (FAILURE) -> R,
): R {
    return invokeOutcomeKt("fold", outcome, onSuccess, onFailure) as R
}

private fun invokeOutcomeKt(
    methodName: String,
    outcome: Outcome<*, *>,
    vararg functions: Any,
): Any {
    val functionTypes = functions.map { it.javaClass.interfaces.single() }.toTypedArray()
    val method = outcomeKtClass.getDeclaredMethod(methodName, Outcome::class.java, *functionTypes)
    return method.invoke(null, outcome, *functions)
        ?: error("Reflective invocation of $methodName returned null.")
}

private val outcomeKtClass: Class<*> = Class.forName("net.thunderbird.components.core.outcome.OutcomeKt")
