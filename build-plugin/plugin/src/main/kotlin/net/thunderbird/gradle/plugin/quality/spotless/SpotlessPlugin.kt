package net.thunderbird.gradle.plugin.quality.spotless

import com.diffplug.gradle.spotless.SpotlessExtension
import kotlinEditorConfigOverride
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * A Gradle plugin to configure Spotless code formatting for Kotlin, Kotlin Gradle scripts, Markdown,
 * and miscellaneous files like .gitignore.
 */
class SpotlessPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.diffplug.spotless")

            if (path == ":") {
                configureSpotlessRoot()
            } else {
                configureSpotless()
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private fun Project.configureSpotless() {
        extensions.configure<SpotlessExtension> {
            val editorConfigPath = isolated.rootProject.projectDirectory.file(".editorconfig").asFile.path

            kotlin {
                target(
                    "src/*/kotlin/*.kt",
                    "src/*/kotlin/**/*.kt",
                )

                ktlint()
                    .setEditorConfigPath(editorConfigPath)
                    .editorConfigOverride(kotlinEditorConfigOverride)
            }

            kotlinGradle {
                target(
                    "*.gradle.kts",
                )

                ktlint()
                    .setEditorConfigPath(editorConfigPath)
                    .editorConfigOverride(
                        mapOf(
                            "ktlint_code_style" to "intellij_idea",
                            "ktlint_standard_function-expression-body" to "disabled",
                            "ktlint_standard_function-signature" to "disabled",
                        ),
                    )
            }

            flexmark {
                target(
                    "*.md",
                )
                flexmark()
            }

            format("misc") {
                target(".gitignore")
                trimTrailingWhitespace()
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private fun Project.configureSpotlessRoot() {
        extensions.configure<SpotlessExtension> {
            val editorConfigPath = isolated.rootProject.projectDirectory.file(".editorconfig").asFile.path

            kotlin {
                target(
                    "build-plugin/plugin/src/*/kotlin/*.kt",
                    "build-plugin/plugin/src/*/kotlin/**/*.kt",
                )
                ktlint()
                    .setEditorConfigPath(editorConfigPath)
                    .editorConfigOverride(kotlinEditorConfigOverride)
            }

            kotlinGradle {
                target(
                    "*.gradle.kts",
                    "build-plugin/*.gradle.kts",
                    "build-plugin/plugin/*.gradle.kts",
                )

                ktlint()
                    .setEditorConfigPath(editorConfigPath)
                    .editorConfigOverride(
                        mapOf(
                            "ktlint_code_style" to "intellij_idea",
                            "ktlint_standard_function-expression-body" to "disabled",
                            "ktlint_standard_function-signature" to "disabled",
                        ),
                    )
            }

            flexmark {
                target(
                    "*.md",
                    "docs/*.md",
                    "docs/**/*.md",
                )
                flexmark()
            }

            format("misc") {
                target(".gitignore")
                trimTrailingWhitespace()
            }
        }
    }
}
