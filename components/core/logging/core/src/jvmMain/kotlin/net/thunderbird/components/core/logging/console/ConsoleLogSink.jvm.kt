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
        val tag = event.tag ?: event.composeTag(ignoredClasses = IGNORE_CLASSES)
        return if (tag != null) {
            "[$tag] ${event.message}"
        } else {
            event.message
        }
    }

    companion object {
        private val IGNORE_CLASSES = setOf(
            JvmConsoleLogSink::class.java.name,
            // Add other classes to ignore if needed
        )
    }
}
