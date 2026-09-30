/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.core.logging

/**
 * Provides the current [LogLevel].
 *
 * This can be used to dynamically change the log level during runtime.
 */
fun interface LogLevelProvider {
    /**
     * Gets the current log level.
     *
     * @return The current log level.
     */
    fun current(): LogLevel
}
