package net.thunderbird.gradle.plugin.versioning.internal

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.messageContains
import java.io.File
import java.util.Properties
import kotlin.test.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class VersionManagerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `get reads nearest version properties`() {
        // Arrange
        val root = createRoot()
        createVersionFile(root, Version(major = 9, minor = 9, patch = 9))
        val component = root.resolve("components/bom")
        val nested = component.resolve("nested/module")
        nested.mkdirs()
        val componentVersionFile = createVersionFile(
            component,
            Version(major = 1, minor = 2, patch = 3),
        )

        // Act
        val manager = VersionManager(base = nested, root = root)

        // Assert
        assertThat(manager.get()).isEqualTo(Version(major = 1, minor = 2, patch = 3))
        assertThat(manager.sourceFile()?.canonicalFile).isEqualTo(componentVersionFile.canonicalFile)
    }

    @Test
    fun `get can read root version properties`() {
        // Arrange
        val root = createRoot()
        val rootVersionFile = createVersionFile(root, Version(major = 1, minor = 0, patch = 0))
        val nested = root.resolve("components/bom")
        nested.mkdirs()

        // Act
        val manager = VersionManager(base = nested, root = root)

        // Assert
        assertThat(manager.get()).isEqualTo(Version(major = 1, minor = 0, patch = 0))
        assertThat(manager.sourceFile()?.canonicalFile).isEqualTo(rootVersionFile.canonicalFile)
    }

    @Test
    fun `get stops searching at repo root`() {
        // Arrange
        val workspace = temporaryFolder.newFolder("workspace")
        createVersionFile(workspace, Version(major = 9, minor = 9, patch = 9))
        val repoRoot = workspace.resolve("repo")
        val nested = repoRoot.resolve("components/bom")
        nested.mkdirs()
        val manager = VersionManager(base = nested, root = repoRoot)

        // Act
        val failure = assertFailure { manager.get() }

        // Assert
        failure.isInstanceOf<IllegalStateException>()
        failure.messageContains("No version.properties found")
        assertThat(manager.sourceFile()).isNull()
    }

    @Test
    fun `get fails when version properties is invalid`() {
        // Arrange
        val root = createRoot()
        root.resolve("version.properties").writeText(
            """
            MAJOR=1
            PATCH=3
            """.trimIndent(),
        )
        val manager = VersionManager(base = root, root = root)

        // Act
        val failure = assertFailure { manager.get() }

        // Assert
        failure.isInstanceOf<IllegalStateException>()
        failure.messageContains("Invalid version.properties")
    }

    @Test
    fun `update writes version to source file`() {
        // Arrange
        val root = createRoot()
        val versionFile = createVersionFile(root, Version(major = 1, minor = 2, patch = 3))
        val manager = VersionManager(base = root, root = root)

        // Act
        manager.update(Version(major = 2, minor = 0, patch = 0))

        // Assert
        assertThat(versionFile.readProperties()).isEqualTo(
            mapOf(
                "MAJOR" to "2",
                "MINOR" to "0",
                "PATCH" to "0",
            ),
        )
    }

    @Test
    fun `update fails when source file is missing`() {
        // Arrange
        val root = createRoot()
        val manager = VersionManager(base = root, root = root)

        // Act
        val failure = assertFailure {
            manager.update(Version(major = 1, minor = 0, patch = 0))
        }

        // Assert
        failure.isInstanceOf<IllegalStateException>()
        failure.messageContains("No version.properties file found to update")
    }

    private fun createRoot(): File = temporaryFolder.newFolder("version-manager-test")

    private fun createVersionFile(dir: File, version: Version): File {
        dir.mkdirs()
        val file = dir.resolve("version.properties")
        file.writeText(
            """
            MAJOR=${version.major}
            MINOR=${version.minor}
            PATCH=${version.patch}
            """.trimIndent(),
        )
        return file
    }

    private fun File.readProperties(): Map<String, String> {
        val properties = Properties()
        inputStream().use(properties::load)
        return properties.entries.associate { (key, value) -> key.toString() to value.toString() }
    }
}
