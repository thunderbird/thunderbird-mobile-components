import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose.hot-reload")
    id("thunderbird.quality.detekt.typed")
    id("thunderbird.quality.spotless")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(ThunderbirdProjectConfig.Compiler.jvmTarget)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(ThunderbirdProjectConfig.Compiler.jvmTarget)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(libs.bundles.shared.kmp.android)
            implementation(libs.androidx.activity.compose)
            implementation(compose.preview)
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

android {
    configureSharedConfig(project)

    defaultConfig {
        targetSdk = ThunderbirdProjectConfig.Android.Application.sdkTarget
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
    }
}
