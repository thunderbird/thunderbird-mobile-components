package net.thunderbird.gradle.plugin.publishing

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class PublishingCoordinatesTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `configurePublishedGroup uses parent path as group`() {
        // Arrange
        val rootProject = ProjectBuilder.builder()
            .withProjectDir(temporaryFolder.newFolder("publishing-group-test"))
            .withName("root")
            .build()
        val componentProject = ProjectBuilder.builder()
            .withName("components")
            .withParent(rootProject)
            .build()
        val publishedProject = ProjectBuilder.builder()
            .withName("example")
            .withParent(componentProject)
            .build()

        // Act
        publishedProject.configurePublishedGroup()

        // Assert
        assertThat(publishedProject.group.toString()).isEqualTo("net.thunderbird.components")
    }

    @Test
    fun `configurePublishedGroup includes nested parent path segments`() {
        // Arrange
        val rootProject = ProjectBuilder.builder()
            .withProjectDir(temporaryFolder.newFolder("nested-publishing-group-test"))
            .withName("root")
            .build()
        val componentProject = ProjectBuilder.builder()
            .withName("components")
            .withParent(rootProject)
            .build()
        val featureProject = ProjectBuilder.builder()
            .withName("feature")
            .withParent(componentProject)
            .build()
        val publishedProject = ProjectBuilder.builder()
            .withName("sync")
            .withParent(featureProject)
            .build()

        // Act
        publishedProject.configurePublishedGroup()

        // Assert
        assertThat(publishedProject.group.toString()).isEqualTo("net.thunderbird.components.feature")
    }

    @Test
    fun `configurePublishedGroup keeps explicitly configured group`() {
        // Arrange
        val rootProject = ProjectBuilder.builder()
            .withProjectDir(temporaryFolder.newFolder("explicit-publishing-group-test"))
            .withName("root")
            .build()
        val componentProject = ProjectBuilder.builder()
            .withName("components")
            .withParent(rootProject)
            .build()
        val publishedProject = ProjectBuilder.builder()
            .withName("example")
            .withParent(componentProject)
            .build()
        publishedProject.group = "custom.group"

        // Act
        publishedProject.configurePublishedGroup()

        // Assert
        assertThat(publishedProject.group.toString()).isEqualTo("custom.group")
    }
}
