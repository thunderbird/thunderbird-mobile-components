/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.file

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.LoggingErrorReporter

public actual fun FileLogSink(
    level: LogLevel,
    file: PlatformFile,
    errorReporter: LoggingErrorReporter,
): FileLogSink = BufferedFileLogSink(
    level = level,
    append = { content -> file.writeString(if (file.exists()) file.readString() + content else content) },
    copyTo = { destination -> file.copyTo(destination) },
    clear = { file.writeString("") },
    errorReporter = errorReporter,
)
