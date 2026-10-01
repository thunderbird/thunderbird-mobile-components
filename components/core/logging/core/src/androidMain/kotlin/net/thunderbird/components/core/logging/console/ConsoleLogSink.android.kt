/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.console

import android.os.Build
import android.util.Log
import net.thunderbird.components.core.logging.LogEvent
import net.thunderbird.components.core.logging.LogLevel

public actual fun ConsoleLogSink(level: LogLevel): ConsoleLogSink = AndroidConsoleLogSink(level)

private const val MAX_LOG_LENGTH = 4000
private const val LEGACY_TAG_LENGTH = 23

private class AndroidConsoleLogSink(
    override val level: LogLevel,
) : ConsoleLogSink {

    override fun log(event: LogEvent) {
        val formatted = formatAndroidLog(
            event = event,
            tag = event.tag,
            legacyTagLimit = Build.VERSION.SDK_INT < Build.VERSION_CODES.O,
        )
        formatted.chunks.forEach { chunk -> Log.println(formatted.priority, formatted.tag, chunk) }
    }
}

internal data class FormattedAndroidLog(
    val priority: Int,
    val tag: String,
    val chunks: List<String>,
)

internal fun formatAndroidLog(event: LogEvent, tag: String, legacyTagLimit: Boolean): FormattedAndroidLog {
    val message = event.message + event.throwable?.let { "\n${it.stackTraceToString()}" }.orEmpty()
    val priority = when (event.level) {
        LogLevel.VERBOSE -> Log.VERBOSE
        LogLevel.DEBUG -> Log.DEBUG
        LogLevel.INFO -> Log.INFO
        LogLevel.WARN -> Log.WARN
        LogLevel.ERROR -> Log.ERROR
    }
    return FormattedAndroidLog(
        priority = priority,
        tag = if (legacyTagLimit) tag.take(LEGACY_TAG_LENGTH) else tag,
        chunks = message.split('\n').flatMap { line ->
            if (line.isEmpty()) listOf("") else line.chunked(MAX_LOG_LENGTH)
        },
    )
}
