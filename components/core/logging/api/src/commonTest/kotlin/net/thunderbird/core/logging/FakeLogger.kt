/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.core.logging

class FakeLogger : Logger {
    val events = mutableListOf<LogEvent>()

    override fun verbose(
        tag: String?,
        throwable: Throwable?,
        message: () -> String,
    ) {
        events.add(LogEvent(LogLevel.VERBOSE, tag, message(), throwable, TIMESTAMP))
    }

    override fun debug(
        tag: String?,
        throwable: Throwable?,
        message: () -> String,
    ) {
        events.add(LogEvent(LogLevel.DEBUG, tag, message(), throwable, TIMESTAMP))
    }

    override fun info(
        tag: String?,
        throwable: Throwable?,
        message: () -> String,
    ) {
        events.add(LogEvent(LogLevel.INFO, tag, message(), throwable, TIMESTAMP))
    }

    override fun warn(
        tag: String?,
        throwable: Throwable?,
        message: () -> String,
    ) {
        events.add(LogEvent(LogLevel.WARN, tag, message(), throwable, TIMESTAMP))
    }

    override fun error(
        tag: String?,
        throwable: Throwable?,
        message: () -> String,
    ) {
        events.add(LogEvent(LogLevel.ERROR, tag, message(), throwable, TIMESTAMP))
    }

    private companion object {
        const val TIMESTAMP = 0L
    }
}
