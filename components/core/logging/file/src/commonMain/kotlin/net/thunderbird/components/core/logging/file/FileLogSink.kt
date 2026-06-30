/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.file

import io.github.vinceglb.filekit.PlatformFile
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.components.core.logging.LogEvent
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.LogSink
import net.thunderbird.components.core.logging.LoggingErrorReporter

private const val LOG_BUFFER_COUNT = 4

/** A [LogSink] that writes log events to a file. */
public interface FileLogSink : LogSink {
    /** Copies the collected log to [destination]. */
    public suspend fun export(destination: PlatformFile)

    /** Copies the collected log to [destination], then clears the source log. */
    public suspend fun exportAndClear(destination: PlatformFile)

    /** Writes all buffered log events to the log file. */
    public suspend fun flush()
}

/**
 * Creates a [FileLogSink] that writes to [file].
 *
 * FileKit must be initialized for platforms that require it before this factory is called. The
 * parent directory of [file] must already exist.
 */
public expect fun FileLogSink(
    level: LogLevel,
    file: PlatformFile,
    errorReporter: LoggingErrorReporter = LoggingErrorReporter { },
): FileLogSink

internal class BufferedFileLogSink(
    override val level: LogLevel,
    private val append: suspend (String) -> Unit,
    private val copyTo: suspend (PlatformFile) -> Unit,
    private val clear: suspend () -> Unit,
    private val errorReporter: LoggingErrorReporter,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : FileLogSink {
    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())
    private val accumulatedLogs = mutableListOf<String>()
    private val mutex = Mutex()

    override fun log(event: LogEvent) {
        coroutineScope.launch {
            mutex.withLock {
                accumulatedLogs += event.format()
                if (accumulatedLogs.size > LOG_BUFFER_COUNT) {
                    flushBuffer()
                }
            }
        }.invokeOnCompletion { error ->
            if (error != null) errorReporter.report(error)
        }
    }

    override suspend fun export(destination: PlatformFile) {
        mutex.withLock {
            flushBuffer()
            copyTo(destination)
        }
    }

    override suspend fun exportAndClear(destination: PlatformFile) {
        mutex.withLock {
            flushBuffer()
            copyTo(destination)
            clear()
        }
    }

    override suspend fun flush() {
        mutex.withLock {
            flushBuffer()
        }
    }

    private suspend fun flushBuffer() {
        if (accumulatedLogs.isNotEmpty()) {
            append(accumulatedLogs.joinToString(separator = "\n", postfix = "\n"))
            accumulatedLogs.clear()
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun LogEvent.format(): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val tagPrefix = tag?.let { "[$it] " }.orEmpty()
        val throwableText = throwable?.let { "\n${it.stackTraceToString()}" }.orEmpty()
        return "${LocalDateTime.Formats.ISO.format(dateTime)} [$level] $tagPrefix$message$throwableText"
    }
}
