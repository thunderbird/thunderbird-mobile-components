package net.thunderbird.gradle.plugin.changelog

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.messageContains
import kotlin.test.Test
import net.thunderbird.gradle.plugin.changelog.internal.fs.FileHelper
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class WriteReleaseNotesTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `writeReleaseNotes writes finalized release section`() {
        // Arrange
        val componentDir = createComponentDir(CHANGELOG)
        val outputFile = temporaryFolder.newFile("release-notes.md")
        val task = createTask(componentDir).apply {
            this.outputFile.set(outputFile)
        }

        // Act
        task.writeReleaseNotes()

        // Assert
        assertThat(outputFile.readText()).isEqualTo(
            """
            ## 1.2.3 - 2026-06-18

            ### Features

            - add feature

            ### Bug Fixes

            - fix bug

            """.trimIndent(),
        )
    }

    @Test
    fun `writeReleaseNotes accepts matching release version override`() {
        // Arrange
        val componentDir = createComponentDir(CHANGELOG)
        val outputFile = temporaryFolder.newFile("release-notes.md")
        val task = createTask(componentDir).apply {
            this.outputFile.set(outputFile)
            releaseVersion.set("1.2.3")
        }

        // Act
        task.writeReleaseNotes()

        // Assert
        assertThat(outputFile.readText()).contains("## 1.2.3 - 2026-06-18")
    }

    @Test
    fun `writeReleaseNotes fails when release section is missing`() {
        // Arrange
        val componentDir = createComponentDir(CHANGELOG_WITHOUT_RELEASE)
        val task = createTask(componentDir)

        // Act
        val failure = assertFailure { task.writeReleaseNotes() }

        // Assert
        failure.messageContains("Release '1.2.3' was not found")
    }

    @Test
    fun `writeReleaseNotes fails when release version override differs from version properties`() {
        // Arrange
        val componentDir = createComponentDir(CHANGELOG)
        val task = createTask(componentDir).apply {
            releaseVersion.set("2.0.0")
        }

        // Act
        val failure = assertFailure { task.writeReleaseNotes() }

        // Assert
        failure.messageContains("releaseVersion '2.0.0' does not match version.properties '1.2.3'")
    }

    private fun createComponentDir(changelog: String) = temporaryFolder.newFolder("component").apply {
        resolve(FileHelper.VERSION_FILE).writeText(VERSION_PROPERTIES)
        resolve(FileHelper.CHANGELOG_FILE).writeText(changelog)
    }

    private fun createTask(componentDir: java.io.File): WriteReleaseNotesTask {
        val project = ProjectBuilder.builder()
            .withProjectDir(componentDir)
            .build()

        return project.tasks.create(WriteReleaseNotesTask.TASK_NAME, WriteReleaseNotesTask::class.java).apply {
            changelogFile.set(componentDir.resolve(FileHelper.CHANGELOG_FILE))
            versionFile.set(componentDir.resolve(FileHelper.VERSION_FILE))
            outputFile.set(temporaryFolder.newFile("release-notes-${System.nanoTime()}.md"))
        }
    }

    private companion object {
        private val VERSION_PROPERTIES = """
            MAJOR=1
            MINOR=2
            PATCH=3
        """.trimIndent()

        private val CHANGELOG = """
            # Changelog

            ## Unreleased

            ## 1.2.3 - 2026-06-18

            ### Features

            - add feature

            ### Bug Fixes

            - fix bug
        """.trimIndent()

        private val CHANGELOG_WITHOUT_RELEASE = """
            # Changelog

            ## Unreleased
        """.trimIndent()
    }
}
