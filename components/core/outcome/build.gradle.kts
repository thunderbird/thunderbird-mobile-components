plugins {
    alias(libs.plugins.tb.library.kmp)
}

kotlin {
    @Suppress("UnstableApiUsage")
    android {
        namespace = "net.thunderbird.components.core.outcome"
    }
    sourceSets {
        commonTest.dependencies {
            implementation(libs.assertk)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

codeCoverage {
    branchCoverage = 28
    lineCoverage = 53
}
