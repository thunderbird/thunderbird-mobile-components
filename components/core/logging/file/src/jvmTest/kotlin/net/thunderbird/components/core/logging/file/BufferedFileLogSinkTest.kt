/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.file

import de.infix.testBalloon.framework.core.testSuite
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import net.thunderbird.components.core.logging.LogEvent
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.testing.temporaryDirectoryFixture

@Suppress("InjectDispatcher", "UnnamedParameterUse")
val bufferedFileLogSinkTest by testSuite("BufferedFileLogSink") {
    temporaryDirectoryFixture(prefix = "file-log-sink-test-").asParameterForEach {
        test("flush writes events and export clears the log file") { directory ->
            val logFile = PlatformFile(File(directory.toString(), "log.txt"))
            val exportedFile = PlatformFile(File(directory.toString(), "exported.txt"))
            val sink = FileLogSink(
                level = LogLevel.INFO,
                file = logFile,
            )

            sink.log(
                LogEvent(
                    level = LogLevel.INFO,
                    tag = "Test",
                    message = "First event",
                    timestamp = 0,
                ),
            )

            sink.flush()
            assertContains(logFile.readString(), "First event")

            sink.exportAndClear(exportedFile)

            assertContains(exportedFile.readString(), "First event")
            assertEquals("", logFile.readString())
        }

        test("export preserves the source log") { directory ->
            val destination = PlatformFile(File(directory.toString(), "exported.txt"))
            val copiedDestinations = mutableListOf<PlatformFile>()
            val sink = BufferedFileLogSink(
                level = LogLevel.INFO,
                append = {},
                copyTo = copiedDestinations::add,
                clear = { error("export should not clear the log") },
                errorReporter = { throw it },
                coroutineContext = Dispatchers.Unconfined,
            )

            sink.export(destination)

            assertEquals(expected = listOf(destination), actual = copiedDestinations)
        }

        test("flushes automatically after the buffer is full") { _ ->
            val appendedContent = mutableListOf<String>()
            val sink = BufferedFileLogSink(
                level = LogLevel.INFO,
                append = appendedContent::add,
                copyTo = {},
                clear = {},
                errorReporter = { throw it },
                coroutineContext = Dispatchers.Unconfined,
            )

            repeat(5) { index ->
                sink.log(LogEvent(level = LogLevel.INFO, message = "event $index", timestamp = 0))
            }

            assertEquals(expected = 1, actual = appendedContent.size)
            assertContains(charSequence = appendedContent.single(), other = "event 4")
        }

        test("reports write errors") { _ ->
            val reportedErrors = mutableListOf<Throwable>()
            val sink = BufferedFileLogSink(
                level = LogLevel.INFO,
                append = { throw IllegalStateException("disk full") },
                copyTo = {},
                clear = {},
                errorReporter = reportedErrors::add,
                coroutineContext = Dispatchers.Unconfined + CoroutineExceptionHandler { _, _ -> },
            )

            repeat(5) { sink.log(LogEvent(level = LogLevel.INFO, message = "event", timestamp = 0)) }

            assertTrue(reportedErrors.single() is IllegalStateException)
        }
    }
}
