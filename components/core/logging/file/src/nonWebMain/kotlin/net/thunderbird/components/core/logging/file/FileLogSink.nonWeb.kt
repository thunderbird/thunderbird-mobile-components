/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.file

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.writeString
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.LoggingErrorReporter

public actual fun FileLogSink(
    level: LogLevel,
    file: PlatformFile,
    errorReporter: LoggingErrorReporter,
    defaultDispatcher: CoroutineDispatcher,
): FileLogSink = BufferedFileLogSink(
    level = level,
    append = { content ->
        withContext(fileWriteDispatcher) {
            file.sink(append = true).buffered().use { it.writeString(content) }
        }
    },
    copyTo = { destination -> file.copyTo(destination) },
    clear = { file.writeString("") },
    errorReporter = errorReporter,
    coroutineContext = defaultDispatcher,
)
