package net.thunderbird.gradle.plugin

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

object ProjectConfig {

    const val group = "net.thunderbird"

    object Android {
        const val sdkMin = 21

        // Only needed for application
        const val sdkTarget = 35

        const val sdkCompile = 36
    }

    object Compiler {
        val javaCompatibility = JavaVersion.VERSION_11
        val jvmTarget = JvmTarget.JVM_11
    }

    object Publishing {
        const val year = "2025"
        const val url = "https://github.com/thunderbird/thunderbird-mobile-components"

        const val licenseName = "MPL-2.0"
        const val licenseUrl = "https://www.mozilla.org/en-US/MPL/2.0/"
        const val licenseDistribution = "https://www.mozilla.org/en-US/MPL/2.0/"

        const val developerId = "thunderbird"
        const val developerName = "Thunderbird Mobile Team"
        const val developerEmail = "mobile@thunderbird.net"

        const val scmUrl = "https://github.com/thunderbird/thunderbird-mobile-components"
        const val scmConnection = "scm:git:git://github.com/thunderbird/thunderbird-mobile-components.git"
        const val scmDeveloperConnection = "scm:git:ssh://git@github.com/thunderbird/thunderbird-mobile-components.git"
    }
}
