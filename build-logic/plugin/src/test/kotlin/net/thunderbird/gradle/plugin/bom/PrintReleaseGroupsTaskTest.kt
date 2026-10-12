/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.gradle.plugin.bom

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.messageContains
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class PrintReleaseGroupsTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `print groups projects by nearest version properties in declaration order`() {
        // Arrange
        val rootDir = temporaryFolder.newFolder("print-release-groups-task-test")
        rootDir.writeVersion("components/core", "1.0.0")
        rootDir.writeVersion("components/core/logging", "2.1.0")
        rootDir.writeVersion("components/bom", "3.0.1")
        rootDir.createProject("components/core/logging/core")
        rootDir.createProject("components/core/outcome")
        rootDir.createProject("components/core/logging/file")
        rootDir.createProject("components/bom")
        val task = createTask(
            rootDir,
            ":components:core:logging:core",
            ":components:core:outcome",
            ":components:core:logging:file",
            ":components:bom",
        )

        // Act
        val output = captureStandardOut { task.print() }

        // Assert
        assertThat(output.trim().lines()).isEqualTo(
            listOf(
                "logging-2.1.0 components/core/logging :components:core:logging:core :components:core:logging:file",
                "core-1.0.0 components/core :components:core:outcome",
                "bom-3.0.1 components/bom :components:bom",
            ),
        )
    }

    @Test
    fun `print fails for project without build file`() {
        // Arrange
        val rootDir = temporaryFolder.newFolder("print-release-groups-task-test")
        rootDir.writeVersion("components/core", "1.0.0")
        val task = createTask(rootDir, ":components:core:missing")

        // Act & Assert
        assertFailure { task.print() }.messageContains("Project :components:core:missing has no build.gradle.kts")
    }

    @Test
    fun `print fails for component missing from bom`() {
        // Arrange
        val rootDir = temporaryFolder.newFolder("print-release-groups-task-test")
        rootDir.writeVersion("components/core", "1.0.0")
        rootDir.createProject("components/core/outcome")
        rootDir.createProject("components/core/forgotten")
        rootDir.createProject("components/core/outcome/build/generated")
        val task = createTask(rootDir, ":components:core:outcome")

        // Act & Assert
        assertFailure { task.print() }.messageContains("Projects missing from the BOM: :components:core:forgotten.")
    }

    private fun createTask(rootDir: File, vararg paths: String): PrintReleaseGroupsTask {
        val project = ProjectBuilder.builder()
            .withProjectDir(rootDir)
            .build()
        return project.tasks.create(PrintReleaseGroupsTask.TASK_NAME, PrintReleaseGroupsTask::class.java).apply {
            projectPaths.set(paths.toList())
            repoRootDir.set(rootDir)
            componentsDir.set(rootDir.resolve("components"))
        }
    }

    private fun File.writeVersion(path: String, version: String) {
        val (major, minor, patch) = version.split(".")
        resolve(path).apply { mkdirs() }
            .resolve("version.properties")
            .writeText("MAJOR=$major\nMINOR=$minor\nPATCH=$patch\n")
    }

    private fun File.createProject(path: String) {
        resolve(path).apply { mkdirs() }.resolve("build.gradle.kts").writeText("")
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
}
