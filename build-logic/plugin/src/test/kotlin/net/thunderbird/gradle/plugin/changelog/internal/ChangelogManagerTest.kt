package net.thunderbird.gradle.plugin.changelog.internal

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.test.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class ChangelogManagerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `get returns default changelog when file does not exist`() {
        // Arrange
        val changelogFile = temporaryFolder.root.resolve("CHANGELOG.md")

        // Act
        val changelog = ChangelogManager(changelogFile).get()

        // Assert
        assertThat(changelog.header.title).isEqualTo("Changelog")
        assertThat(changelog.releases.single().version).isEqualTo("Unreleased")
    }

    @Test
    fun `update creates parent directories and writes renderable changelog`() {
        // Arrange
        val changelogFile = temporaryFolder.root.resolve("nested/component/CHANGELOG.md")
        val changelog = Changelog(
            header = Header("Changelog", listOf(ChangelogEntry("Description."))),
            releases = listOf(
                Release(
                    version = "Unreleased",
                    date = null,
                    sections = mapOf(SectionType.Features to listOf(ChangelogEntry("add feature"))),
                ),
            ),
        )

        // Act
        ChangelogManager(changelogFile).update(changelog)

        // Assert
        assertThat(changelogFile.exists()).isTrue()
        val parsed = ChangelogManager(changelogFile).get()
        assertThat(parsed).isEqualTo(changelog)
    }
}
