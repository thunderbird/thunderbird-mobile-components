/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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
            val entries = parseEntries(block.drop(1))
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

    private fun parseEntries(lines: List<String>): List<ChangelogEntry> {
        val entries = mutableListOf<ChangelogEntry>()
        for (line in lines) {
            val trimmed = line.trim()
            if (line.startsWith(SUB_ENTRY_PREFIX) && entries.isNotEmpty()) {
                val text = trimmed.removePrefix(CHANGELOG_ENTRY_PREFIX).trim()
                if (text.isNotEmpty()) {
                    val previous = entries.last()
                    entries[entries.lastIndex] = previous.copy(subEntries = previous.subEntries + text)
                }
            } else if (line.startsWith(CHANGELOG_ENTRY_PREFIX)) {
                val text = trimmed.removePrefix(CHANGELOG_ENTRY_PREFIX).trim()
                if (text.isNotEmpty()) entries.add(ChangelogEntry(text))
            }
        }
        return entries
    }

    private companion object {
        const val SECTION_HEADER_PREFIX = "### "
        const val RELEASE_HEADER_REGEX =
            "^##\\s+(?:\\[(?<bracketedVersion>[^]]+)]|(?<plainVersion>[^-]+?))(?:\\s+-\\s+(?<date>\\d{4}-\\d{2}-\\d{2}))?\\s*$"
        const val CHANGELOG_ENTRY_PREFIX = "- "
        const val SUB_ENTRY_PREFIX = "  - "
    }
}
