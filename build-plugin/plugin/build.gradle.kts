plugins {
    `kotlin-dsl`
}

group = "net.thunderbird.gradle.plugin"

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    compileOnly(plugin(libs.plugins.android.application))

    compileOnly(plugin(libs.plugins.kotlin.multiplatform))
    compileOnly(plugin(libs.plugins.kotlin.serialization))

    compileOnly(plugin(libs.plugins.compose.compiler))
    compileOnly(plugin(libs.plugins.compose.multiplatform))
    compileOnly(plugin(libs.plugins.compose.hot.reload))

    implementation(plugin(libs.plugins.dependency.check))

    implementation(plugin(libs.plugins.kover))
    implementation(plugin(libs.plugins.detekt))
    implementation(plugin(libs.plugins.spotless))
}

gradlePlugin {
    plugins {
        register("AppKmpCompose") {
            id = "net.thunderbird.gradle.plugin.app.kmp.compose"
            implementationClass = "net.thunderbird.gradle.plugin.app.kmp.compose.AppKmpComposePlugin"
        }
        register("LibraryKmp") {
            id = "net.thunderbird.gradle.plugin.library.kmp"
            implementationClass = "net.thunderbird.gradle.plugin.library.kmp.LibraryKmpPlugin"
        }
        register("LibraryKmpCompose") {
            id = "net.thunderbird.gradle.plugin.library.kmp.compose"
            implementationClass = "net.thunderbird.gradle.plugin.library.kmp.compose.LibraryKmpComposePlugin"
        }

        register("DependencyCheck") {
            id = "net.thunderbird.gradle.plugin.dependency.check"
            implementationClass = "net.thunderbird.gradle.plugin.dependency.check.DependencyCheckPlugin"
        }

        register("QualityCodeCoverage") {
            id = "net.thunderbird.gradle.plugin.quality.coverage"
            implementationClass = "net.thunderbird.gradle.plugin.quality.coverage.CodeCoveragePlugin"
        }
        register("QualityDetekt") {
            id = "net.thunderbird.gradle.plugin.quality.detekt"
            implementationClass = "net.thunderbird.gradle.plugin.quality.detekt.DetektPlugin"
        }
        register("QualitySpotless") {
            id = "net.thunderbird.gradle.plugin.quality.spotless"
            implementationClass = "net.thunderbird.gradle.plugin.quality.spotless.SpotlessPlugin"
        }
    }
}

private fun plugin(provider: Provider<PluginDependency>) = with(provider.get()) {
    "$pluginId:$pluginId.gradle.plugin:$version"
}
