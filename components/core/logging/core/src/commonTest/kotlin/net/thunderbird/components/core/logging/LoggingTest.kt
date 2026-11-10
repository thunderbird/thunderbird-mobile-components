/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.logging

import assertk.assertThat
import assertk.assertions.isInstanceOf
import de.infix.testBalloon.framework.core.testSuite

val loggingTest by testSuite("Logging") {
    test("create returns a default logger") {
        assertThat(Logging.create()).isInstanceOf<DefaultLogger>()
    }
}
