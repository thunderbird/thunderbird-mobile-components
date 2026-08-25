package net.thunderbird.gradle.plugin.versioning

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class PrintVersionTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `print writes resolved version to standard output`() {
        // Arrange
        val rootDir = temporaryFolder.newFolder("print-version-task-test")
        val componentDir = rootDir.resolve("components/bom")
        componentDir.mkdirs()
        componentDir.resolve("version.properties").writeText(VERSION_PROPERTIES)
        val project = ProjectBuilder.builder()
            .withProjectDir(componentDir)
            .build()
        val task = project.tasks.create(PrintVersionTask.TASK_NAME, PrintVersionTask::class.java).apply {
            startDir.set(project.layout.projectDirectory)
            repoRootDir.set(rootDir)
        }

        // Act
        val output = captureStandardOut { task.print() }

        // Assert
        assertThat(output.trim()).isEqualTo("1.2.3-SNAPSHOT")
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
    }
}
