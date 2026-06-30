/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.composite

import net.thunderbird.components.core.logging.LogEvent
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.LogLevelProvider
import net.thunderbird.components.core.logging.LogSink

internal class DefaultCompositeLogSink(
    private val logLevelProvider: LogLevelProvider,
    override val manager: CompositeLogSinkManager = DefaultLogSinkManager(),
    sinks: List<LogSink> = emptyList(),
) : CompositeLogSink {
    override val level: LogLevel get() = logLevelProvider.current()

    init {
        manager.addAll(sinks)
    }

    override fun log(event: LogEvent) {
        if (canLog(event.level)) {
            manager.getAll().forEach { sink ->
                if (sink.canLog(event.level)) {
                    sink.log(event)
                }
            }
        }
    }
}
