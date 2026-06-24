package net.thunderbird.gradle.plugin.changelog.internal.parser

import net.thunderbird.gradle.plugin.changelog.internal.Changelog
import net.thunderbird.gradle.plugin.changelog.internal.ChangelogEntry
import net.thunderbird.gradle.plugin.changelog.internal.Header
import net.thunderbird.gradle.plugin.changelog.internal.Release
import net.thunderbird.gradle.plugin.changelog.internal.SectionType

internal class ChangelogParser(
    private val headerParser: HeaderParser = HeaderParser(),
    private val releaseParser: ReleaseParser = ReleaseParser(),
) {

    fun parse(changelogText: String): Changelog? {
        val lines = changelogText.lineSequence().toList()
        val headerLines = lines.takeWhile { !it.trim().startsWith(RELEASE_HEADER_PREFIX) }
        val releasesLines = lines.drop(headerLines.size)

        val header = headerParser.parse(headerLines) ?: return null
        val releases = parseReleases(releasesLines)

        return Changelog(
            header = header,
            releases = releases,
        )
    }

    private fun parseReleases(lines: List<String>): List<Release> {
        val releases = mutableListOf<Release>()
        val releaseBlocks = splitIntoReleaseBlocks(lines)

        for (block in releaseBlocks) {
            val release = releaseParser.parse(block) ?: continue
            releases.add(release)
        }

        return releases
    }

    private fun splitIntoReleaseBlocks(lines: List<String>): List<List<String>> {
        val blocks = mutableListOf<List<String>>()
        val releaseIndices = lines.mapIndexedNotNull { index, line ->
            if (line.trim().startsWith(RELEASE_HEADER_PREFIX)) index else null
        } + lines.size

        for (i in releaseIndices.indices) {
            val from = releaseIndices[i]
            val to = if (i + 1 < releaseIndices.size) releaseIndices[i + 1] else lines.size
            blocks.add(lines.subList(from, to))
        }
        return blocks
    }

    private companion object {
        const val RELEASE_HEADER_PREFIX = "## "
    }
}
