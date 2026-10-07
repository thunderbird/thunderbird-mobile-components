/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging

import net.thunderbird.components.core.logging.composite.CompositeLogSink
import net.thunderbird.components.core.logging.console.ConsoleLogSink

/** Creates ready-to-use loggers from the built-in sinks. */
public object Logging {
    /** Creates a logger that writes to the console. */
    public fun create(
        level: LogLevel = LogLevel.INFO,
    ): Logger {
        return DefaultLogger(CompositeLogSink({ level }, sinks = listOf(ConsoleLogSink(level))))
    }
}
