/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.composite

import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.LogLevelProvider
import net.thunderbird.components.core.logging.LogSink

/**
 * A [LogSink] that aggregates multiple [LogSink] and forwards log events to them.
 *
 * This [CompositeLogSink] is useful when you want to log messages to multiple destinations
 * (e.g., console, file, etc.) without having to manage each [LogSink] individually.
 *
 * It checks the log level of each event against its own level and forwards the event
 * to all managed sinks that can handle the event's level.
 *
 * @param level The minimum log level this sink will process. Log events with a lower priority will be ignored.
 * @param manager The [CompositeLogSinkManager] that manages the collection of sinks.
 */
public interface CompositeLogSink : LogSink {
    public val manager: CompositeLogSinkManager
}

/**
 * Creates a [CompositeLogSink] with the specified log level and manager.
 *
 * @param logLevelProvider The minimum [LogLevel] for messages to be logged.
 * @param manager The [CompositeLogSinkManager] that manages the collection of sinks.
 * @param sinks A list of [LogSink] instances to be managed by this composite sink.
 * @return A new instance of [CompositeLogSink].
 */
public fun CompositeLogSink(
    logLevelProvider: LogLevelProvider,
    manager: CompositeLogSinkManager = DefaultLogSinkManager(),
    sinks: List<LogSink> = emptyList(),
): CompositeLogSink {
    return DefaultCompositeLogSink(logLevelProvider, manager, sinks)
}
