package net.thunderbird.gradle.plugin.library.kmp.compose

import net.thunderbird.gradle.plugin.ProjectConfig
import net.thunderbird.gradle.plugin.library.kmp.android
import net.thunderbird.gradle.plugin.library.kmp.androidHostTest
import net.thunderbird.gradle.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A Gradle plugin to configure a Kotlin Multiplatform Compose Library project.
 *
 * Supported platforms include Android, iOS (x64, Arm64, Simulator Arm64), and JVM.
 *
 * It sets up the necessary plugins, targets, source sets, dependencies, and Compose settings.
 */
class LibraryKmpComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.kotlin.multiplatform.library")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.kotlin.multiplatform")
                apply("org.jetbrains.kotlin.plugin.serialization")

                apply("net.thunderbird.gradle.plugin.changelog")
                apply("net.thunderbird.gradle.plugin.versioning")
                apply("net.thunderbird.gradle.plugin.publishing")

                apply("net.thunderbird.gradle.plugin.quality.coverage")
                apply("net.thunderbird.gradle.plugin.quality.detekt")
                apply("net.thunderbird.gradle.plugin.quality.spotless")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                explicitApi()

                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }

                android {
                    minSdk = ProjectConfig.Android.sdkMin
                    compileSdk = ProjectConfig.Android.sdkCompile

                    androidResources.enable = true

                    withHostTest {
                        isIncludeAndroidResources = true
                    }

                    compilerOptions {
                        jvmTarget.set(ProjectConfig.Compiler.jvmTarget)
                    }
                }

                iosArm64()
                iosSimulatorArm64()

                jvm {
                    compilerOptions {
                        jvmTarget.set(ProjectConfig.Compiler.jvmTarget)
                    }
                }

                sourceSets {
                    commonMain.dependencies {
                        implementation(project.dependencies.platform(libs.kotlin.bom))
                        implementation(libs.bundles.shared.kmp.common)
                        implementation(libs.bundles.shared.kmp.compose.common)
                    }
                    commonTest.dependencies {
                        implementation(libs.bundles.shared.kmp.common.test)
                        implementation(libs.bundles.shared.kmp.compose.common.test)
                    }

                    androidMain.dependencies {
                        implementation(libs.bundles.shared.kmp.android)
                        implementation(libs.bundles.shared.kmp.compose.android)
                    }
                    androidHostTest.dependencies {
                        implementation(libs.bundles.shared.kmp.android.test)
                        implementation(libs.bundles.shared.kmp.compose.android.test)
                    }

                    jvmMain.dependencies {
                        implementation(libs.bundles.shared.kmp.jvm)
                        implementation(libs.bundles.shared.kmp.compose.jvm)
                    }
                    jvmTest.dependencies {
                        implementation(libs.bundles.shared.kmp.jvm.test)
                        implementation(libs.bundles.shared.kmp.compose.jvm.test)
                    }

                    nativeMain.dependencies {
                        implementation(libs.bundles.shared.kmp.native)
                        implementation(libs.bundles.shared.kmp.compose.native)
                    }
                    nativeTest.dependencies {
                        implementation(libs.bundles.shared.kmp.native.test)
                        implementation(libs.bundles.shared.kmp.compose.native.test)
                    }
                }
            }
        }
    }
}
