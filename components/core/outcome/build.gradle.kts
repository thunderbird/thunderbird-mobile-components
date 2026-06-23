plugins {
    alias(libs.plugins.tb.library.kmp)
    alias(libs.plugins.testBalloon)
}

kotlin {
    @Suppress("UnstableApiUsage")
    android {
        namespace = "net.thunderbird.components.core.outcome"
    }
}
