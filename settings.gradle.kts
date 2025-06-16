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
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    """
        Java 17+ is required to build Thunderbird for Android.
        But it found an incompatible Java version ${{JavaVersion.current()}}.

        Java Home: [${System.getProperty("java.home")}]

        Please install Java 17+ and set JAVA_HOME to the directory containing the Java 17+ installation.
        https://developer.android.com/build/jdks#jdk-config-in-studio
    """.trimIndent()
}
