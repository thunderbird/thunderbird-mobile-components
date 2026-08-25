package net.thunderbird.gradle.plugin.changelog.internal.git

import net.thunderbird.gradle.plugin.changelog.internal.SectionType

internal data class ConventionalCommit(
    val type: SectionType,
    val scope: String?,
    val description: String,
)
