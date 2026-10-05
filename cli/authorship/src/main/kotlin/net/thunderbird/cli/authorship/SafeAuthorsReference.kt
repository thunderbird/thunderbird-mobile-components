/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import kotlinx.serialization.Serializable

@Serializable
internal data class SafeAuthorsReference(
    val repoUrl: String,
    val revision: String,
    val path: String = "safe-authors.json",
)
