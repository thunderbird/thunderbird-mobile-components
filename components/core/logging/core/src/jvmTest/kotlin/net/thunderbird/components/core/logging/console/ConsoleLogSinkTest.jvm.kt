/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.console

import de.infix.testBalloon.framework.core.testSuite
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import net.thunderbird.components.core.logging.LogEvent
import net.thunderbird.components.core.logging.LogLevel

private const val TAG = "TestTag"

val consoleLogSinkTest by testSuite("ConsoleLogSink") {

    test("shouldHaveCorrectLogLevel") {
        // Arrange
        val testSubject = ConsoleLogSink(LogLevel.INFO)

        // Act & Assert
        assertEquals(expected = LogLevel.INFO, actual = testSubject.level)
    }

    test("shouldLogMessages") {
        // Arrange
        val originalOut = System.out
        val outContent = ByteArrayOutputStream()
        PrintStream(outContent).use { printStream ->
            System.setOut(printStream)

            try {
                val eventInfo = LogEvent(
                    level = LogLevel.INFO,
                    tag = TAG,
                    message = "This is an info message",
                    throwable = null,
                    timestamp = 0L,
                )

                val testSubject = ConsoleLogSink(LogLevel.VERBOSE)

                // Act
                testSubject.log(eventInfo)

                // Assert
                val output = outContent.toString().trim()
                println("[DEBUG_LOG] Actual output: '$output'")

                val expectedOutput = "[INFO] [$TAG] This is an info message"
                println("[DEBUG_LOG] Expected output: '$expectedOutput'")

                assertEquals(expected = expectedOutput, actual = output)
            } finally {
                System.setOut(originalOut)
            }
        }
    }

    test("shouldLogMessagesWithThrowable") {
        val sink = ConsoleLogSink(LogLevel.INFO)

        sink.log(
            LogEvent(
                level = LogLevel.INFO,
                tag = TAG,
                message = "message with a throwable",
                throwable = IllegalStateException("failure"),
                timestamp = 0L,
            ),
        )
    }

    test("standardOutputSinkLogsTaggedEvents") {
        val sink = StandardOutputConsoleLogSink(LogLevel.INFO)

        sink.log(LogEvent(level = LogLevel.INFO, tag = TAG, message = "tagged", timestamp = 0L))
        sink.log(
            LogEvent(
                level = LogLevel.ERROR,
                tag = TAG,
                message = "error",
                throwable = IllegalStateException("failure"),
                timestamp = 0L,
            ),
        )
    }
}
