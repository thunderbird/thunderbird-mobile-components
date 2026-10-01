/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import de.infix.testBalloon.framework.core.testSuite

private const val TAG = "ToggleableLoggerTest"

val toggleableLoggerTest by testSuite("ToggleableLogger") {
    test("disabled logger drops all levels without evaluating messages") {
        val sink = FakeLogSink(LogLevel.VERBOSE)
        val logger = ToggleableLogger(DefaultLogger(sink), enabled = false)
        var evaluated = false
        val message = {
            evaluated = true
            "private message"
        }

        logger.verbose(tag = TAG, message = message)
        logger.debug(tag = TAG, message = message)
        logger.info(tag = TAG, message = message)
        logger.warn(tag = TAG, message = message)
        logger.error(tag = TAG, message = message)

        assertThat(evaluated).isEqualTo(false)
        assertThat(sink.events).isEmpty()
    }

    test("enabling and disabling affects only this logger") {
        val sink = FakeLogSink(LogLevel.VERBOSE)
        val logger = ToggleableLogger(DefaultLogger(sink), enabled = false)
        val otherLogger = DefaultLogger(sink)

        logger.setEnabled(true)
        logger.info(tag = TAG) { "enabled" }
        logger.setEnabled(false)
        logger.info(tag = TAG) { "disabled" }
        otherLogger.info(tag = TAG) { "other" }

        assertThat(sink.events).hasSize(2)
        assertThat(sink.events.map { it.tag }).isEqualTo(listOf(TAG, TAG))
    }
}
