/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.testing

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertContains
import kotlin.test.assertEquals
import net.thunderbird.components.core.logging.LogLevel

private const val TAG = "TestLoggerTest"

val testLoggerTest by testSuite("TestLogger") {
    test("formats multiline messages and throwable without printing") {
        val logger = TestLogger()
        logger.info(tag = TAG) { "first line\nsecond line" }
        logger.error(tag = TAG, throwable = IllegalStateException("failure")) { "failed" }

        assertEquals(
            expected = "I: first line   \n   second line\n",
            actual = logger.formatForDump(logger.events[0]),
        )
        val errorOutput = logger.formatForDump(logger.events[1])
        assertContains(charSequence = errorOutput, other = "E: failed")
        assertContains(charSequence = errorOutput, other = "IllegalStateException: failure")
        assertContains(charSequence = errorOutput, other = "      ")
    }

    test("records every log level") {
        val logger = TestLogger()

        logger.verbose(tag = TAG) { "verbose" }
        logger.debug(tag = TAG) { "debug" }
        logger.info(tag = TAG) { "info" }
        logger.warn(tag = TAG) { "warn" }
        logger.error(tag = TAG) { "error" }

        assertEquals(
            expected = listOf(LogLevel.VERBOSE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR),
            actual = logger.events.map { it.level },
        )
    }
}
