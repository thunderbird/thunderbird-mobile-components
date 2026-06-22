package net.thunderbird.components.core.outcome

/**
 * A sealed interface representing the outcome of an operation.
 *
 * @param SUCCESS The type of the value when the operation succeeds.
 * @param FAILURE The type of the error when the operation fails.
 */
public sealed interface Outcome<out SUCCESS, out FAILURE> {

    /**
     * A successful outcome with a value of type [SUCCESS].
     *
     * @param data The value of the successful outcome.
     */
    public data class Success<out SUCCESS>(public val data: SUCCESS) : Outcome<SUCCESS, Nothing>

    /**
     * A failed outcome with an error of type [FAILURE].
     *
     * @param error The error of the failed outcome.
     * @param cause The cause of the failed outcome.
     */
    public data class Failure<out FAILURE>(
        public val error: FAILURE,
        public val cause: Any? = null,
    ) : Outcome<Nothing, FAILURE>

    /**
     * Whether the outcome is a success.
     */
    public val isSuccess: Boolean
        get() = this is Success

    /**
     * Whether the outcome is a failure.
     */
    public val isFailure: Boolean
        get() = this is Failure

    public companion object {
        /**
         * Create a [Outcome.Success] outcome with the given value.
         *
         * @param data The value of the successful outcome.
         */
        public fun <SUCCESS> success(data: SUCCESS): Outcome<SUCCESS, Nothing> = Success(data)

        /**
         * Create a [Outcome.Failure] outcome with the given error.
         *
         * @param error The error of the failed outcome.
         */
        public fun <FAILURE> failure(error: FAILURE): Outcome<Nothing, FAILURE> = Failure(error)
    }
}

/**
 * Map the value and error of an [Outcome] to a new value.
 *
 * @param transformSuccess The function to transform the value of a [Outcome.Success] to a new value.
 * @param transformFailure The function to transform the value of a [Outcome.Failure] to a new value.
 */
public inline fun <IN_SUCCESS, IN_FAILURE, OUT_SUCCESS, OUT_FAILURE> Outcome<IN_SUCCESS, IN_FAILURE>.map(
    transformSuccess: (IN_SUCCESS) -> OUT_SUCCESS,
    transformFailure: (IN_FAILURE, Any?) -> OUT_FAILURE,
): Outcome<OUT_SUCCESS, OUT_FAILURE> {
    return when (this) {
        is Outcome.Success -> Outcome.Success(transformSuccess(data))
        is Outcome.Failure -> Outcome.Failure(transformFailure(error, cause))
    }
}

/**
 * Map the value of a [Outcome] to a new value.
 *
 * @param transformSuccess The function to transform the value of a [Outcome.Success] to a new value.
 */
public inline fun <IN_SUCCESS, OUT_SUCCESS, FAILURE> Outcome<IN_SUCCESS, FAILURE>.mapSuccess(
    transformSuccess: (IN_SUCCESS) -> OUT_SUCCESS,
): Outcome<OUT_SUCCESS, FAILURE> {
    return when (this) {
        is Outcome.Success -> Outcome.Success(transformSuccess(data))
        is Outcome.Failure -> this
    }
}

/**
 * Flat map the value and error of an [Outcome] to a new [Outcome].
 */
public inline fun <IN_SUCCESS, FAILURE, OUT_SUCCESS> Outcome<IN_SUCCESS, FAILURE>.flatMapSuccess(
    transformSuccess: (IN_SUCCESS) -> Outcome<OUT_SUCCESS, FAILURE>,
): Outcome<OUT_SUCCESS, FAILURE> {
    return when (this) {
        is Outcome.Success -> transformSuccess(data)
        is Outcome.Failure -> this
    }
}

/**
 * Map the error of a [Outcome] to a new value.
 *
 * @param transformFailure The function to transform the value of a [Outcome.Failure] to a new value.
 */
public inline fun <SUCCESS, IN_FAILURE, OUT_FAILURE> Outcome<SUCCESS, IN_FAILURE>.mapFailure(
    transformFailure: (IN_FAILURE, Any?) -> OUT_FAILURE,
): Outcome<SUCCESS, OUT_FAILURE> {
    return when (this) {
        is Outcome.Success -> this
        is Outcome.Failure -> Outcome.Failure(transformFailure(error, cause))
    }
}

/**
 * Handle the value of an [Outcome] and execute the given function.
 *
 * @param onSuccess The function to execute if the outcome is a [Outcome.Success].
 * @param onFailure The function to execute if the outcome is a [Outcome.Failure].
 */
public fun <SUCCESS, FAILURE> Outcome<SUCCESS, FAILURE>.handle(
    onSuccess: (SUCCESS) -> Unit,
    onFailure: (FAILURE) -> Unit,
) {
    when (this) {
        is Outcome.Success -> onSuccess(data)
        is Outcome.Failure -> onFailure(error)
    }
}

/**
 * Handle the value of an [Outcome] and execute the given function.
 *
 * @param onSuccess The function to execute if the outcome is a [Outcome.Success].
 * @param onFailure The function to execute if the outcome is a [Outcome.Failure].
 */
public suspend fun <SUCCESS, FAILURE> Outcome<SUCCESS, FAILURE>.handleAsync(
    onSuccess: suspend (SUCCESS) -> Unit,
    onFailure: suspend (FAILURE) -> Unit,
) {
    when (this) {
        is Outcome.Success -> onSuccess(data)
        is Outcome.Failure -> onFailure(error)
    }
}

/**
 * Fold the value of an [Outcome] to a new value.
 */
public inline fun <SUCCESS, FAILURE, R> Outcome<SUCCESS, FAILURE>.fold(
    onSuccess: (SUCCESS) -> R,
    onFailure: (FAILURE) -> R,
): R {
    return when (this) {
        is Outcome.Success -> onSuccess(data)
        is Outcome.Failure -> onFailure(error)
    }
}
