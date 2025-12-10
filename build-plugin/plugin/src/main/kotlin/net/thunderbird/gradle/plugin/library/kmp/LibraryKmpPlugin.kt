package net.thunderbird.gradle.plugin.library.kmp

import com.android.build.api.dsl.androidLibrary
import net.thunderbird.gradle.plugin.ProjectConfig
import net.thunderbird.gradle.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.Actions.with
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A Gradle plugin to configure a Kotlin Multiplatform Library project.
 *
 * Supported platforms include Android, iOS (x64, Arm64, Simulator Arm64), and JVM.
 *
 * It sets up the necessary plugins, targets, source sets, and dependencies.
 */
class LibraryKmpPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.kotlin.multiplatform.library")
                apply("org.jetbrains.kotlin.multiplatform")
                apply("org.jetbrains.kotlin.plugin.serialization")

                apply("net.thunderbird.gradle.plugin.publishing")

                apply("net.thunderbird.gradle.plugin.quality.coverage")
                apply("net.thunderbird.gradle.plugin.quality.detekt")
                apply("net.thunderbird.gradle.plugin.quality.spotless")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                @Suppress("UnstableApiUsage")
                androidLibrary {
                    minSdk = ProjectConfig.Android.sdkMin
                    compileSdk = ProjectConfig.Android.sdkCompile
                    compilerOptions {
                        jvmTarget.set(ProjectConfig.Compiler.jvmTarget)
                    }
                }

                iosX64()
                iosArm64()
                iosSimulatorArm64()

                jvm {
                    compilerOptions {
                        jvmTarget.set(ProjectConfig.Compiler.jvmTarget)
                    }
                }

                sourceSets {
                    androidMain.dependencies {
                        implementation(libs.bundles.shared.kmp.android)
                    }

                    commonMain.dependencies {
                        implementation(project.dependencies.platform(libs.kotlin.bom))
                        implementation(libs.bundles.shared.kmp.common)
                    }

                    commonTest.dependencies {
                        implementation(libs.bundles.shared.kmp.common.test)
                    }
                }
            }
        }
    }
}
