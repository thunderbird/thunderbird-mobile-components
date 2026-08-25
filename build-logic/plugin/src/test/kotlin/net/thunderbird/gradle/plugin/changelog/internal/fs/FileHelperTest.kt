package net.thunderbird.gradle.plugin.changelog.internal.fs

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.io.File
import kotlin.test.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class FileHelperTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `locateNearestVersionDir walks up to nearest version properties`() {
        // Arrange
        val root = createRoot()
        val component = createVersionedDir(root.resolve("components/bom"))
        val nested = component.resolve("src/main")
        nested.mkdirs()

        // Act
        val versionDir = FileHelper.locateNearestVersionDir(nested, root)

        // Assert
        assertThat(versionDir).isEqualTo(component)
    }

    @Test
    fun `locateNearestVersionDir returns start directory when it contains version properties`() {
        // Arrange
        val root = createRoot()
        val component = createVersionedDir(root.resolve("components/bom"))

        // Act
        val versionDir = FileHelper.locateNearestVersionDir(component, root)

        // Assert
        assertThat(versionDir).isEqualTo(component)
    }

    @Test
    fun `locateNearestVersionDir returns nearest version properties when multiple parents have one`() {
        // Arrange
        val root = createRoot()
        createVersionedDir(root)
        val component = createVersionedDir(root.resolve("components/bom"))
        val nested = component.resolve("nested/module")
        nested.mkdirs()

        // Act
        val versionDir = FileHelper.locateNearestVersionDir(nested, root)

        // Assert
        assertThat(versionDir).isEqualTo(component)
    }

    @Test
    fun `locateNearestVersionDir can return repo root when root contains version properties`() {
        // Arrange
        val root = createVersionedDir(createRoot())
        val nested = root.resolve("components/bom/src/main")
        nested.mkdirs()

        // Act
        val versionDir = FileHelper.locateNearestVersionDir(nested, root)

        // Assert
        assertThat(versionDir).isEqualTo(root)
    }

    @Test
    fun `locateNearestVersionDir stops at repo root`() {
        // Arrange
        val workspace = temporaryFolder.newFolder("workspace")
        createVersionedDir(workspace)
        val repoRoot = workspace.resolve("repo")
        val nested = repoRoot.resolve("components/bom/src/main")
        nested.mkdirs()

        // Act
        val versionDir = FileHelper.locateNearestVersionDir(nested, repoRoot)

        // Assert
        assertThat(versionDir).isNull()
    }

    @Test
    fun `locateNearestVersionDir returns null when version properties is absent`() {
        // Arrange
        val root = createRoot()
        val nested = root.resolve("components/bom/src/main")
        nested.mkdirs()

        // Act
        val versionDir = FileHelper.locateNearestVersionDir(nested, root)

        // Assert
        assertThat(versionDir).isNull()
    }

    private fun createRoot() = temporaryFolder.newFolder("file-helper-test")

    private fun createVersionedDir(dir: File): File {
        dir.mkdirs()
        dir.resolve(FileHelper.VERSION_FILE).writeText(VERSION_PROPERTIES)
        return dir
    }

    private companion object {
        private val VERSION_PROPERTIES = """
            MAJOR=1
            MINOR=0
            PATCH=0
        """.trimIndent()
    }
}
