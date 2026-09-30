/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.core.logging.composite

import net.thunderbird.core.logging.LogSink

/**
 * Default implementation of [CompositeLogSinkManager] that manages a collection of [LogSink] instances.
 */
internal class DefaultLogSinkManager : CompositeLogSinkManager {
    private val sinks: MutableList<LogSink> = mutableListOf()

    override fun getAll(): List<LogSink> {
        return sinks.toList()
    }

    override fun addAll(sinks: List<LogSink>) {
        sinks.forEach {
            add(it)
        }
    }

    override fun add(sink: LogSink) {
        if (sink !in sinks) {
            sinks.add(sink)
        }
    }

    override fun remove(sink: LogSink) {
        if (sink in sinks) {
            sinks.remove(sink)
        }
    }

    override fun removeAll() {
        sinks.clear()
    }
}
