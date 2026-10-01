/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging

/** A logger that can be disabled without changing its sinks or log level. */
public class ToggleableLogger(
    private val delegate: Logger,
    enabled: Boolean = true,
) : Logger by delegate, LoggingControl {
    private var isEnabled: Boolean = enabled

    override fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    override fun verbose(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        if (isEnabled) delegate.verbose(tag, throwable, message)
    }

    override fun debug(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        if (isEnabled) delegate.debug(tag, throwable, message)
    }

    override fun info(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        if (isEnabled) delegate.info(tag, throwable, message)
    }

    override fun warn(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        if (isEnabled) delegate.warn(tag, throwable, message)
    }

    override fun error(tag: LogTag?, throwable: Throwable?, message: () -> LogMessage) {
        if (isEnabled) delegate.error(tag, throwable, message)
    }
}
