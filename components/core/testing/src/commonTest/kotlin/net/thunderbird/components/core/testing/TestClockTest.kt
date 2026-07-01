/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.testing

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
internal class TestClockTest {
    @Test
    fun `returns the current time`() {
        val clock = TestClock(Instant.DISTANT_PAST)

        assertThat(clock.now()).isEqualTo(Instant.DISTANT_PAST)
    }

    @Test
    fun `changes the current time`() {
        val clock = TestClock(Instant.DISTANT_PAST)
        clock.changeTimeTo(Instant.DISTANT_FUTURE)

        assertThat(clock.now()).isEqualTo(Instant.DISTANT_FUTURE)
    }

    @Test
    fun `advances the current time`() {
        val clock = TestClock(Instant.DISTANT_PAST)
        clock.advanceTimeBy(1.milliseconds)

        assertThat(clock.now()).isEqualTo(Instant.DISTANT_PAST + 1.milliseconds)
    }
}
