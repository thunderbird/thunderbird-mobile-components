package net.thunderbird.gradle.plugin.versioning.internal

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class VersionTest {

    @Test
    fun `toStringValue returns base semantic version`() {
        // Arrange
        val version = Version(major = 1, minor = 2, patch = 3)

        // Act
        val value = version.toStringValue()

        // Assert
        assertThat(value).isEqualTo("1.2.3")
    }

    @Test
    fun `bumpMajor increments major and resets minor and patch`() {
        // Arrange
        val version = Version(major = 1, minor = 2, patch = 3)

        // Act
        val bumped = version.bumpMajor()

        // Assert
        assertThat(bumped).isEqualTo(Version(major = 2, minor = 0, patch = 0))
    }

    @Test
    fun `bumpMinor increments minor and resets patch`() {
        // Arrange
        val version = Version(major = 1, minor = 2, patch = 3)

        // Act
        val bumped = version.bumpMinor()

        // Assert
        assertThat(bumped).isEqualTo(Version(major = 1, minor = 3, patch = 0))
    }

    @Test
    fun `bumpPatch increments patch`() {
        // Arrange
        val version = Version(major = 1, minor = 2, patch = 3)

        // Act
        val bumped = version.bumpPatch()

        // Assert
        assertThat(bumped).isEqualTo(Version(major = 1, minor = 2, patch = 4))
    }
}
