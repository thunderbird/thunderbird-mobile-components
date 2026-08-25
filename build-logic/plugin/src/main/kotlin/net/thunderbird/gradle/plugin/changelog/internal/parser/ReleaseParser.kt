package net.thunderbird.gradle.plugin.changelog.internal.parser

import kotlinx.datetime.LocalDate
import net.thunderbird.gradle.plugin.changelog.internal.ChangelogEntry
import net.thunderbird.gradle.plugin.changelog.internal.Release
import net.thunderbird.gradle.plugin.changelog.internal.SectionType

internal class ReleaseParser {

    fun parse(lines: List<String>): Release? {
        val headerLine = lines.firstOrNull() ?: return null
        val match = RELEASE_HEADER_REGEX.toRegex().matchEntire(headerLine.trim())
            ?: return null

        val version = (match.groups["bracketedVersion"] ?: match.groups["plainVersion"])
            ?.value
            ?.trim()
            ?: return null
        val date = match.groups["date"]?.value?.let(LocalDate::parse)

        val sections = parseSections(lines.drop(1))

        return Release(
            version = version,
            date = date,
            sections = sections,
        )
    }

    private fun parseSections(lines: List<String>): Map<SectionType, List<ChangelogEntry>> {
        val sections = linkedMapOf<SectionType, MutableList<ChangelogEntry>>()
        val sectionBlocks = splitIntoSectionBlocks(lines)

        for (block in sectionBlocks) {
            val headerLine = block.firstOrNull() ?: continue
            val sectionType = parseSectionType(headerLine) ?: continue
            val entries = block.drop(1)
                .mapNotNull { parseSectionEntryOrNull(it) }
            if (entries.isNotEmpty()) {
                sections.getOrPut(sectionType) { mutableListOf() }.addAll(entries)
            }
        }

        return sections.mapValues { it.value.toList() }
    }

    private fun splitIntoSectionBlocks(lines: List<String>): List<List<String>> {
        val indices = mutableListOf<Int>()
        lines.forEachIndexed { idx, raw ->
            if (raw.trim().startsWith(SECTION_HEADER_PREFIX)) indices += idx
        }
        if (indices.isEmpty()) return emptyList()
        val ends = indices.drop(1) + lines.size
        return indices.zip(ends).map { (from, to) -> lines.subList(from, to) }
    }

    private fun parseSectionType(line: String): SectionType? {
        val header = line.trim().removePrefix(SECTION_HEADER_PREFIX).trim()
        return SectionType.fromHeader(header)
    }

    private fun parseSectionEntryOrNull(line: String): ChangelogEntry? {
        val trimmed = line.trim()
        if (!trimmed.startsWith(CHANGELOG_ENTRY_PREFIX)) return null
        val text = trimmed.removePrefix(CHANGELOG_ENTRY_PREFIX).trim()
        if (text.isEmpty()) return null
        return ChangelogEntry(text = text)
    }

    private companion object {
        const val SECTION_HEADER_PREFIX = "### "
        const val RELEASE_HEADER_REGEX =
            "^##\\s+(?:\\[(?<bracketedVersion>[^]]+)]|(?<plainVersion>[^-]+?))(?:\\s+-\\s+(?<date>\\d{4}-\\d{2}-\\d{2}))?\\s*$"
        const val CHANGELOG_ENTRY_PREFIX = "- "
    }
}
