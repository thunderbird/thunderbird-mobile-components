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
import timber.log.Timber

val consoleLogSinkTest by testSuite("ConsoleLogSink") {

    test("shouldHaveCorrectLogLevel") {
        // Arrange
        val testSubject = ConsoleLogSink(LogLevel.INFO)

        // Act & Assert
        assertThat(testSubject.level).isEqualTo(LogLevel.INFO)
    }

    test("shouldLogMessages") {
        // Arrange
        val testTree = TestTree()
        Timber.plant(testTree)
        val eventVerbose = LogEvent(
            level = LogLevel.VERBOSE,
            tag = "TestTag",
            message = "This is a verbose message",
            throwable = null,
            timestamp = 0L,
        )
        val eventDebug = LogEvent(
            level = LogLevel.DEBUG,
            tag = "TestTag",
            message = "This is a debug message",
            throwable = null,
            timestamp = 0L,
        )
        val eventInfo = LogEvent(
            level = LogLevel.INFO,
            tag = "TestTag",
            message = "This is a info message",
            throwable = null,
            timestamp = 0L,
        )
        val eventWarn = LogEvent(
            level = LogLevel.WARN,
            tag = "TestTag",
            message = "This is a warning message",
            throwable = null,
            timestamp = 0L,
        )
        val eventError = LogEvent(
            level = LogLevel.ERROR,
            tag = "TestTag",
            message = "This is an error message",
            throwable = null,
            timestamp = 0L,
        )

        val testSubject = ConsoleLogSink(LogLevel.VERBOSE)

        // Act
        testSubject.log(eventVerbose)
        testSubject.log(eventDebug)
        testSubject.log(eventInfo)
        testSubject.log(eventWarn)
        testSubject.log(eventError)

        // Assert
        assertThat(testTree.events).hasSize(5)
        assertThat(testTree.events[0]).isEqualTo(eventVerbose)
        assertThat(testTree.events[1]).isEqualTo(eventDebug)
        assertThat(testTree.events[2]).isEqualTo(eventInfo)
        assertThat(testTree.events[3]).isEqualTo(eventWarn)
        assertThat(testTree.events[4]).isEqualTo(eventError)
    }
}

private class TestTree : Timber.DebugTree() {
    val events = mutableListOf<LogEvent>()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        events.add(
            LogEvent(
                level = mapPriorityToLogLevel(priority),
                tag = tag,
                message = message,
                throwable = t,
                timestamp = 0L,
            ),
        )
    }

    private fun mapPriorityToLogLevel(priority: Int): LogLevel {
        return when (priority) {
            Log.VERBOSE -> LogLevel.VERBOSE
            Log.DEBUG -> LogLevel.DEBUG
            Log.INFO -> LogLevel.INFO
            Log.WARN -> LogLevel.WARN
            Log.ERROR -> LogLevel.ERROR
            else -> throw IllegalArgumentException("Unknown log priority: $priority")
        }
    }
}
