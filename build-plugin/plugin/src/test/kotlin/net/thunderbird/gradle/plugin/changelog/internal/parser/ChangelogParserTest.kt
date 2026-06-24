package net.thunderbird.gradle.plugin.changelog.internal.parser

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlin.test.Test
import net.thunderbird.gradle.plugin.changelog.internal.SectionType

class ChangelogParserTest {
    private val parser = ChangelogParser()

    @Test
    fun `parse handles full rendered changelog`() {
        // Arrange
        val content = """
            # Changelog

            All notable changes to this component will be documented in this file.

            ## Unreleased 

            ### Chores

            - update build-plugin ([#4](https://github.com/thunderbird/thunderbird-mobile-components/pull/4))
        """.trimIndent()

        // Act
        val changelog = parser.parse(content)

        // Assert
        assertThat(changelog).isNotNull().given { parsed ->
            assertThat(parsed.header.title).isEqualTo("Changelog")
            assertThat(parsed.header.descriptions.map { it.text }).isEqualTo(
                listOf("All notable changes to this component will be documented in this file."),
            )
            assertThat(parsed.releases.single().version).isEqualTo("Unreleased")
            assertThat(parsed.releases.single().sections.getValue(SectionType.Chores).map { it.text }).isEqualTo(
                listOf(
                    "update build-plugin ([#4](https://github.com/thunderbird/thunderbird-mobile-components/pull/4))",
                ),
            )
        }
    }
}
