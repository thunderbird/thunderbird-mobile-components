/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import com.android.build.api.dsl.KotlinMultiplatformAndroidCompilation
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.tb.library.kmp)
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("commonJvm") {
                withCompilations { it is KotlinMultiplatformAndroidCompilation }
                withJvm()
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
        }

        commonTest.dependencies {
            implementation(projects.components.core.testing)
        }

        androidHostTest.dependencies {
            implementation(libs.robolectric)
        }
    }
}
