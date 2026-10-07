/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.console

import android.util.Log
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import de.infix.testBalloon.framework.core.testSuite
import net.thunderbird.components.core.logging.LogEvent
import net.thunderbird.components.core.logging.LogLevel

val consoleLogSinkTest by testSuite("ConsoleLogSink") {
    test("retains its level") {
        assertThat(ConsoleLogSink(LogLevel.INFO).level).isEqualTo(LogLevel.INFO)
    }

    test("maps levels to Android priorities") {
        val levels = listOf(LogLevel.VERBOSE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR)

        assertThat(levels.map { formatAndroidLog(event(level = it), "TestTag", false).priority })
            .isEqualTo(listOf(Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR))
    }

    test("includes throwable stack trace") {
        val error = IllegalStateException("failure")

        val formatted = formatAndroidLog(
            event = event(message = "Unable to continue", throwable = error),
            tag = "TestTag",
            legacyTagLimit = false,
            stackTraceString = Throwable::stackTraceToString,
        )

        assertThat(formatted.chunks.first()).isEqualTo("Unable to continue")
        assertThat(formatted.chunks.any { it.contains("IllegalStateException: failure") }).isEqualTo(true)
    }

    test("splits long messages without dropping their contents") {
        val message = "a".repeat(9000)

        val chunks = formatAndroidLog(event(message = message), "TestTag", false).chunks

        assertThat(chunks).hasSize(3)
        assertThat(chunks.joinToString("")).isEqualTo(message)
        assertThat(chunks.all { it.length <= 4000 }).isEqualTo(true)
    }

    test("preserves line breaks as separate logcat entries") {
        val formatted = formatAndroidLog(event(message = "first\nsecond"), "TestTag", false)

        assertThat(formatted.chunks).isEqualTo(listOf("first", "second"))
    }

    test("truncates tags only for older Android versions") {
        val longTag = "abcdefghijklmnopqrstuvwxyz"
        val logEvent = event(tag = longTag)

        assertThat(formatAndroidLog(logEvent, longTag, true).tag).isEqualTo(longTag.take(23))
        assertThat(formatAndroidLog(logEvent, longTag, false).tag).isEqualTo(longTag)
    }
}

private fun event(
    level: LogLevel = LogLevel.INFO,
    tag: String = "TestTag",
    message: String = "message",
    throwable: Throwable? = null,
) = LogEvent(level = level, tag = tag, message = message, throwable = throwable, timestamp = 0L)
