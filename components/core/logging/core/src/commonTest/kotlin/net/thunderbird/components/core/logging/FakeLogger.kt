/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging

class FakeLogger : Logger {
    val events = mutableListOf<LogEvent>()

    override fun verbose(
        tag: LogTag,
        throwable: Throwable?,
        message: () -> String,
    ) {
        events.add(
            LogEvent(
                level = LogLevel.VERBOSE,
                tag = tag,
                message = message(),
                throwable = throwable,
                timestamp = TIMESTAMP,
            ),
        )
    }

    override fun debug(
        tag: LogTag,
        throwable: Throwable?,
        message: () -> String,
    ) {
        events.add(
            LogEvent(
                level = LogLevel.DEBUG,
                tag = tag,
                message = message(),
                throwable = throwable,
                timestamp = TIMESTAMP,
            ),
        )
    }

    override fun info(
        tag: LogTag,
        throwable: Throwable?,
        message: () -> String,
    ) {
        events.add(
            LogEvent(
                level = LogLevel.INFO,
                tag = tag,
                message = message(),
                throwable = throwable,
                timestamp = TIMESTAMP,
            ),
        )
    }

    override fun warn(
        tag: LogTag,
        throwable: Throwable?,
        message: () -> String,
    ) {
        events.add(
            LogEvent(
                level = LogLevel.WARN,
                tag = tag,
                message = message(),
                throwable = throwable,
                timestamp = TIMESTAMP,
            ),
        )
    }

    override fun error(
        tag: LogTag,
        throwable: Throwable?,
        message: () -> String,
    ) {
        events.add(
            LogEvent(
                level = LogLevel.ERROR,
                tag = tag,
                message = message(),
                throwable = throwable,
                timestamp = TIMESTAMP,
            ),
        )
    }

    private companion object {
        const val TIMESTAMP = 0L
    }
}
