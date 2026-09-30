/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.core.logging.file

import net.thunderbird.core.file.FileManager
import net.thunderbird.core.logging.LogLevel

actual fun FileLogSink(
    level: LogLevel,
    fileName: String,
    fileLocation: String,
    fileManager: FileManager,
): FileLogSink {
    return AndroidFileLogSink(
        level = level,
        fileName = fileName,
        fileLocation = fileLocation,
        fileManager = fileManager,
    )
}
