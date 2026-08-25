package net.thunderbird.gradle.plugin.versioning

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class PrintReleaseTagTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `print writes release tag to standard output`() {
        // Arrange
        val rootDir = temporaryFolder.newFolder("print-release-tag-task-test")
        val componentDir = rootDir.resolve("components/bom")
        componentDir.mkdirs()
        componentDir.resolve("version.properties").writeText(VERSION_PROPERTIES)
        val project = ProjectBuilder.builder()
            .withProjectDir(componentDir)
            .build()
        val task = project.tasks.create(PrintReleaseTagTask.TASK_NAME, PrintReleaseTagTask::class.java).apply {
            startDir.set(project.layout.projectDirectory)
            repoRootDir.set(rootDir)
        }

        // Act
        val output = captureStandardOut { task.print() }

        // Assert
        assertThat(output.trim()).isEqualTo("bom-1.2.3")
    }

    @Test
    fun `print uses nearest version properties for nested module`() {
        // Arrange
        val rootDir = temporaryFolder.newFolder("print-release-tag-task-test")
        rootDir.resolve("version.properties").writeText(ROOT_VERSION_PROPERTIES)
        val componentDir = rootDir.resolve("components/feature/sync")
        val moduleDir = componentDir.resolve("impl")
        moduleDir.mkdirs()
        componentDir.resolve("version.properties").writeText(VERSION_PROPERTIES)
        val project = ProjectBuilder.builder()
            .withProjectDir(moduleDir)
            .build()
        val task = project.tasks.create(PrintReleaseTagTask.TASK_NAME, PrintReleaseTagTask::class.java).apply {
            startDir.set(project.layout.projectDirectory)
            repoRootDir.set(rootDir)
        }

        // Act
        val output = captureStandardOut { task.print() }

        // Assert
        assertThat(output.trim()).isEqualTo("sync-1.2.3")
    }

    private fun captureStandardOut(block: () -> Unit): String {
        val originalOut = System.out
        val buffer = ByteArrayOutputStream()
        System.setOut(PrintStream(buffer))
        try {
            block()
        } finally {
            System.setOut(originalOut)
        }
        return buffer.toString()
    }

    private companion object {
        private val VERSION_PROPERTIES = """
            MAJOR=1
            MINOR=2
            PATCH=3
        """.trimIndent()

        private val ROOT_VERSION_PROPERTIES = """
            MAJOR=9
            MINOR=8
            PATCH=7
        """.trimIndent()
    }
}
