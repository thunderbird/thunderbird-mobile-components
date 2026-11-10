/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import de.infix.testBalloon.framework.core.testSuite

val logSinkTest by testSuite("LogSink") {

    test("canLog should return true for same level") {
        // Arrange
        val testSubject = TestLogSink(LogLevel.INFO)

        // Act && Assert
        assertThat(testSubject.canLog(LogLevel.INFO)).isTrue()
    }

    test("canLog should return false for level below sink level") {
        // Arrange
        val testSubject = TestLogSink(LogLevel.INFO)

        // Act && Assert
        assertThat(testSubject.canLog(LogLevel.DEBUG)).isFalse()
    }

    test("canLog should return true for level above sink level") {
        // Arrange
        val testSubject = TestLogSink(LogLevel.INFO)

        // Act && Assert
        assertThat(testSubject.canLog(LogLevel.WARN)).isTrue()
    }
}

private class TestLogSink(
    override val level: LogLevel,
) : LogSink {
    override fun log(event: LogEvent) = Unit
}
