/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.testing

import assertk.assertThat
import assertk.assertions.isEqualTo
import de.infix.testBalloon.framework.core.testSuite
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

val testClockTest by testSuite("TestClock") {
    test("returns the current time") {
        val clock = TestClock(Instant.DISTANT_PAST)

        assertThat(clock.now()).isEqualTo(Instant.DISTANT_PAST)
    }

    test("changes the current time") {
        val clock = TestClock(Instant.DISTANT_PAST)
        clock.changeTimeTo(Instant.DISTANT_FUTURE)

        assertThat(clock.now()).isEqualTo(Instant.DISTANT_FUTURE)
    }

    test("advances the current time") {
        val clock = TestClock(Instant.DISTANT_PAST)
        clock.advanceTimeBy(1.milliseconds)

        assertThat(clock.now()).isEqualTo(Instant.DISTANT_PAST + 1.milliseconds)
    }
}
