/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.console

import net.thunderbird.components.core.logging.LogEvent
import net.thunderbird.components.core.logging.LogLevel

public actual fun ConsoleLogSink(level: LogLevel): ConsoleLogSink = JvmConsoleLogSink(level)

private class JvmConsoleLogSink(
    override val level: LogLevel,
) : ConsoleLogSink {

    override fun log(event: LogEvent) {
        println("[${event.level}] ${composeMessage(event)}")
        event.throwable?.printStackTrace()
    }

    private fun composeMessage(event: LogEvent): String {
        return "[${event.tag}] ${event.message}"
    }
}
