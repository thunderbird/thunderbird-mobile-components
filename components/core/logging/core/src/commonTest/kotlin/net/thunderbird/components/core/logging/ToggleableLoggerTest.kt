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

val toggleableLoggerTest by testSuite("ToggleableLogger") {
    test("disabled logger drops all levels without evaluating messages") {
        val sink = FakeLogSink(LogLevel.VERBOSE)
        val logger = ToggleableLogger(DefaultLogger(sink), enabled = false)
        var evaluated = false
        val message = {
            evaluated = true
            "private message"
        }

        logger.verbose(message = message)
        logger.debug(message = message)
        logger.info(message = message)
        logger.warn(message = message)
        logger.error(message = message)

        assertThat(evaluated).isEqualTo(false)
        assertThat(sink.events).isEmpty()
    }

    test("enabling and disabling affects only this logger") {
        val sink = FakeLogSink(LogLevel.VERBOSE)
        val logger = ToggleableLogger(DefaultLogger(sink), enabled = false)
        val otherLogger = DefaultLogger(sink)

        logger.setEnabled(true)
        logger.info { "enabled" }
        logger.setEnabled(false)
        logger.info { "disabled" }
        otherLogger.info { "other" }

        assertThat(sink.events).hasSize(2)
    }
}
