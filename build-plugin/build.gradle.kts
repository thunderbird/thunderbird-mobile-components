plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(plugin(libs.plugins.kotlin.multiplatform))
    implementation(plugin(libs.plugins.kotlin.parcelize))
    implementation(plugin(libs.plugins.kotlin.serialization))

    implementation(plugin(libs.plugins.android.application))
    implementation(plugin(libs.plugins.android.library))

    implementation(plugin(libs.plugins.compose.compiler))
    implementation(plugin(libs.plugins.compose.multiplatform))
    implementation(plugin(libs.plugins.compose.hot.reload))

    implementation(plugin(libs.plugins.dependency.check))
    implementation(plugin(libs.plugins.detekt))
    implementation(plugin(libs.plugins.spotless))

    compileOnly(libs.android.tools.common)

    // This defines the used Kotlin version for all Plugin dependencies
    // and ensures that transitive dependencies are aligned on one version.
    implementation(platform(libs.kotlin.gradle.bom))
}

fun plugin(provider: Provider<PluginDependency>) = with(provider.get()) {
    "$pluginId:$pluginId.gradle.plugin:$version"
}
