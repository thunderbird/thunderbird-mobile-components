/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.composite

import net.thunderbird.components.core.logging.LogEvent
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.LogSink

class FakeLogSink(override val level: LogLevel) : LogSink {

    val events = mutableListOf<LogEvent>()

    override fun log(event: LogEvent) {
        events.add(event)
    }
}
