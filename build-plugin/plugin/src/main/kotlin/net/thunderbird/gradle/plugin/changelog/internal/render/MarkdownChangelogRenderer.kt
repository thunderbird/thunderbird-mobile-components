package net.thunderbird.gradle.plugin.changelog.internal.render

import kotlinx.datetime.LocalDate
import net.thunderbird.gradle.plugin.changelog.internal.Changelog
import net.thunderbird.gradle.plugin.changelog.internal.ChangelogEntry
import net.thunderbird.gradle.plugin.changelog.internal.Header
import net.thunderbird.gradle.plugin.changelog.internal.Release
import net.thunderbird.gradle.plugin.changelog.internal.SectionType

/**
 * Markdown renderer for the changelog.
 */
internal class MarkdownChangelogRenderer : ChangelogRenderer {

    override fun render(changelog: Changelog): String = buildString {
        append(renderHeader(changelog.header))
        append(renderReleases(changelog.releases))
        appendLine()
    }

    private fun renderHeader(header: Header): String = buildString {
        appendLine("# ${header.title}")
        if (header.descriptions.isNotEmpty()) {
            header.descriptions.forEach {
                appendLine()
                appendLine(it.text)
            }
        }
    }

    private fun renderReleases(releases: List<Release>): String = buildString {
        if (releases.isNotEmpty()) {
            for (release in releases) {
                appendLine()
                appendLine("## ${release.version} ${renderReleaseDate(release.date)}")
                if (release.sections.isNotEmpty()) {
                    append(renderSections(release.sections))
                }
            }
        } else {
            appendLine()
            appendLine("## Unreleased")
        }
    }

    private fun renderReleaseDate(date: LocalDate?): String {
        return if (date != null) "- $date" else ""
    }

    private fun renderSections(sections: Map<SectionType, List<ChangelogEntry>>): String = buildString {
        sections.forEach { (sectionType, entries) ->
            append(renderSection(sectionType, entries))
        }
    }

    private fun renderSection(type: SectionType, entries: List<ChangelogEntry>): String = buildString {
        appendLine()
        appendLine("### ${type.header}")
        if (entries.isNotEmpty()) {
            appendLine()
            entries.forEach { entry ->
                if (entry.text.isNotBlank()) {
                    appendLine("- ${entry.text}")
                }
            }
        }
    }
}
