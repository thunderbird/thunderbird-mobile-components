/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.gradle.plugin.changelog.internal.git

import net.thunderbird.gradle.plugin.changelog.internal.SectionType

internal data class ConventionalCommit(
    val type: SectionType,
    val scope: String?,
    val description: String,
)
