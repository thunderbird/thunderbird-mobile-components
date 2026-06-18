package net.thunderbird.gradle.plugin.versioning.internal

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.File
import kotlin.test.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class GitVersionReaderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `read returns release version when head has matching component tag`() {
        // Arrange
        val repo = createRepository()
        repo.runGit("tag", "bom-1.2.3")
        val versionFile = repo.resolve("components/bom/version.properties")

        // Act
        val version = GitVersionReader().read(
            repoRoot = repo,
            versionFile = versionFile,
            version = Version(major = 1, minor = 2, patch = 3),
        )

        // Assert
        assertThat(version).isEqualTo("1.2.3")
    }

    @Test
    fun `read returns snapshot version when head has no matching component tag`() {
        // Arrange
        val repo = createRepository()
        val versionFile = repo.resolve("components/bom/version.properties")

        // Act
        val version = GitVersionReader().read(
            repoRoot = repo,
            versionFile = versionFile,
            version = Version(major = 1, minor = 2, patch = 3),
        )

        // Assert
        assertThat(version).isEqualTo("1.2.3-SNAPSHOT")
    }

    @Test
    fun `read ignores tags for other components`() {
        // Arrange
        val repo = createRepository()
        repo.runGit("tag", "other-1.2.3")
        val versionFile = repo.resolve("components/bom/version.properties")

        // Act
        val version = GitVersionReader().read(
            repoRoot = repo,
            versionFile = versionFile,
            version = Version(major = 1, minor = 2, patch = 3),
        )

        // Assert
        assertThat(version).isEqualTo("1.2.3-SNAPSHOT")
    }

    @Test
    fun `read returns snapshot version outside a git repository`() {
        // Arrange
        val repo = temporaryFolder.newFolder("not-a-git-repo")
        val componentDir = repo.resolve("components/bom")
        componentDir.mkdirs()
        val versionFile = componentDir.resolve("version.properties")
        versionFile.writeText("")

        // Act
        val version = GitVersionReader().read(
            repoRoot = repo,
            versionFile = versionFile,
            version = Version(major = 1, minor = 2, patch = 3),
        )

        // Assert
        assertThat(version).isEqualTo("1.2.3-SNAPSHOT")
    }

    private fun createRepository(): File {
        val repo = temporaryFolder.newFolder("git-version-reader-test")
        repo.runGit("init")
        repo.runGit("config", "user.email", "test@example.com")
        repo.runGit("config", "user.name", "Test User")
        repo.runGit("config", "commit.gpgsign", "false")

        val componentDir = repo.resolve("components/bom")
        componentDir.mkdirs()
        componentDir.resolve("version.properties").writeText(
            """
            MAJOR=1
            MINOR=2
            PATCH=3
            """.trimIndent(),
        )
        repo.runGit("add", ".")
        repo.runGit("commit", "-m", "chore: initial")
        return repo
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
