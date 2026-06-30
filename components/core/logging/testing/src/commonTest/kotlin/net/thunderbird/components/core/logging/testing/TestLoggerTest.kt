/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.testing

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import net.thunderbird.components.core.logging.LogLevel

val testLoggerTest by testSuite("TestLogger") {
    test("records every log level") {
        val logger = TestLogger()

        logger.verbose { "verbose" }
        logger.debug { "debug" }
        logger.info { "info" }
        logger.warn { "warn" }
        logger.error { "error" }

        assertEquals(
            expected = listOf(LogLevel.VERBOSE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR),
            actual = logger.events.map { it.level },
        )
    }

    test("dumps messages with and without a throwable") {
        val logger = TestLogger()
        logger.info { "first line\nsecond line" }
        logger.error(throwable = IllegalStateException("failure")) { "failed" }

        logger.dump()
    }
}
