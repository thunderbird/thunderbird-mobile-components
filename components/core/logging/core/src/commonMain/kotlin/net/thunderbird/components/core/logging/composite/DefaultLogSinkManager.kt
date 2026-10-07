/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.composite

import net.thunderbird.components.core.logging.LogSink

/**
 * Default implementation of [CompositeLogSinkManager] that manages a collection of [LogSink] instances.
 */
internal class DefaultLogSinkManager : CompositeLogSinkManager {
    private var sinks: List<LogSink> = emptyList()

    override fun getAll(): List<LogSink> {
        return sinks
    }

    override fun addAll(sinks: List<LogSink>) {
        this.sinks = (this.sinks + sinks).distinct()
    }

    override fun add(sink: LogSink) {
        if (sink !in sinks) sinks = sinks + sink
    }

    override fun remove(sink: LogSink) {
        sinks = sinks - sink
    }

    override fun removeAll() {
        sinks = emptyList()
    }
}
