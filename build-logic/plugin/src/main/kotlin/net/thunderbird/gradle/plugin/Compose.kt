package net.thunderbird.gradle.plugin

import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposePlugin.Dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal val KotlinMultiplatformExtension.compose: Dependencies
    get() = (this as ExtensionAware).extensions.getByType<Dependencies>()
