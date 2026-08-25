package net.thunderbird.gradle.plugin.changelog.internal.parser

import net.thunderbird.gradle.plugin.changelog.internal.ChangelogEntry
import net.thunderbird.gradle.plugin.changelog.internal.Header

internal class HeaderParser {

    fun parse(lines: List<String>): Header? {
        var title: String? = null
        val headerEntries = mutableListOf<ChangelogEntry>()

        for (raw in lines) {
            val line = raw.trim()
            when {
                line.isBlank() -> continue
                line.startsWith(HEADER_PREFIX) -> title = parseHeaderTitle(line)
                else -> headerEntries.add(parseHeaderEntry(line))
            }
        }

        if (title == null) return null

        return Header(
            title = title,
            descriptions = headerEntries,
        )
    }

    private fun parseHeaderTitle(line: String): String? {
        return line.removePrefix(HEADER_PREFIX).trim().ifBlank { null }
    }

    private fun parseHeaderEntry(line: String): ChangelogEntry {
        return ChangelogEntry(text = line.trim())
    }

    private companion object {
        const val HEADER_PREFIX = "# "
    }
}
