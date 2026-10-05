/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import kotlinx.serialization.Serializable

@Serializable
public data class ContributorStats(
    val name: String,
    val email: String,
    val isSafe: Boolean,
    val affiliation: String,
    val commitCount: Int = 0,
    val blameLines: Int = 0,
    val commits: Set<String> = emptySet(),
) {
    public fun withIncrementedBlame(): ContributorStats = copy(blameLines = blameLines + 1)

    public fun withAddedCommit(commitHash: String): ContributorStats {
        val updatedCommits = commits + commitHash
        return copy(commits = updatedCommits, commitCount = updatedCommits.size)
    }
}
