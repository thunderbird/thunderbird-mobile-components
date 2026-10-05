/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import kotlinx.serialization.Serializable

@Serializable
public data class FileStats(
    val path: String,
    val isSafe: Boolean,
    val totalLines: Int = 0,
    val isBinary: Boolean = false,
    val authorsBlame: Map<String, Int> = emptyMap(),
    val authorsCommits: Set<String> = emptySet(),
    val unsafeContributors: Set<String> = emptySet(),
)
