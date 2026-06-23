package net.thunderbird.gradle.plugin.quality.detekt

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import net.thunderbird.gradle.plugin.ProjectConfig
import net.thunderbird.gradle.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/**
 * Detekt plugin configuration.
 *
 * Applies the Detekt plugin, sets up configuration, and defines tasks for static code analysis.
 */
class DetektPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("base")
            pluginManager.apply("dev.detekt")

            dependencies {
                add("detektPlugins", libs.detekt.plugin.compose)
            }

            configureDetekt()
            configureDetektTasks()
        }
    }

    @Suppress("UnstableApiUsage")
    private fun Project.configureDetekt() {
        extensions.configure<DetektExtension>("detekt") {
            config.setFrom(project.isolated.rootProject.projectDirectory.file("config/detekt/detekt.yml"))

            ignoredBuildTypes = listOf("release")
        }
    }

    private fun Project.configureDetektTasks() {
        with(tasks) {
            withType<Detekt>().configureEach {
                if (name.contains("androidHostTest", ignoreCase = true)) {
                    enabled = false
                }

                jvmTarget = ProjectConfig.Compiler.jvmTarget.target

                exclude(defaultExcludes)

                reports {
                    checkstyle.required.set(false)
                    html.required.set(false)
                    sarif.required.set(true)
                    markdown.required.set(true)
                }
            }

            withType<DetektCreateBaselineTask>().configureEach {
                if (name.contains("androidHostTest", ignoreCase = true)) {
                    enabled = false
                }

                jvmTarget = ProjectConfig.Compiler.jvmTarget.target

                exclude(defaultExcludes)
            }

            val detektAll = register("detektAll") {
                group = "verification"
                description = "Runs detekt on this project"

                dependsOn(tasks.withType<Detekt>())
            }

            named("check") {
                dependsOn(detektAll)
            }
        }
    }
}

private val defaultExcludes = listOf(
    "**/.gradle/**",
    "**/.idea/**",
    "**/build/**",
    "**/generated/**",
    ".github/**",
    "gradle/**",
)
