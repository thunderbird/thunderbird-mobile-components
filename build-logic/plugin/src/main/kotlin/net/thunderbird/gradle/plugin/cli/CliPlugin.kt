/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.gradle.plugin.cli

import net.thunderbird.gradle.plugin.ProjectConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * A Gradle plugin to configure a CLI Application project.
 */
class CliPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.jvm")
                apply("application")
                apply("org.jetbrains.kotlin.plugin.serialization")
                apply("de.infix.testBalloon")

                apply("net.thunderbird.gradle.plugin.quality.detekt")
                apply("net.thunderbird.gradle.plugin.quality.spotless")
            }

            extensions.configure<KotlinJvmProjectExtension> {
                explicitApi()
                jvmToolchain {
                    languageVersion.set(
                        JavaLanguageVersion.of(ProjectConfig.Compiler.javaCompatibility.majorVersion),
                    )
                }
            }
        }
    }
}
