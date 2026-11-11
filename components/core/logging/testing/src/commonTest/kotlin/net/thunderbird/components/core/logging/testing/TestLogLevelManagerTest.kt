/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.testing

import assertk.assertThat
import assertk.assertions.isEqualTo
import de.infix.testBalloon.framework.core.testSuite
import net.thunderbird.components.core.logging.LogLevel

val testLogLevelManagerTest by testSuite("TestLogLevelManager") {
    test("overrides and restores the log level") {
        val manager = TestLogLevelManager()

        manager.override(LogLevel.ERROR)
        assertThat(manager.current()).isEqualTo(LogLevel.ERROR)

        manager.restoreDefault()
        assertThat(manager.current()).isEqualTo(LogLevel.VERBOSE)
    }
}
