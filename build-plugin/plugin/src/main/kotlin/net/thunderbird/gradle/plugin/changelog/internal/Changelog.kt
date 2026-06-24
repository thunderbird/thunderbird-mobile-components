package net.thunderbird.gradle.plugin.changelog.internal

import kotlinx.datetime.LocalDate

/**
 * Changelog model.
 */
internal data class Changelog(
    val header: Header,
    val releases: List<Release>,
)

/**
 * The header information of the changelog.
 */
internal data class Header(
    val title: String,
    val descriptions: List<ChangelogEntry>,
)

/**
 * A release in the changelog.
 */
internal data class Release(
    val version: String,
    val date: LocalDate?,
    val sections: Map<SectionType, List<ChangelogEntry>>,
)

/**
 * A single entry in a changelog section.
 */
internal data class ChangelogEntry(
    val text: String,
)

/**
 * Conventional sections used in this repository (1:1 with Conventional Commit types).
 */
internal enum class SectionType(val header: String) {
    Features("Features"), // feat
    BugFixes("Bug Fixes"), // fix
    Documentation("Documentation"), // docs
    Styles("Styles"), // style
    Refactoring("Refactoring"), // refactor (+ perf)
    Tests("Tests"), // test
    Chores("Chores"), // chore (+ build/ci/deps)
    Reverts("Reverts"), // revert
    ;

    companion object {
        private val byHeader = entries.associateBy { it.header }
        fun fromHeader(header: String): SectionType? = byHeader[header]
    }
}
