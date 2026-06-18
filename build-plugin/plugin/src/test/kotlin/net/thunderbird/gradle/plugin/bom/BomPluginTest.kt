package net.thunderbird.gradle.plugin.bom

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlin.test.Test
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlatformPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class BomPluginTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `apply configures bom project`() {
        // Arrange
        val project = createBomProject(parentName = "components")

        // Act
        project.plugins.apply(BomPlugin::class.java)

        // Assert
        assertThat(project.group.toString()).isEqualTo("net.thunderbird.components")
        assertThat(project.plugins.findPlugin(JavaPlatformPlugin::class.java)).isNotNull()
        assertThat(project.tasks.findByName("updateChangelog")).isNotNull()
        assertThat(project.tasks.findByName("printVersion")).isNotNull()
        assertThat(project.tasks.findByName("validateSnapshotVersionForPublishing")).isNotNull()
        assertThat(project.tasks.findByName("publishToMavenLocal")).isNotNull()
    }

    @Test
    fun `apply derives group from bom parent path`() {
        // Arrange
        val project = createBomProject(parentName = "platform")

        // Act
        project.plugins.apply(BomPlugin::class.java)

        // Assert
        assertThat(project.group.toString()).isEqualTo("net.thunderbird.platform")
    }

    private fun createBomProject(parentName: String) = createProject(parentName = parentName, projectName = "bom")

    private fun createProject(
        parentName: String,
        projectName: String,
    ): Project {
        val rootDir = temporaryFolder.newFolder("$parentName-$projectName-plugin-test")
        val projectDir = rootDir.resolve("$parentName/$projectName")
        projectDir.mkdirs()
        projectDir.resolve("version.properties").writeText(VERSION_PROPERTIES)
        val rootProject = ProjectBuilder.builder()
            .withProjectDir(rootDir)
            .withName("root")
            .build()
        val parentProject = ProjectBuilder.builder()
            .withName(parentName)
            .withParent(rootProject)
            .build()
        return ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .withName(projectName)
            .withParent(parentProject)
            .build()
    }

    private companion object {
        private val VERSION_PROPERTIES = """
            MAJOR=1
            MINOR=2
            PATCH=3
        """.trimIndent()
    }
}
