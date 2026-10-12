/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.gradle.plugin.bom

import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependencyConstraint
import org.gradle.api.plugins.JavaPlatformExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

class BomPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("java-platform")
            pluginManager.apply("net.thunderbird.gradle.plugin.changelog")
            pluginManager.apply("net.thunderbird.gradle.plugin.versioning")
            pluginManager.apply("net.thunderbird.gradle.plugin.publishing")

            extensions.configure<JavaPlatformExtension> {
                allowDependencies()
            }

            registerPrintReleaseGroupsTask()
        }
    }

    private fun Project.registerPrintReleaseGroupsTask() {
        val apiConstraints = configurations.named("api").map { it.dependencyConstraints }
        tasks.register<PrintReleaseGroupsTask>(PrintReleaseGroupsTask.TASK_NAME) {
            group = "release"
            description = "Print the release groups of the BOM constraints, followed by the BOM itself"
            projectPaths.set(
                apiConstraints.map { constraints ->
                    // Gradle has no public API for the project path of a project constraint.
                    val paths = constraints.map { constraint ->
                        require(constraint is DefaultProjectDependencyConstraint) {
                            "BOM constraint ${constraint.group}:${constraint.name} must use api(projects.<accessor>)."
                        }
                        constraint.projectDependency.path
                    }
                    paths + project.path
                },
            )
            @Suppress("UnstableApiUsage")
            repoRootDir.set(project.isolated.rootProject.projectDirectory)
            componentsDir.set(project.layout.projectDirectory.dir(".."))
            providers.gradleProperty("releaseGroupsFile").orNull?.let { releaseGroupsFile ->
                outputFile.set(layout.file(provider { File(releaseGroupsFile) }))
            }
        }
    }
}
