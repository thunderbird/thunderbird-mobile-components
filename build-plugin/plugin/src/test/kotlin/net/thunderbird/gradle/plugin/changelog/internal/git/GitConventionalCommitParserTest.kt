package net.thunderbird.gradle.plugin.changelog.internal.git

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import kotlin.test.Test
import net.thunderbird.gradle.plugin.changelog.internal.SectionType

class GitConventionalCommitParserTest {
    private val parser = GitConventionalCommitParser()
    private val repoUrl = "https://github.com/thunderbird/thunderbird-mobile-components"

    @Test
    fun `given simple commit when parsing then returns correct commit`() {
        // Arrange
        val message = "feat: add new feature"

        // Act
        val result = parser.parse(message)

        // Assert
        assertThat(result).isNotNull().all {
            prop(ConventionalCommit::type).isEqualTo(SectionType.Features)
            prop(ConventionalCommit::description).isEqualTo("add new feature")
            prop(ConventionalCommit::scope).isNull()
        }
    }

    @Test
    fun `given commit with scope when parsing then returns correct commit`() {
        // Arrange
        val message = "fix(ui): resolve crash"

        // Act
        val result = parser.parse(message)

        // Assert
        assertThat(result).isNotNull().all {
            prop(ConventionalCommit::type).isEqualTo(SectionType.BugFixes)
            prop(ConventionalCommit::description).isEqualTo("resolve crash")
            prop(ConventionalCommit::scope).isEqualTo("ui")
        }
    }

    @Test
    fun `given commit with PR number and repoUrl when parsing then links PR`() {
        // Arrange
        val message = "feat: add awesome feature (#123)"

        // Act
        val result = parser.parse(message, repoUrl)

        // Assert
        assertThat(result).isNotNull().all {
            prop(ConventionalCommit::type).isEqualTo(SectionType.Features)
            prop(
                ConventionalCommit::description,
            ).isEqualTo(
                "add awesome feature ([#123](https://github.com/thunderbird/thunderbird-mobile-components/pull/123))",
            )
        }
    }

    @Test
    fun `given commit with PR number and no repoUrl when parsing then does not link PR`() {
        // Arrange
        val message = "feat: add awesome feature (#123)"

        // Act
        val result = parser.parse(message, repoUrl = null)

        // Assert
        assertThat(result).isNotNull().prop(ConventionalCommit::description).isEqualTo("add awesome feature (#123)")
    }

    @Test
    fun `given commit with multiple PR numbers when parsing then links all PRs`() {
        // Arrange
        val message = "fix: fix issue (#456) and (#789)"

        // Act
        val result = parser.parse(message, repoUrl)

        // Assert
        assertThat(
            result,
        ).isNotNull().prop(
            ConventionalCommit::description,
        ).isEqualTo(
            "fix issue ([#456](https://github.com/thunderbird/thunderbird-mobile-components/pull/456)) and ([#789](https://github.com/thunderbird/thunderbird-mobile-components/pull/789))",
        )
    }

    @Test
    fun `given merge commit with PR number when parsing then links PR`() {
        // Arrange
        val message = "chore(build): update build-plugin (#4)"

        // Act
        val result = parser.parse(message, repoUrl)

        // Assert
        assertThat(result).isNotNull().all {
            prop(ConventionalCommit::type).isEqualTo(SectionType.Chores)
            prop(
                ConventionalCommit::description,
            ).isEqualTo(
                "update build-plugin ([#4](https://github.com/thunderbird/thunderbird-mobile-components/pull/4))",
            )
        }
    }

    @Test
    fun `given invalid commit when parsing then returns null`() {
        // Arrange
        val message = "not a conventional commit"

        // Act
        val result = parser.parse(message)

        // Assert
        assertThat(result).isNull()
    }
}
