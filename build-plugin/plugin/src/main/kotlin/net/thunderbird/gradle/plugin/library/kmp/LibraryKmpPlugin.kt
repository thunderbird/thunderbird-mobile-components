package net.thunderbird.gradle.plugin.library.kmp

import net.thunderbird.gradle.plugin.ProjectConfig
import net.thunderbird.gradle.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

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
                apply("org.jetbrains.dokka")
                apply("org.jetbrains.kotlin.multiplatform")
                apply("org.jetbrains.kotlin.plugin.serialization")

                apply("net.thunderbird.gradle.plugin.changelog")
                apply("net.thunderbird.gradle.plugin.versioning")
                apply("net.thunderbird.gradle.plugin.publishing")

                apply("net.thunderbird.gradle.plugin.quality.coverage")
                apply("net.thunderbird.gradle.plugin.quality.detekt")
                apply("net.thunderbird.gradle.plugin.quality.spotless")
            }

            @OptIn(ExperimentalAbiValidation::class)
            extensions.configure<KotlinMultiplatformExtension> {
                explicitApi()
                abiValidation()

                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }

                android {
                    minSdk = ProjectConfig.Android.sdkMin
                    compileSdk = ProjectConfig.Android.sdkCompile

                    withHostTest { }

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
                    }
                    commonTest.dependencies {
                        implementation(libs.bundles.shared.kmp.common.test)
                    }

                    androidMain.dependencies {
                        implementation(libs.bundles.shared.kmp.android)
                    }
                    androidHostTest.dependencies {
                        implementation(libs.bundles.shared.kmp.android.test)
                    }

                    jvmMain.dependencies {
                        implementation(libs.bundles.shared.kmp.jvm)
                    }
                    jvmTest.dependencies {
                        implementation(libs.bundles.shared.kmp.jvm.test)
                    }

                    nativeMain.dependencies {
                        implementation(libs.bundles.shared.kmp.native)
                    }
                    nativeTest.dependencies {
                        implementation(libs.bundles.shared.kmp.native.test)
                    }
                }
            }
        }
    }
}
