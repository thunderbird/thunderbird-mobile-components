/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging

import kotlin.test.Test
import kotlin.test.assertIs

class LoggingTest {
    @Test
    fun `create returns a default logger`() {
        assertIs<DefaultLogger>(Logging.create())
    }
}
