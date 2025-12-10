package net.thunderbird.gradle.plugin.app.kmp.compose

import com.android.build.api.dsl.ApplicationExtension
import net.thunderbird.gradle.plugin.ProjectConfig
import net.thunderbird.gradle.plugin.compose
import net.thunderbird.gradle.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class AppKmpComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.kotlin.multiplatform")
                apply("org.jetbrains.kotlin.plugin.serialization")

                apply("net.thunderbird.gradle.plugin.quality.coverage")
                apply("net.thunderbird.gradle.plugin.quality.detekt")
                apply("net.thunderbird.gradle.plugin.quality.spotless")
            }

            configureAppKmpCompose()
            configureAndroidApp()
        }
    }

    private fun Project.configureAppKmpCompose() {
        extensions.configure<KotlinMultiplatformExtension> {
            androidTarget {
                compilerOptions {
                    jvmTarget.set(ProjectConfig.Compiler.jvmTarget)
                }
            }

            listOf(
                iosX64(),
                iosArm64(),
                iosSimulatorArm64(),
            ).forEach { iosTarget ->
                iosTarget.binaries.framework {
                    baseName = "ComposeApp"
                    isStatic = true
                }
            }

            jvm("desktop") {
                compilerOptions {
                    jvmTarget.set(ProjectConfig.Compiler.jvmTarget)
                }
            }

            sourceSets {
                val desktopMain = getByName("desktopMain")

                androidMain.dependencies {
                    implementation(libs.bundles.shared.kmp.android)
                    implementation(libs.androidx.activity.compose)
                }

                commonMain.dependencies {
                    implementation(project.dependencies.platform(libs.kotlin.bom))
                    implementation(libs.bundles.shared.kmp.common)
                    implementation(libs.bundles.shared.kmp.compose)

                    implementation(compose.runtime)
                    implementation(compose.foundation)
                    implementation(compose.ui)
                    implementation(compose.components.resources)
                    implementation(compose.components.uiToolingPreview)
                }

                commonTest.dependencies {
                    implementation(libs.bundles.shared.kmp.common.test)
                }

                desktopMain.dependencies {
                    implementation(compose.desktop.currentOs)
                    implementation(libs.kotlinx.coroutines.swing)
                }
            }
        }
    }

    private fun Project.configureAndroidApp() {
        extensions.configure<ApplicationExtension> {
            compileSdk = ProjectConfig.Android.sdkCompile

            defaultConfig {
                minSdk = ProjectConfig.Android.sdkMin
                targetSdk = ProjectConfig.Android.sdkTarget
            }

            buildTypes {
                getByName("release") {
                    isMinifyEnabled = false
                }
            }

            compileOptions {
                sourceCompatibility = ProjectConfig.Compiler.javaCompatibility
                targetCompatibility = ProjectConfig.Compiler.javaCompatibility
            }

            tasks.withType<KotlinCompile>().configureEach {
                compilerOptions {
                    jvmTarget.set(ProjectConfig.Compiler.jvmTarget)
                }
            }

            lint {
                warningsAsErrors = false
                abortOnError = true
                checkDependencies = true
                lintConfig = project.file("${project.rootProject.projectDir}/config/lint/lint.xml")
            }

            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }
        }
    }
}
