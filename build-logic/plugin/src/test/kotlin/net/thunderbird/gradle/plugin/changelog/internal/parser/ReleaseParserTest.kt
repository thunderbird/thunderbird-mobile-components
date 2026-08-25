package net.thunderbird.gradle.plugin.changelog.internal.parser

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.datetime.LocalDate
import net.thunderbird.gradle.plugin.changelog.internal.SectionType

class ReleaseParserTest {
    private val parser = ReleaseParser()

    @Test
    fun `parse handles rendered unreleased release with sections`() {
        // Arrange
        val lines = listOf(
            "## Unreleased ",
            "",
            "### Chores",
            "",
            "- update build-plugin ([#4](https://github.com/thunderbird/thunderbird-mobile-components/pull/4))",
        )

        // Act
        val release = parser.parse(lines)

        // Assert
        assertThat(release).isNotNull().given { parsed ->
            assertThat(parsed.version).isEqualTo("Unreleased")
            assertThat(parsed.date).isNull()
            assertThat(parsed.sections.getValue(SectionType.Chores).map { it.text }).isEqualTo(
                listOf(
                    "update build-plugin ([#4](https://github.com/thunderbird/thunderbird-mobile-components/pull/4))",
                ),
            )
        }
    }

    @Test
    fun `parse handles bracketed dated release`() {
        // Arrange
        val lines = listOf(
            "## [1.2.3] - 2026-06-18",
            "",
            "### Bug Fixes",
            "",
            "- fix crash",
        )

        // Act
        val release = parser.parse(lines)

        // Assert
        assertThat(release).isNotNull().given { parsed ->
            assertThat(parsed.version).isEqualTo("1.2.3")
            assertThat(parsed.date).isEqualTo(LocalDate(2026, 6, 18))
            assertThat(parsed.sections.getValue(SectionType.BugFixes).map { it.text }).isEqualTo(listOf("fix crash"))
        }
    }
}
