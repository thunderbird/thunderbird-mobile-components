/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.testing

import kotlin.test.Test
import kotlin.test.assertEquals
import net.thunderbird.components.core.logging.LogLevel

class TestLogLevelManagerTest {
    @Test
    fun `overrides and restores the log level`() {
        val manager = TestLogLevelManager()

        manager.override(LogLevel.ERROR)
        assertEquals(expected = LogLevel.ERROR, actual = manager.current())

        manager.restoreDefault()
        assertEquals(expected = LogLevel.VERBOSE, actual = manager.current())
    }
}
