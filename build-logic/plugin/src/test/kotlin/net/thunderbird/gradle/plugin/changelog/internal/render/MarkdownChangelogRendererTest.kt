package net.thunderbird.gradle.plugin.changelog.internal.render

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.gradle.plugin.changelog.internal.Changelog
import net.thunderbird.gradle.plugin.changelog.internal.ChangelogEntry
import net.thunderbird.gradle.plugin.changelog.internal.Header
import net.thunderbird.gradle.plugin.changelog.internal.Release
import net.thunderbird.gradle.plugin.changelog.internal.SectionType

class MarkdownChangelogRendererTest {
    private val renderer = MarkdownChangelogRenderer()

    @Test
    fun `render writes header releases sections and entries`() {
        // Arrange
        val changelog = Changelog(
            header = Header(
                title = "Changelog",
                descriptions = listOf(ChangelogEntry("All notable changes are documented here.")),
            ),
            releases = listOf(
                Release(
                    version = "Unreleased",
                    date = null,
                    sections = mapOf(
                        SectionType.Chores to
                            listOf(ChangelogEntry("update build-plugin ([#4](https://example.test/pull/4))")),
                    ),
                ),
            ),
        )

        // Act
        val markdown = renderer.render(changelog)

        // Assert
        assertThat(markdown).isEqualTo(
            """
            # Changelog

            All notable changes are documented here.

            ## Unreleased 

            ### Chores

            - update build-plugin ([#4](https://example.test/pull/4))


            """.trimIndent(),
        )
    }
}
