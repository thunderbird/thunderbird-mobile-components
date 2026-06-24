package net.thunderbird.gradle.plugin.versioning

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import java.io.File
import kotlin.test.Test
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class VersioningPluginTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `apply uses snapshot version when head does not have release tag`() {
        // Arrange
        val fixture = createNestedComponentProject(VERSION_PROPERTIES)

        // Act
        fixture.project.plugins.apply(VersioningPlugin::class.java)

        // Assert
        assertThat(fixture.project.version.toString()).isEqualTo("1.2.3-SNAPSHOT")
    }

    @Test
    fun `apply uses release version when head has component release tag`() {
        // Arrange
        val fixture = createNestedComponentProject(VERSION_PROPERTIES)
        fixture.rootDir.runGit("init")
        fixture.rootDir.runGit("config", "user.email", "test@example.com")
        fixture.rootDir.runGit("config", "user.name", "Test User")
        fixture.rootDir.runGit("config", "commit.gpgsign", "false")
        fixture.rootDir.runGit("add", ".")
        fixture.rootDir.runGit("commit", "-m", "chore: release")
        fixture.rootDir.runGit("tag", "bom-1.2.3")

        // Act
        fixture.project.plugins.apply(VersioningPlugin::class.java)

        // Assert
        assertThat(fixture.project.version.toString()).isEqualTo("1.2.3")
    }

    @Test
    fun `apply registers versioning tasks`() {
        // Arrange
        val fixture = createNestedComponentProject(VERSION_PROPERTIES)

        // Act
        fixture.project.plugins.apply(VersioningPlugin::class.java)

        // Assert
        assertThat(fixture.project.tasks.findByName("versionBumpMajor")).isNotNull()
        assertThat(fixture.project.tasks.findByName("versionBumpMinor")).isNotNull()
        assertThat(fixture.project.tasks.findByName("versionBumpPatch")).isNotNull()
        assertThat(fixture.project.tasks.findByName(PrintVersionTask.TASK_NAME)).isNotNull()
        assertThat(fixture.project.tasks.findByName(CreateReleaseTagTask.TASK_NAME)).isNotNull()
    }

    @Test
    fun `apply fails when version properties is missing`() {
        // Arrange
        val rootDir = temporaryFolder.newFolder("versioning-plugin-test")
        val projectDir = rootDir.resolve("components/bom")
        projectDir.mkdirs()
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()

        // Act
        val failure = runCatching {
            project.plugins.apply(VersioningPlugin::class.java)
        }.exceptionOrNull()

        // Assert
        assertThat(failure).isNotNull().isInstanceOf<GradleException>()
        assertThat(failure?.cause).isNotNull().isInstanceOf<IllegalStateException>()
        assertThat(failure?.cause?.message).isNotNull().contains("No version.properties found")
    }

    @Test
    fun `apply fails when version properties is invalid`() {
        // Arrange
        val fixture = createNestedComponentProject(INVALID_VERSION_PROPERTIES)

        // Act
        val failure = runCatching {
            fixture.project.plugins.apply(VersioningPlugin::class.java)
        }.exceptionOrNull()

        // Assert
        assertThat(failure).isNotNull().isInstanceOf<GradleException>()
        assertThat(failure?.cause).isNotNull().isInstanceOf<IllegalStateException>()
        assertThat(failure?.cause?.message).isNotNull().contains("Invalid version.properties")
    }

    private fun createNestedComponentProject(versionProperties: String): ComponentProject {
        val rootDir = temporaryFolder.newFolder("versioning-plugin-test")
        val componentDir = rootDir.resolve("components/bom")
        val projectDir = componentDir.resolve("nested/module")
        projectDir.mkdirs()

        val versionPropertiesFile = componentDir.resolve("version.properties")
        versionPropertiesFile.writeText(versionProperties)

        val rootProject = ProjectBuilder.builder()
            .withProjectDir(rootDir)
            .withName("root")
            .build()
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .withName("module")
            .withParent(rootProject)
            .build()

        return ComponentProject(
            rootDir = rootDir,
            project = project,
        )
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

    private data class ComponentProject(
        val rootDir: File,
        val project: Project,
    )

    private companion object {
        private val VERSION_PROPERTIES = """
            MAJOR=1
            MINOR=2
            PATCH=3
        """.trimIndent()

        private val INVALID_VERSION_PROPERTIES = """
            MAJOR=1
            PATCH=3
        """.trimIndent()
    }
}
