/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import kotlinx.serialization.Serializable

@Serializable
public data class AuthorshipReport(
    val allSafe: Boolean,
    val sourceRepoUrl: String,
    val sourceCommit: String,
    val targetPaths: List<String>,
    val contributors: List<ContributorStats>,
    val filesStats: Map<String, FileStats>,
    val commitMessage: String,
)
