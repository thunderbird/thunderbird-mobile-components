package net.thunderbird.gradle.plugin.publishing

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import java.util.Properties
import net.thunderbird.gradle.plugin.ProjectConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure

/**
 * Publishing plugin configuration.
 *
 * Applies the Maven Publish plugin, sets up publishing repositories,
 * and configures POM metadata for publishing artifacts.
 *
 * It adds a local Maven repository and a build directory repository for local builds.
 * Also configures publishing to Maven Central with signing.
 *
 * Requires signing properties to be provided in a `.signing/signing.properties` file
 * at the root of the project with following keys:
 *
 * - signing.keyId - ID of the signing key
 * - signing.password - Password for the signing key
 * - signing.secretKeyRingFile - Path to the secret key ring file
 */
class PublishingPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            ensureProjectGroup()
            loadSigningProperties()

            pluginManager.apply("com.vanniktech.maven.publish")

            configurePublishing()
            configurePublish()
        }
    }

    private fun Project.ensureProjectGroup() {
        group = group.toString().replace("tmc", ProjectConfig.group + ".")
    }

    private fun Project.loadSigningProperties() {
        val signingPropsFile = rootProject.file(".signing/signing.properties")
        if (signingPropsFile.exists()) {
            val properties = Properties()
            signingPropsFile.inputStream().use { properties.load(it) }
            properties.forEach { (key, value) ->
                project.extensions.extraProperties[key.toString()] = value
            }
            logger.lifecycle("[publishing] Loaded signing properties from ${signingPropsFile.path}")
        } else {
            logger.lifecycle("[publishing] No signing properties file found at ${signingPropsFile.path}")
        }
    }

    private fun Project.configurePublishing() {
        extensions.configure<PublishingExtension>("publishing") {
            repositories {
                mavenLocal()

                maven {
                    name = "localBuild"
                    url = rootProject.layout.buildDirectory.dir("maven-repo").get().asFile.toURI()
                }
            }
        }
    }

    private fun Project.configurePublish() {
        extensions.configure<MavenPublishBaseExtension> {
            coordinates(
                groupId = project.group.toString(),
                artifactId = project.name,
                version = version.toString(),
            )

            pom {
                inceptionYear.set(ProjectConfig.Publishing.year)
                url.set(ProjectConfig.Publishing.url)

                licenses {
                    license {
                        name.set(ProjectConfig.Publishing.licenseName)
                        url.set(ProjectConfig.Publishing.licenseUrl)
                        distribution.set(ProjectConfig.Publishing.licenseDistribution)
                    }
                }

                developers {
                    developer {
                        id.set(ProjectConfig.Publishing.developerId)
                        name.set(ProjectConfig.Publishing.developerName)
                        email.set(ProjectConfig.Publishing.developerEmail)
                    }
                }

                scm {
                    url.set(ProjectConfig.Publishing.scmUrl)
                    connection.set(ProjectConfig.Publishing.scmConnection)
                    developerConnection.set(ProjectConfig.Publishing.scmDeveloperConnection)
                }
            }

            publishToMavenCentral()

            signAllPublications()
        }
    }
}
