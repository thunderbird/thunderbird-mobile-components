package net.thunderbird.gradle.plugin.changelog.internal.git

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.File
import kotlin.test.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class GitClientTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `logComponentSubjects returns merge and direct mainline commits only`() {
        // Arrange
        val repo = temporaryFolder.newFolder("git-client-test")
        repo.initGit()

        val componentDir = repo.resolve("components/bom")
        componentDir.mkdirs()
        val componentFile = componentDir.resolve("file.txt")
        componentFile.writeText("initial")
        repo.runGit("add", ".")
        repo.runGit("commit", "-m", "chore: initial")

        repo.runGit("checkout", "-b", "feature/build-plugin")
        componentFile.appendText("\nchangelog")
        repo.runGit("commit", "-am", "feat: add changelog plugin")
        componentFile.appendText("\nversioning")
        repo.runGit("commit", "-am", "feat: add versioning plugin")

        repo.runGit("checkout", "-")
        repo.runGit("merge", "--no-ff", "feature/build-plugin", "-m", "chore(build): update build-plugin (#4)")

        componentFile.appendText("\ndirect")
        repo.runGit("commit", "-am", "fix: direct mainline change")

        // Act
        val subjects = GitClient().logComponentSubjects(repo, "components/bom", startRef = null)

        // Assert
        assertThat(subjects).isEqualTo(
            listOf(
                "fix: direct mainline change",
                "chore(build): update build-plugin (#4)",
            ),
        )
    }

    @Test
    fun `logComponentSubjects reads mainline ref instead of current feature branch`() {
        // Arrange
        val repo = temporaryFolder.newFolder("git-client-test")
        repo.initGit()

        val componentDir = repo.resolve("components/bom")
        componentDir.mkdirs()
        val componentFile = componentDir.resolve("file.txt")
        componentFile.writeText("initial")
        repo.runGit("add", ".")
        repo.runGit("commit", "-m", "chore: initial")

        repo.runGit("checkout", "-b", "feature/build-plugin")
        componentFile.appendText("\nchangelog")
        repo.runGit("commit", "-am", "chore(build): add changelog plugin")

        // Act
        val subjects = GitClient().logComponentSubjects(repo, "components/bom", startRef = null)

        // Assert
        assertThat(subjects).isEqualTo(emptyList())
    }

    @Test
    fun `logComponentSubjects uses start ref as exact exclusive lower bound`() {
        // Arrange
        val repo = temporaryFolder.newFolder("git-client-test")
        repo.initGit()

        val componentDir = repo.resolve("components/bom")
        componentDir.mkdirs()
        val componentFile = componentDir.resolve("file.txt")
        componentFile.writeText("initial")
        repo.runGit("add", ".")
        repo.runGit("commit", "-m", "chore: initial")

        componentFile.appendText("\nold")
        repo.runGit("commit", "-am", "fix: old released change")
        repo.runGit("tag", "v1.0.0")

        componentFile.appendText("\nnew")
        repo.runGit("commit", "-am", "fix: new unreleased change")

        // Act
        val subjects = GitClient().logComponentSubjects(repo, "components/bom", startRef = "v1.0.0")

        // Assert
        assertThat(subjects).isEqualTo(listOf("fix: new unreleased change"))
    }

    private fun File.initGit() {
        runGit("init")
        runGit("branch", "-M", "main")
        runGit("config", "user.email", "test@example.com")
        runGit("config", "user.name", "Test User")
        runGit("config", "commit.gpgsign", "false")
    }

    private fun File.runGit(vararg args: String): List<String> {
        val command = listOf("git", "-C", absolutePath) + args
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readLines()
        val exitCode = process.waitFor()
        check(exitCode == 0) {
            "Command failed ($exitCode): ${command.joinToString(" ")}\n${output.joinToString("\n")}"
        }
        return output
    }
}
