package net.thunderbird.gradle.plugin.changelog.internal

import java.io.File
import kotlinx.datetime.LocalDate
import net.thunderbird.gradle.plugin.changelog.internal.parser.ChangelogParser
import net.thunderbird.gradle.plugin.changelog.internal.render.MarkdownChangelogRenderer

/**
 * ChangelogManager provides high-level operations to read and modify a component-local
 * changelog as structured data and write the changes back to disk.
 *
 * @param file The changelog file to manage.
 */
internal class ChangelogManager(
    val file: File,
) {

    private val parser = ChangelogParser()
    private val markdownRenderer = MarkdownChangelogRenderer()

    fun get(): Changelog {
        return read(file) ?: DEFAULT_CHANGELOG
    }

    fun update(changelog: Changelog) {
        val markdown = markdownRenderer.render(changelog)
        write(file, markdown)
    }

    fun getLatestRelease(changelog: Changelog): Release? {
        return changelog.releases
            .filter { it.version != "Unreleased" }
            .maxByOrNull { it.version }
    }

    private fun read(file: File): Changelog? {
        if (!file.exists()) return null
        val text = file.readText()
        return parser.parse(text)
    }

    private fun write(file: File, content: String) {
        if (!file.exists()) file.parentFile.mkdirs()
        file.writeText(content)
    }

    private companion object {
        val DEFAULT_CHANGELOG = Changelog(
            header = Header(
                title = "Changelog",
                descriptions =
                listOf(
                    ChangelogEntry(
                        "All notable changes to this component will be documented in this file.",
                    ),
                    ChangelogEntry(
                        "This project uses [Conventional Commits](https://www.conventionalcommits.org/) for commit messages.",
                    ),
                    ChangelogEntry(
                        "Changelog entries are derived from commit history and grouped by commit type.",
                    ),
                ),
            ),
            releases = listOf(
                Release(
                    version = "Unreleased",
                    date = null,
                    sections = mutableMapOf(),
                ),
            ),
        )
    }
}
