/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.core.logging.file

import com.eygraber.uri.Uri
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel

internal class JvmFileLogSink(
    override val level: LogLevel,
    fileName: String,
    fileLocation: String,
) : FileLogSink {

    override fun log(event: LogEvent) {
        println("[$level] ${composeMessage(event)}")
        event.throwable?.printStackTrace()
    }

    override suspend fun export(uri: Uri) {
        // TODO: Implementation https://github.com/thunderbird/thunderbird-android/issues/9435
    }

    override suspend fun flushAndCloseBuffer() {
        TODO("Not yet implemented")
    }

    private fun composeMessage(event: LogEvent): String {
        return if (event.tag != null) {
            "[${event.tag}] ${event.message}"
        } else {
            event.message
        }
    }
}
