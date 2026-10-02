/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging

/**
 * A logging interface that provides methods for logging messages at specific log levels.
 */
public interface Logger {
    /**
     * Logs a message at the verbose log level.
     *
     * @param tag The [LogTag] identifying the log source.
     * @param throwable An optional throwable to log.
     * @param message Lambda that returns the [LogMessage] to log.
     */
    public fun verbose(
        tag: LogTag,
        throwable: Throwable? = null,
        message: () -> LogMessage,
    )

    /**
     * Logs a message at the debug log level.
     *
     * @param tag The [LogTag] identifying the log source.
     * @param throwable An optional throwable to log.
     * @param message Lambda that returns the [LogMessage] to log.
     */
    public fun debug(
        tag: LogTag,
        throwable: Throwable? = null,
        message: () -> LogMessage,
    )

    /**
     * Logs a message at the info log level.
     *
     * @param tag The [LogTag] identifying the log source.
     * @param throwable An optional throwable to log.
     * @param message Lambda that returns the [LogMessage] to log.
     */
    public fun info(
        tag: LogTag,
        throwable: Throwable? = null,
        message: () -> LogMessage,
    )

    /**
     * Logs a message at the warn log level.
     *
     * @param tag The [LogTag] identifying the log source.
     * @param throwable An optional throwable to log.
     * @param message Lambda that returns the [LogMessage] to log.
     */
    public fun warn(
        tag: LogTag,
        throwable: Throwable? = null,
        message: () -> LogMessage,
    )

    /**
     * Logs a message at the error log level.
     *
     * @param tag The [LogTag] identifying the log source.
     * @param throwable An optional throwable to log.
     * @param message Lambda that returns the [LogMessage] to log.
     */
    public fun error(
        tag: LogTag,
        throwable: Throwable? = null,
        message: () -> LogMessage,
    )
}
