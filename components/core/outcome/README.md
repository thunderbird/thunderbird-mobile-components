# Thunderbird Outcome Component

Outcome provides a small result type for operations that need a domain-specific failure type.

Use it when Kotlin `Result` is too restrictive because callers need a typed failure value instead
of only a `Throwable`.

## Dependency

```kotlin
dependencies {
    implementation("net.thunderbird.components.core:outcome:<version>")
}
```

## Usage

```kotlin
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.components.core.outcome.flatMapSuccess
import net.thunderbird.components.core.outcome.fold
import net.thunderbird.components.core.outcome.mapFailure
import net.thunderbird.components.core.outcome.mapSuccess

sealed interface ReadError {
    data object Missing : ReadError
    data object Invalid : ReadError
}

fun readValue(): Outcome<String, ReadError> {
    return Outcome.success("42")
}

val result = readValue()
    .mapSuccess { it.toInt() }
    .mapFailure { error, _ -> error }
    .flatMapSuccess { value ->
        if (value > 0) {
            Outcome.success(value)
        } else {
            Outcome.failure(ReadError.Invalid)
        }
    }
    .fold(
        onSuccess = { value -> "Read value: $value" },
        onFailure = { error -> "Could not read value: $error" },
    )
```

## Error Handling

Prefer domain-specific failure types over plain strings or exceptions:

```kotlin
sealed interface FileReadError {
    data object NotFound : FileReadError
    data object PermissionDenied : FileReadError
    data class Unknown(val message: String) : FileReadError
}
```

This keeps callers exhaustive and makes expected failure states visible at the API boundary.
