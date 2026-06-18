package net.thunderbird.gradle.plugin.changelog

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import java.io.File
import kotlin.test.Test
import net.thunderbird.gradle.plugin.changelog.internal.fs.FileHelper
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class FinalizeChangelogTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `finalizeChangelog moves Unreleased entries to release and creates empty Unreleased section`() {
        // Arrange
        val componentDir = createComponentDir(CHANGES_UNDER_UNRELEASED)
        val task = createTask(componentDir, releaseDate = "2026-06-18")

        // Act
        task.finalizeChangelog()

        // Assert
        val changelog = componentDir.resolve(FileHelper.CHANGELOG_FILE).readText()
        assertThat(changelog).contains(
            "## Unreleased",
            "## 0.1.0 - 2026-06-18",
            "- update build-plugin ([#4](https://github.com/thunderbird/thunderbird-mobile-components/pull/4))",
        )
        assertThat(changelog.substringAfter("## Unreleased").substringBefore("## 0.1.0").trim()).isEqualTo("")
    }

    @Test
    fun `finalizeChangelog accepts matching release version override`() {
        // Arrange
        val componentDir = createComponentDir(CHANGES_UNDER_UNRELEASED)
        val task = createTask(componentDir, releaseVersion = "0.1.0", releaseDate = "2026-06-18")

        // Act
        task.finalizeChangelog()

        // Assert
        val changelog = componentDir.resolve(FileHelper.CHANGELOG_FILE).readText()
        assertThat(changelog).contains("## 0.1.0 - 2026-06-18")
    }

    @Test
    fun `finalizeChangelog fails when release version override differs from version properties`() {
        // Arrange
        val componentDir = createComponentDir(CHANGES_UNDER_UNRELEASED)
        val task = createTask(componentDir, releaseVersion = "0.2.0", releaseDate = "2026-06-18")

        // Act
        val failure = assertFailure { task.finalizeChangelog() }

        // Assert
        failure.isInstanceOf<IllegalArgumentException>()
        failure.messageContains("releaseVersion '0.2.0' does not match version.properties '0.1.0'")
    }

    @Test
    fun `finalizeChangelog fails when release version already exists`() {
        // Arrange
        val componentDir = createComponentDir(CHANGELOG_WITH_EXISTING_RELEASE)
        val task = createTask(componentDir, releaseVersion = "0.1.0", releaseDate = "2026-06-18")

        // Act
        val failure = assertFailure { task.finalizeChangelog() }

        // Assert
        failure.isInstanceOf<IllegalArgumentException>()
        failure.messageContains("Release '0.1.0' already exists")
    }

    @Test
    fun `finalizeChangelog fails when Unreleased section is empty`() {
        // Arrange
        val componentDir = createComponentDir(EMPTY_UNRELEASED)
        val task = createTask(componentDir, releaseVersion = "0.1.0", releaseDate = "2026-06-18")

        // Act
        val failure = assertFailure { task.finalizeChangelog() }

        // Assert
        failure.isInstanceOf<IllegalArgumentException>()
        failure.messageContains("'Unreleased' section is empty")
    }

    @Test
    fun `finalizeChangelog fails when release version is blank`() {
        // Arrange
        val componentDir = createComponentDir(CHANGES_UNDER_UNRELEASED)
        val task = createTask(componentDir, releaseVersion = " ", releaseDate = "2026-06-18")

        // Act
        val failure = assertFailure { task.finalizeChangelog() }

        // Assert
        failure.isInstanceOf<IllegalArgumentException>()
        failure.messageContains("releaseVersion must not be blank")
    }

    private fun createComponentDir(changelog: String): File {
        val componentDir = temporaryFolder.newFolder("finalize-changelog-task-test")
        componentDir.resolve(FileHelper.VERSION_FILE).writeText(VERSION_PROPERTIES)
        componentDir.resolve(FileHelper.CHANGELOG_FILE).writeText(changelog)
        return componentDir
    }

    private fun createTask(
        componentDir: File,
        releaseVersion: String? = null,
        releaseDate: String,
    ): FinalizeChangelogTask {
        val project = ProjectBuilder.builder()
            .withProjectDir(componentDir)
            .build()
        return project.tasks.create(FinalizeChangelogTask.TASK_NAME, FinalizeChangelogTask::class.java).apply {
            changelogFile.set(componentDir.resolve(FileHelper.CHANGELOG_FILE))
            versionFile.set(componentDir.resolve(FileHelper.VERSION_FILE))
            if (releaseVersion != null) {
                this.releaseVersion.set(releaseVersion)
            }
            this.releaseDate.set(releaseDate)
        }
    }

    private companion object {
        private val VERSION_PROPERTIES = """
            MAJOR=0
            MINOR=1
            PATCH=0
        """.trimIndent()

        private val EMPTY_UNRELEASED = """
            # Changelog

            ## Unreleased

        """.trimIndent()

        private val CHANGES_UNDER_UNRELEASED = """
            # Changelog

            ## Unreleased

            ### Chores

            - update build-plugin ([#4](https://github.com/thunderbird/thunderbird-mobile-components/pull/4))

        """.trimIndent()

        private val CHANGELOG_WITH_EXISTING_RELEASE = """
            # Changelog

            ## Unreleased

            ### Chores

            - update build-plugin

            ## 0.1.0 - 2026-06-17

            ### Chores

            - previous release

        """.trimIndent()
    }
}
