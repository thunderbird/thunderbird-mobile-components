package net.thunderbird.gradle.plugin.versioning.internal

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.util.Properties
import kotlin.test.Test

class VersionPropertiesMapperTest {

    private val mapper = VersionPropertiesMapper()

    @Test
    fun `to maps valid properties to version`() {
        // Arrange
        val properties = properties(
            "MAJOR" to "1",
            "MINOR" to "2",
            "PATCH" to "3",
        )

        // Act
        val version = mapper.to(properties)

        // Assert
        assertThat(version).isEqualTo(Version(major = 1, minor = 2, patch = 3))
    }

    @Test
    fun `to returns null when required property is missing`() {
        // Arrange
        val properties = properties(
            "MAJOR" to "1",
            "PATCH" to "3",
        )

        // Act
        val version = mapper.to(properties)

        // Assert
        assertThat(version).isNull()
    }

    @Test
    fun `to returns null when numeric property is invalid`() {
        // Arrange
        val properties = properties(
            "MAJOR" to "one",
            "MINOR" to "2",
            "PATCH" to "3",
        )

        // Act
        val version = mapper.to(properties)

        // Assert
        assertThat(version).isNull()
    }

    @Test
    fun `from maps version to properties`() {
        // Arrange
        val version = Version(major = 1, minor = 2, patch = 3)

        // Act
        val properties = mapper.from(version)

        // Assert
        assertThat(properties.toMap()).isEqualTo(
            mapOf(
                "MAJOR" to "1",
                "MINOR" to "2",
                "PATCH" to "3",
            ),
        )
    }

    private fun properties(vararg values: Pair<String, String>): Properties {
        return Properties().apply {
            values.forEach { (key, value) -> setProperty(key, value) }
        }
    }

    private fun Properties.toMap(): Map<String, String> {
        return entries.associate { (key, value) -> key.toString() to value.toString() }
    }
}
