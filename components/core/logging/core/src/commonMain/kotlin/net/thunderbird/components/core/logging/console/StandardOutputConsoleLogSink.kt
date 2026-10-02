/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.console

import net.thunderbird.components.core.logging.LogEvent
import net.thunderbird.components.core.logging.LogLevel

internal class StandardOutputConsoleLogSink(
    override val level: LogLevel,
) : ConsoleLogSink {
    override fun log(event: LogEvent) {
        println("[${event.level}] [${event.tag}] ${event.message}")
        event.throwable?.printStackTrace()
    }
}
