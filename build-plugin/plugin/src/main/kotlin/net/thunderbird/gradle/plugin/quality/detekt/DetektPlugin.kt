package net.thunderbird.gradle.plugin.quality.detekt

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import net.thunderbird.gradle.plugin.ProjectConfig
import net.thunderbird.gradle.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * Detekt plugin configuration.
 *
 *
 */
class DetektPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.gitlab.arturbosch.detekt")

            dependencies {
                add("detektPlugins", libs.detekt.plugin.compose)
            }

            if (this == rootProject) {
                configureRootDetektTasks()
            } else {
                configureDetekt()
                configureDetektTasks()
            }
        }
    }

    private fun Project.configureDetekt() {
        extensions.configure<DetektExtension>("detekt") {
            config.setFrom(project.rootProject.files("config/detekt/detekt.yml"))

            val name = project.path.replace(":", "-").replace("/", "-")
            baseline = project.rootProject.file("config/detekt/detekt-baseline$name.xml")

            ignoredBuildTypes = listOf("release")
        }
    }

    private fun Project.configureDetektTasks() {
        with(tasks) {
            withType<Detekt>().configureEach {
                jvmTarget = ProjectConfig.Compiler.javaCompatibility.toString()

                exclude(defaultExcludes)

                reports {
                    html.required.set(true)
                    sarif.required.set(true)
                    xml.required.set(true)
                }

                tasks.getByName("build").dependsOn(this)
            }

            withType<DetektCreateBaselineTask>().configureEach {
                jvmTarget = ProjectConfig.Compiler.javaCompatibility.toString()

                exclude(defaultExcludes)
            }

            register("detektAll") {
                group = "verification"
                description = "Runs detekt on this project"

                dependsOn(tasks.withType<Detekt>())
            }
        }
    }

    private fun Project.configureRootDetektTasks() {
        with(tasks) {
            register("detektAll") {
                group = "verification"
                description = "Runs detekt on the whole project"

                allprojects {
                    this@register.dependsOn(tasks.withType<Detekt>())
                }
            }
        }
    }
}

private val defaultExcludes = listOf(
    "**/.gradle/**",
    "**/.idea/**",
    "**/build/**",
    ".github/**",
    "gradle/**",
)
