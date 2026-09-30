/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.core.logging

/**
 * Manages the log level for the application.
 *
 * This interface provides a way to update the log level dynamically.
 * Implementations of this interface are responsible for persisting the log level
 * and notifying listeners of changes.
 */
interface LogLevelManager : LogLevelProvider {
    /**
     * Overrides the current log level.
     *
     * This function allows for a temporary change in the log level,
     * typically for debugging or specific operational needs.
     * The original log level can be restored by calling [restoreDefault] function
     *
     * @param level The new log level to set
     */
    fun override(level: LogLevel)

    /**
     * Restores the log level to its default value.
     *
     * The default log level is defined by the specific implementation of this interface.
     */
    fun restoreDefault()
}
