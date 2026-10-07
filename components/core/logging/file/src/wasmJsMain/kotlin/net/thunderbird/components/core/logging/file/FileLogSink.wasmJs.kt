/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.file

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineDispatcher
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.LoggingErrorReporter

public actual fun FileLogSink(
    level: LogLevel,
    file: PlatformFile,
    errorReporter: LoggingErrorReporter,
    defaultDispatcher: CoroutineDispatcher,
): FileLogSink = throw UnsupportedOperationException(
    "File logging is not supported on WebAssembly because FileKit does not provide file writing there.",
)
