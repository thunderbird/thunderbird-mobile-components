/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val logSinkTest by testSuite("LogSink") {

    test("canLog should return true for same level") {
        // Arrange
        val testSubject = TestLogSink(LogLevel.INFO)

        // Act && Assert
        assertTrue { testSubject.canLog(LogLevel.INFO) }
    }

    test("canLog should return false for level below sink level") {
        // Arrange
        val testSubject = TestLogSink(LogLevel.INFO)

        // Act && Assert
        assertFalse { testSubject.canLog(LogLevel.DEBUG) }
    }

    test("canLog should return true for level above sink level") {
        // Arrange
        val testSubject = TestLogSink(LogLevel.INFO)

        // Act && Assert
        assertTrue { testSubject.canLog(LogLevel.WARN) }
    }
}

private class TestLogSink(
    override val level: LogLevel,
) : LogSink {
    override fun log(event: LogEvent) = Unit
}
