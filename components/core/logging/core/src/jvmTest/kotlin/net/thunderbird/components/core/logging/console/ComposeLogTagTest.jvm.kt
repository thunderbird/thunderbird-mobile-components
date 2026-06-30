/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging.console

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import net.thunderbird.components.core.logging.LogEvent
import net.thunderbird.components.core.logging.LogLevel

val composeLogTagTest by testSuite("ComposeLogTag") {
    test("uses an explicit tag") {
        val tag = LogEvent(
            level = LogLevel.INFO,
            tag = "explicit",
            message = "message",
            timestamp = 0,
        ).composeTag(emptySet())

        assertEquals(expected = "explicit", actual = tag)
    }

    test("creates a tag from an anonymous caller") {
        val tag = object {
            fun composeTag(): String? = LogEvent(
                level = LogLevel.INFO,
                message = "message",
                timestamp = 0,
            ).composeTag(emptySet())
        }.composeTag()

        assertNotNull(tag)
    }
}
