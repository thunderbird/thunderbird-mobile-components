package net.thunderbird.gradle.plugin.versioning

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import java.io.File
import java.util.Properties
import kotlin.test.Test
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class VersionBumpTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `bump increments major and resets minor and patch`() {
        // Arrange
        val fixture = createComponentProject()
        val task = createTask(fixture, part = "major")

        // Act
        task.bump()

        // Assert
        assertThat(fixture.versionProperties.readProperties()).isEqualTo(
            mapOf(
                "MAJOR" to "2",
                "MINOR" to "0",
                "PATCH" to "0",
            ),
        )
    }

    @Test
    fun `bump increments minor and resets patch`() {
        // Arrange
        val fixture = createComponentProject()
        val task = createTask(fixture, part = "minor")

        // Act
        task.bump()

        // Assert
        assertThat(fixture.versionProperties.readProperties()).isEqualTo(
            mapOf(
                "MAJOR" to "1",
                "MINOR" to "3",
                "PATCH" to "0",
            ),
        )
    }

    @Test
    fun `bump increments patch`() {
        // Arrange
        val fixture = createComponentProject()
        val task = createTask(fixture, part = "patch")

        // Act
        task.bump()

        // Assert
        assertThat(fixture.versionProperties.readProperties()).isEqualTo(
            mapOf(
                "MAJOR" to "1",
                "MINOR" to "2",
                "PATCH" to "4",
            ),
        )
    }

    @Test
    fun `bump fails for invalid part`() {
        // Arrange
        val fixture = createComponentProject()
        val task = createTask(fixture, part = "build")

        // Act
        val failure = assertFailure { task.bump() }

        // Assert
        failure.isInstanceOf<IllegalArgumentException>()
        failure.messageContains("Invalid part to bump: build")
    }

    private fun createComponentProject(): ComponentProject {
        val rootDir = temporaryFolder.newFolder("version-bump-task-test")
        val componentDir = rootDir.resolve("components/bom")
        componentDir.mkdirs()
        val versionProperties = componentDir.resolve("version.properties")
        versionProperties.writeText(VERSION_PROPERTIES)
        val project = ProjectBuilder.builder()
            .withProjectDir(componentDir)
            .build()

        return ComponentProject(
            rootDir = rootDir,
            componentDir = componentDir,
            versionProperties = versionProperties,
            project = project,
        )
    }

    private fun createTask(
        fixture: ComponentProject,
        part: String,
    ): VersionBumpTask {
        return fixture.project.tasks.create(
            "versionBump${part.replaceFirstChar {
                it.uppercase()
            }}",
            VersionBumpTask::class.java,
        )
            .apply {
                startDir.set(fixture.project.layout.projectDirectory)
                repoRootDir.set(fixture.rootDir)
                this.part.set(part)
            }
    }

    private fun File.readProperties(): Map<String, String> {
        val properties = Properties()
        inputStream().use(properties::load)
        return properties.entries.associate { (key, value) -> key.toString() to value.toString() }
    }

    private data class ComponentProject(
        val rootDir: File,
        val componentDir: File,
        val versionProperties: File,
        val project: org.gradle.api.Project,
    )

    private companion object {
        private val VERSION_PROPERTIES = """
            MAJOR=1
            MINOR=2
            PATCH=3
        """.trimIndent()
    }
}
