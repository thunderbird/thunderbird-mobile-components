package net.thunderbird.gradle.plugin.bom

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlatformExtension
import org.gradle.kotlin.dsl.configure

class BomPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("java-platform")
            pluginManager.apply("net.thunderbird.gradle.plugin.changelog")
            pluginManager.apply("net.thunderbird.gradle.plugin.versioning")
            pluginManager.apply("net.thunderbird.gradle.plugin.publishing")

            extensions.configure<JavaPlatformExtension> {
                allowDependencies()
            }
        }
    }
}
