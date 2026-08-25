package net.thunderbird.gradle.plugin.publishing

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.messageContains
import java.io.File
import kotlin.test.Test
import net.thunderbird.gradle.plugin.versioning.VersioningPlugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class PublishingPluginTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `apply derives project group from parent path`() {
        // Arrange
        val rootProject = ProjectBuilder.builder()
            .withProjectDir(temporaryFolder.newFolder("publishing-group-test"))
            .withName("root")
            .build()
        val componentProject = ProjectBuilder.builder()
            .withName("components")
            .withParent(rootProject)
            .build()
        val project = ProjectBuilder.builder()
            .withName("example")
            .withParent(componentProject)
            .build()

        // Act
        project.plugins.apply(PublishingPlugin::class.java)

        // Assert
        assertThat(project.group.toString()).isEqualTo("net.thunderbird.components")
    }

    @Test
    fun `apply keeps explicitly configured project group`() {
        // Arrange
        val rootProject = ProjectBuilder.builder()
            .withProjectDir(temporaryFolder.newFolder("explicit-publishing-group-test"))
            .withName("root")
            .build()
        val componentProject = ProjectBuilder.builder()
            .withName("components")
            .withParent(rootProject)
            .build()
        val project = ProjectBuilder.builder()
            .withName("example")
            .withParent(componentProject)
            .build()
        project.group = "custom.group"

        // Act
        project.plugins.apply(PublishingPlugin::class.java)

        // Assert
        assertThat(project.group.toString()).isEqualTo("custom.group")
    }

    @Test
    fun `apply registers validation tasks and keeps vanniktech publishing tasks`() {
        // Arrange
        val project = ProjectBuilder.builder()
            .withProjectDir(temporaryFolder.newFolder("publishing-task-test"))
            .build()

        // Act
        project.plugins.apply(PublishingPlugin::class.java)

        // Assert
        assertThat(project.tasks.findByName("validateStableVersionForPublishing")).isNotNull()
        assertThat(project.tasks.findByName("validateSnapshotVersionForPublishing")).isNotNull()
        assertThat(project.tasks.findByName("publishToMavenLocal")).isNotNull()
        assertThat(project.tasks.findByName("publishToMavenCentral")).isNotNull()
        assertThat(project.tasks.findByName("publishAndReleaseToMavenCentral")).isNotNull()
    }

    @Test
    fun `validateSnapshotVersionForPublishing accepts snapshot version`() {
        // Arrange
        val project = createVersionedPublishingProject()

        // Act
        project.executeTask("validateSnapshotVersionForPublishing")

        // Assert
        assertThat(project.version.toString()).isEqualTo("1.2.3-SNAPSHOT")
    }

    @Test
    fun `validateStableVersionForPublishing rejects snapshot version`() {
        // Arrange
        val project = createVersionedPublishingProject()

        // Act
        val failure = assertFailure {
            project.executeTask("validateStableVersionForPublishing")
        }

        // Assert
        failure.messageContains("Stable releases require a non-SNAPSHOT version")
    }

    @Test
    fun `validateStableVersionForPublishing accepts stable version`() {
        // Arrange
        val fixture = createVersionedPublishingProjectFixture(releaseTagged = true)

        // Act
        fixture.project.executeTask("validateStableVersionForPublishing")

        // Assert
        assertThat(fixture.project.version.toString()).isEqualTo("1.2.3")
    }

    @Test
    fun `validateSnapshotVersionForPublishing rejects stable version`() {
        // Arrange
        val fixture = createVersionedPublishingProjectFixture(releaseTagged = true)

        // Act
        val failure = assertFailure {
            fixture.project.executeTask("validateSnapshotVersionForPublishing")
        }

        // Assert
        failure.messageContains("Daily snapshots require a SNAPSHOT version")
    }

    private fun createVersionedPublishingProject(): Project {
        return createVersionedPublishingProjectFixture().project
    }

    private fun createVersionedPublishingProjectFixture(releaseTagged: Boolean = false): ProjectFixture {
        val rootDir = temporaryFolder.newFolder("publishing-version-test")
        rootDir.runGit("init")
        rootDir.runGit("config", "user.email", "test@example.com")
        rootDir.runGit("config", "user.name", "Test User")
        rootDir.runGit("config", "commit.gpgsign", "false")

        val componentDir = rootDir.resolve("components/bom")
        componentDir.mkdirs()
        componentDir.resolve("version.properties").writeText(VERSION_PROPERTIES)
        componentDir.resolve("file.txt").writeText("initial")
        rootDir.runGit("add", ".")
        rootDir.runGit("commit", "-m", "chore: initial")
        if (releaseTagged) {
            rootDir.runGit("tag", "bom-1.2.3")
        }

        val rootProject = ProjectBuilder.builder()
            .withProjectDir(rootDir)
            .withName("root")
            .build()
        val project = ProjectBuilder.builder()
            .withProjectDir(componentDir)
            .withName("bom")
            .withParent(rootProject)
            .build()
        project.plugins.apply(VersioningPlugin::class.java)
        project.plugins.apply(PublishingPlugin::class.java)

        return ProjectFixture(rootDir, project)
    }

    private fun Project.executeTask(name: String) {
        val task = tasks.getByName(name)
        task.actions.forEach { action -> action.execute(task) }
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

    private data class ProjectFixture(
        val rootDir: File,
        val project: Project,
    )

    private companion object {
        private val VERSION_PROPERTIES = """
            MAJOR=1
            MINOR=2
            PATCH=3
        """.trimIndent()
    }
}
