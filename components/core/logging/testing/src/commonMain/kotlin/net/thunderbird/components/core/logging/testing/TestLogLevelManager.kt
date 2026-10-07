/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.testing

import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.LogLevelManager

public class TestLogLevelManager : LogLevelManager {
    public var logLevel: LogLevel = LogLevel.VERBOSE
    override fun override(level: LogLevel) {
        logLevel = level
    }

    override fun restoreDefault() {
        logLevel = LogLevel.VERBOSE
    }

    override fun current(): LogLevel = logLevel
}
