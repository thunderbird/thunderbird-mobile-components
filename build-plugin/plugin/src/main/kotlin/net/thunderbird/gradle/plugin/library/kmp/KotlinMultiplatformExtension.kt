package net.thunderbird.gradle.plugin.library.kmp

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import net.thunderbird.gradle.plugin.ProjectConfig
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

val NamedDomainObjectContainer<KotlinSourceSet>.androidHostTest: NamedDomainObjectProvider<KotlinSourceSet>
    get() = this.named<KotlinSourceSet>("androidHostTest")

fun KotlinMultiplatformExtension.android(configure: Action<KotlinMultiplatformAndroidLibraryTarget>) {
    (this as ExtensionAware).extensions.configure("android", configure)
}

fun KotlinMultiplatformAndroidLibraryTarget.namespaceByPath(project: Project) {
    val pathSegments = project.path.split(':')
        .filter { it.isNotBlank() }
        .flatMap { it.split('-') }
        .filter { it.isNotBlank() }

    namespace = listOf(ProjectConfig.group)
        .plus(pathSegments)
        .joinToString(separator = ".")
}

/**
 * Creates a dependency with the given configuration.
 *
 * This is a workaround for kotlin-multiplatform's implementation() function not supporting excludes when
 * declaring dependencies from a version catalog.
 *
 * Example:
 *
 * ```kotlin
 * implementationWithExcludes(libs.foo.bar) {
 *     exclude(group = "org.foo.bar", module = "dependency")
 * }
 * ```
 *
 * @param dependency the dependency to create
 * @param configure the configuration to apply to the dependency
 */
fun KotlinDependencyHandler.implementationWithExcludes(
    dependency: Provider<MinimalExternalModuleDependency>,
    configure: ExternalModuleDependency.() -> Unit,
) {
    val copy = dependency.get().copy()
    copy.configure()
    implementation(copy)
}
