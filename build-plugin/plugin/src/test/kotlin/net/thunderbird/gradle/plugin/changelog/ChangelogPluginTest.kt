package net.thunderbird.gradle.plugin.changelog

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import java.io.File
import kotlin.test.Test
import net.thunderbird.gradle.plugin.ProjectConfig
import net.thunderbird.gradle.plugin.changelog.internal.fs.FileHelper
import net.thunderbird.gradle.plugin.versioning.VersioningPlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class ChangelogPluginTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `apply registers changelog tasks next to nearest version properties`() {
        // Arrange
        val fixture = createNestedComponentProject()

        // Act
        fixture.project.plugins.apply(ChangelogPlugin::class.java)

        // Assert
        val task = fixture.project.tasks.named(UpdateChangelogTask.TASK_NAME).get() as UpdateChangelogTask
        assertThat(task.versionFile.get().asFile.canonicalFile).isEqualTo(fixture.versionFile.canonicalFile)
        assertThat(task.changelogFile.get().asFile.canonicalFile).isEqualTo(fixture.changelogFile.canonicalFile)
        assertThat(task.repoRootDir.get().asFile.canonicalFile).isEqualTo(fixture.rootDir.canonicalFile)
        assertThat(task.repoUrl.get()).isEqualTo(ProjectConfig.Publishing.url)

        val finalizeTask = fixture.project.tasks.named(FinalizeChangelogTask.TASK_NAME).get() as FinalizeChangelogTask
        assertThat(finalizeTask.changelogFile.get().asFile.canonicalFile)
            .isEqualTo(fixture.changelogFile.canonicalFile)
    }

    @Test
    fun `apply is compatible with versioning plugin version properties`() {
        // Arrange
        val fixture = createNestedComponentProject("changelog-versioning-plugin-test")

        // Act
        fixture.project.plugins.apply(ChangelogPlugin::class.java)
        fixture.project.plugins.apply(VersioningPlugin::class.java)

        // Assert
        assertThat(fixture.project.version.toString()).isEqualTo("1.2.3-SNAPSHOT")
        assertThat(fixture.project.tasks.findByName(UpdateChangelogTask.TASK_NAME)).isNotNull()
        assertThat(fixture.project.tasks.findByName("versionBumpPatch")).isNotNull()
    }

    @Test
    fun `apply does not register changelog tasks when version properties is absent`() {
        // Arrange
        val rootDir = temporaryFolder.newFolder("changelog-plugin-test")
        val projectDir = rootDir.resolve("components/bom")
        projectDir.mkdirs()
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .build()

        // Act
        project.plugins.apply(ChangelogPlugin::class.java)

        // Assert
        assertThat(project.tasks.findByName(UpdateChangelogTask.TASK_NAME)).isNull()
        assertThat(project.tasks.findByName(FinalizeChangelogTask.TASK_NAME)).isNull()
    }

    private fun createNestedComponentProject(
        rootFolderName: String = "changelog-plugin-test",
    ): ComponentProject {
        val rootDir = temporaryFolder.newFolder(rootFolderName)
        val componentDir = rootDir.resolve("components/bom")
        val projectDir = componentDir.resolve("nested/module")
        projectDir.mkdirs()
        componentDir.resolve(FileHelper.VERSION_FILE).writeText(versionProperties())

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
            componentDir = componentDir,
            project = project,
        )
    }

    private data class ComponentProject(
        val rootDir: File,
        val componentDir: File,
        val project: Project,
    ) {
        val versionFile: File = componentDir.resolve(FileHelper.VERSION_FILE)
        val changelogFile: File = componentDir.resolve(FileHelper.CHANGELOG_FILE)
    }

    private companion object {
        private fun versionProperties(): String = """
            MAJOR=1
            MINOR=2
            PATCH=3
        """.trimIndent()
    }
}
