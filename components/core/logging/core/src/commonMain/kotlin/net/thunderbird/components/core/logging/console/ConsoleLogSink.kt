/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.console

import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.LogSink

/**
 * A [LogSink] implementation that logs messages to the console.
 *
 * This sink uses the platform-specific implementations to handle logging.
 *
 * @param level The minimum [LogLevel] for messages to be logged.
 */
public interface ConsoleLogSink : LogSink

/**
 * Creates a [ConsoleLogSink] with the specified log level.
 *
 * @param level The minimum [LogLevel] for messages to be logged.
 * @return A new instance of [ConsoleLogSink].
 */
public expect fun ConsoleLogSink(
    level: LogLevel = LogLevel.INFO,
): ConsoleLogSink
