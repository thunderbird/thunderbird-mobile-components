/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.components.core.testing

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** A mutable [Clock] for deterministic tests. */
@OptIn(ExperimentalTime::class)
public class TestClock(
    private var currentTime: Instant = Clock.System.now(),
) : Clock {
    override fun now(): Instant = currentTime

    /** Sets the current time to [time]. */
    public fun changeTimeTo(time: Instant) {
        currentTime = time
    }

    /** Advances the current time by [duration]. */
    public fun advanceTimeBy(duration: Duration) {
        currentTime += duration
    }
}
