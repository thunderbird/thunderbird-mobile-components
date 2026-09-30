/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.core.logging.composite

import net.thunderbird.core.logging.LogSink

class FakeCompositeLogSinkManager(
    val sinks: MutableList<LogSink> = mutableListOf(),
) : CompositeLogSinkManager {

    override fun getAll(): List<LogSink> = sinks

    override fun add(sink: LogSink) = Unit

    override fun addAll(sinks: List<LogSink>) {
        this.sinks.addAll(sinks)
    }

    override fun remove(sink: LogSink) = Unit

    override fun removeAll() = Unit
}
