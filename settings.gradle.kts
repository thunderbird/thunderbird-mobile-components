/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
// Thunderbird Mobile Components
rootProject.name = "tmc"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        ivy("https://nodejs.org/dist") {
            name = "Node.js distributions"
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("org.nodejs", "node")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":components:bom")

// Core
include(
    ":components:core:outcome",
)

include(":quality:konsist")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    """
        Java 17+ is required to build Thunderbird for Android.
        But it found an incompatible Java version ${{JavaVersion.current()}}.

        Java Home: [${System.getProperty("java.home")}]

        Please install Java 17+ and set JAVA_HOME to the directory containing the Java 17+ installation.
        https://developer.android.com/build/jdks#jdk-config-in-studio
    """.trimIndent()
}
