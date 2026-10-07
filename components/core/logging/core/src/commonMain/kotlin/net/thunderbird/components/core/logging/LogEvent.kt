/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging

public typealias LogTag = String
public typealias LogMessage = String

/**
 * Represents a single log event
 *
 * @property level The [LogLevel] of the log event.
 * @property tag The [LogTag] identifying the log source.
 * @property message The [LogMessage] associated with the log event.
 * @property throwable An optional [Throwable] associated with the log event.
 * @property timestamp The timestamp of the log event in milliseconds.
 */
public data class LogEvent(
    public val level: LogLevel,
    public val tag: LogTag,
    public val message: LogMessage,
    public val throwable: Throwable? = null,
    public val timestamp: Long,
)
