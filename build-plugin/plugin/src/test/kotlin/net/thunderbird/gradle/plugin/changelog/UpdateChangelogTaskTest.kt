package net.thunderbird.gradle.plugin.changelog

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import java.io.File
import kotlin.test.Test
import net.thunderbird.gradle.plugin.changelog.internal.fs.FileHelper
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class UpdateChangelogTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `update writes changelog from merge commits and remains idempotent`() {
        // Arrange
        val repo = createRepositoryWithMergedComponentCommits()
        val task = createTask(repo)

        // Act
        task.update()
        task.update()

        // Assert
        val changelog = repo.componentChangelogFile.readText()
        assertThat(changelog).doesNotContain("- add changelog plugin", "- add versioning plugin")
        assertThat(Regex("^- update build-plugin ", RegexOption.MULTILINE).findAll(changelog).count()).isEqualTo(1)
        assertThat(changelog).contains(
            "- update build-plugin ([#4](https://github.com/thunderbird/thunderbird-mobile-components/pull/4))",
        )
    }

    @Test
    fun `update uses latest release tag as exact lower bound`() {
        // Arrange
        val repo = createRepositoryWithReleaseTag()
        val task = createTask(repo, taskName = "${UpdateChangelogTask.TASK_NAME}WithTag")

        // Act
        task.update()

        // Assert
        val changelog = repo.componentChangelogFile.readText()
        val unreleased = changelog.substringBefore("## 1.0.0")
        assertThat(unreleased).doesNotContain("- old released change")
        assertThat(unreleased).contains("- new unreleased change")
    }

    @Test
    fun `update fails when latest release has no matching git tag`() {
        // Arrange
        val repo = createRepositoryWithReleaseTag(createTag = false)
        val task = createTask(repo, taskName = "${UpdateChangelogTask.TASK_NAME}WithoutTag")

        // Act
        val failure = assertFailure { task.update() }

        // Assert
        failure.isInstanceOf<IllegalStateException>()
        failure.messageContains("No git tag found for latest changelog release '1.0.0'")
    }

    private fun createRepositoryWithMergedComponentCommits(): File {
        val repo = createGitRepository()
        val componentDir = repo.componentDir
        componentDir.mkdirs()
        repo.componentVersionFile.writeText(VERSION_PROPERTIES)
        componentDir.resolve("file.txt").writeText("initial")
        repo.runGit("add", ".")
        repo.runGit("commit", "-m", "Initial commit")

        repo.runGit("checkout", "-b", "feature/build-plugin")
        componentDir.resolve("file.txt").appendText("\nchangelog")
        repo.runGit("commit", "-am", "feat: add changelog plugin")
        componentDir.resolve("file.txt").appendText("\nversioning")
        repo.runGit("commit", "-am", "feat: add versioning plugin")

        repo.runGit("checkout", "-")
        repo.runGit("merge", "--no-ff", "feature/build-plugin", "-m", "chore(build): update build-plugin (#4)")
        return repo
    }

    private fun createRepositoryWithReleaseTag(createTag: Boolean = true): File {
        val repo = createGitRepository()
        repo.runGit("branch", "-M", "main")

        val componentDir = repo.componentDir
        componentDir.mkdirs()
        repo.componentVersionFile.writeText(VERSION_PROPERTIES)
        repo.componentChangelogFile.writeText(CHANGELOG_WITH_RELEASE)
        componentDir.resolve("file.txt").writeText("initial")
        repo.runGit("add", ".")
        repo.runGit("commit", "-m", "chore: initial")

        componentDir.resolve("file.txt").appendText("\nold")
        repo.runGit("commit", "-am", "fix: old released change")
        if (createTag) {
            repo.runGit("tag", "bom-1.0.0")
        }

        componentDir.resolve("file.txt").appendText("\nnew")
        repo.runGit("commit", "-am", "fix: new unreleased change")
        return repo
    }

    private fun createGitRepository(): File {
        val repo = temporaryFolder.newFolder("update-changelog-task-test")
        repo.runGit("init")
        repo.runGit("config", "user.email", "test@example.com")
        repo.runGit("config", "user.name", "Test User")
        repo.runGit("config", "commit.gpgsign", "false")
        return repo
    }

    private fun createTask(
        repo: File,
        taskName: String = UpdateChangelogTask.TASK_NAME,
    ): UpdateChangelogTask {
        val project = ProjectBuilder.builder()
            .withProjectDir(repo.componentDir)
            .build()

        return project.tasks.create(taskName, UpdateChangelogTask::class.java).apply {
            versionFile.set(repo.componentVersionFile)
            changelogFile.set(repo.componentChangelogFile)
            repoRootDir.set(repo)
            repoUrl.set("https://github.com/thunderbird/thunderbird-mobile-components")
        }
    }

    private fun File.runGit(vararg args: String) {
        val command = listOf("git", "-C", absolutePath) + args
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        check(exitCode == 0) {
            "Command failed ($exitCode): ${command.joinToString(" ")}\n$output"
        }
    }

    private val File.componentDir: File
        get() = resolve("components/bom")

    private val File.componentVersionFile: File
        get() = componentDir.resolve(FileHelper.VERSION_FILE)

    private val File.componentChangelogFile: File
        get() = componentDir.resolve(FileHelper.CHANGELOG_FILE)

    private companion object {
        private val VERSION_PROPERTIES = """
            MAJOR=1
            MINOR=0
            PATCH=0
            SNAPSHOT=false
        """.trimIndent()

        private val CHANGELOG_WITH_RELEASE = """
            # Changelog

            ## Unreleased

            ## [1.0.0] - 2026-06-18

            ### Bug Fixes

            - old released change

        """.trimIndent()
    }
}
