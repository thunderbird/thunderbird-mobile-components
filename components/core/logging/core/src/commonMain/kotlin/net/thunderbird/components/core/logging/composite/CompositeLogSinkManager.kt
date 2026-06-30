/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.composite

import net.thunderbird.components.core.logging.LogSink

/**
 * CompositeLogSinkManager is responsible for managing a collection of [LogSink] instances.
 */
public interface CompositeLogSinkManager {

    /**
     * Retrieves all [LogSink] instances managed by this manager.
     *
     * @return A list of all sinks.
     */
    public fun getAll(): List<LogSink>

    /**
     * Adds a [LogSink] to the manager.
     *
     * @param sink The [LogSink] to add.
     */
    public fun add(sink: LogSink)

    /**
     * Adds multiple [LogSink] instances to the manager.
     *
     * @param sinks The list of [LogSink] to add.
     */
    public fun addAll(sinks: List<LogSink>)

    /**
     * Removes a [LogSink] from the manager.
     *
     * @param sink The [LogSink] to remove.
     */
    public fun remove(sink: LogSink)

    /**
     * Removes all [LogSink] instances from the manager.
     */
    public fun removeAll()
}
