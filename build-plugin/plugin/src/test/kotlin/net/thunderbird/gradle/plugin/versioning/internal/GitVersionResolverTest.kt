package net.thunderbird.gradle.plugin.versioning.internal

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.File
import kotlin.test.Test

class GitVersionResolverTest {

    private val resolver = GitVersionResolver()

    @Test
    fun `resolve returns release version when tag output contains expected tag`() {
        // Arrange
        val version = Version(major = 1, minor = 2, patch = 3)

        // Act
        val resolvedVersion = resolver.resolve(
            version = version,
            tagName = "bom-1.2.3",
            tagOutput = "bom-1.2.3\n",
        )

        // Assert
        assertThat(resolvedVersion).isEqualTo("1.2.3")
    }

    @Test
    fun `resolve returns snapshot version when tag output does not contain expected tag`() {
        // Arrange
        val version = Version(major = 1, minor = 2, patch = 3)

        // Act
        val resolvedVersion = resolver.resolve(
            version = version,
            tagName = "bom-1.2.3",
            tagOutput = "other-1.2.3\n",
        )

        // Assert
        assertThat(resolvedVersion).isEqualTo("1.2.3-SNAPSHOT")
    }

    @Test
    fun `tagName returns component name and version`() {
        // Arrange
        val versionFile = File("components/bom/version.properties")
        val version = Version(major = 1, minor = 2, patch = 3)

        // Act
        val tagName = resolver.tagName(versionFile, version)

        // Assert
        assertThat(tagName).isEqualTo("bom-1.2.3")
    }
}
