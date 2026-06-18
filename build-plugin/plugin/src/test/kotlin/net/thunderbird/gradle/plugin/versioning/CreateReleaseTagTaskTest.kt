package net.thunderbird.gradle.plugin.versioning

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import assertk.assertions.messageContains
import java.io.File
import kotlin.test.Test
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class CreateReleaseTagTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `versioning plugin registers createReleaseTag task`() {
        // Arrange
        val repo = createRepository(VERSION_PROPERTIES)
        val project = ProjectBuilder.builder()
            .withProjectDir(repo.resolve("components/bom"))
            .build()

        // Act
        project.plugins.apply(VersioningPlugin::class.java)

        // Assert
        assertThat(project.tasks.findByName(CreateReleaseTagTask.TASK_NAME)).isNotNull()
    }

    @Test
    fun `createReleaseTag creates component tag from version properties`() {
        // Arrange
        val repo = createRepository(VERSION_PROPERTIES)
        val task = createTask(repo)

        // Act
        task.createReleaseTag()

        // Assert
        assertThat(repo.runGit("tag", "--list")).contains("bom-1.2.3")
    }

    @Test
    fun `createReleaseTag fails when tag already exists`() {
        // Arrange
        val repo = createRepository(VERSION_PROPERTIES)
        repo.runGit("tag", "bom-1.2.3")
        val task = createTask(repo)

        // Act
        val failure = assertFailure { task.createReleaseTag() }

        // Assert
        failure.messageContains("Release tag 'bom-1.2.3' already exists")
    }

    private fun createRepository(versionProperties: String): File {
        val repo = temporaryFolder.newFolder("create-release-tag-test")
        repo.runGit("init")
        repo.runGit("config", "user.email", "test@example.com")
        repo.runGit("config", "user.name", "Test User")
        repo.runGit("config", "commit.gpgsign", "false")

        val componentDir = repo.resolve("components/bom")
        componentDir.mkdirs()
        componentDir.resolve("version.properties").writeText(versionProperties)
        componentDir.resolve("file.txt").writeText("initial")
        repo.runGit("add", ".")
        repo.runGit("commit", "-m", "chore: initial")

        return repo
    }

    private fun createTask(repo: File): CreateReleaseTagTask {
        val componentDir = repo.resolve("components/bom")
        val project = ProjectBuilder.builder()
            .withProjectDir(componentDir)
            .build()

        return project.tasks.create(CreateReleaseTagTask.TASK_NAME, CreateReleaseTagTask::class.java).apply {
            startDir.set(project.layout.projectDirectory)
            repoRootDir.set(repo)
        }
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

    private companion object {
        private val VERSION_PROPERTIES = """
            MAJOR=1
            MINOR=2
            PATCH=3
        """.trimIndent()
    }
}
