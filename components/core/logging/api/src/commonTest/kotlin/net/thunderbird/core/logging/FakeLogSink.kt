/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.core.logging

class FakeLogSink(
    override val level: LogLevel = LogLevel.VERBOSE,
) : LogSink {

    val events = mutableListOf<LogEvent>()

    override fun log(event: LogEvent) {
        events.add(event)
    }
}
