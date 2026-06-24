package net.thunderbird.gradle.plugin.versioning

import net.thunderbird.gradle.plugin.versioning.internal.GitVersionProvider
import net.thunderbird.gradle.plugin.versioning.internal.ProviderBackedVersion
import net.thunderbird.gradle.plugin.versioning.internal.VersionManager
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * Versioning plugin to manage project versions via version.properties files.
 *
 * Behavior:
 * - Sets the project version at configuration time based on the nearest version.properties file.
 * - Resolution is per-project by walking up from each project’s directory to the repo root
 *   and using the nearest version.properties file.
 * - Provides tasks to bump version parts and create release tags.
 *
 * Usage:
 * - Apply the plugin in your build.gradle.kts: `plugins { id("net.thunderbird.gradle.plugin.versioning") }`
 * - Define version.properties files in your project directories as needed.
 * - Use the provided tasks to manage versioning.
 *
 * Tasks:
 * - versionBumpMajor: Bump MAJOR version.
 * - versionBumpMinor: Bump MINOR version.
 * - versionBumpPatch: Bump PATCH version.
 * - printVersion: Print the effective project version.
 */
class VersioningPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureVersioning()
            registerBumpTasks()
            registerPrintVersionTask()
            registerCreateReleaseTagTask()
        }
    }

    private fun Project.configureVersioning() {
        val root = this.rootProject
        val versionManager = VersionManager(
            base = projectDir,
            root = root.projectDir,
        ) { message -> logger.warn(message) }
        val version = versionManager.get()
        val versionFile = versionManager.sourceFile()
            ?: error("No version.properties file found to resolve the project version.")
        val versionProvider = GitVersionProvider(providers).resolve(root.projectDir, versionFile, version)

        this.version = ProviderBackedVersion(versionProvider)
        logger.lifecycle("[versioning] Project version will be resolved from ${versionFile.path}")
    }

    private fun Project.registerBumpTasks() {
        tasks.register<VersionBumpTask>("versionBumpMajor") {
            group = "versioning"
            description = "Bump MAJOR and reset MINOR/PATCH to 0 in nearest version.properties"
            startDir.set(project.layout.projectDirectory)
            repoRootDir.set(project.rootProject.layout.projectDirectory)
            part.set("major")
        }
        tasks.register<VersionBumpTask>("versionBumpMinor") {
            group = "versioning"
            description = "Bump MINOR and reset PATCH to 0 in nearest version.properties"
            startDir.set(project.layout.projectDirectory)
            repoRootDir.set(project.rootProject.layout.projectDirectory)
            part.set("minor")
        }
        tasks.register<VersionBumpTask>("versionBumpPatch") {
            group = "versioning"
            description = "Bump PATCH in nearest version.properties"
            startDir.set(project.layout.projectDirectory)
            repoRootDir.set(project.rootProject.layout.projectDirectory)
            part.set("patch")
        }
    }

    private fun Project.registerPrintVersionTask() {
        tasks.register<PrintVersionTask>(PrintVersionTask.TASK_NAME) {
            group = "versioning"
            description = "Print the version resolved from the nearest version.properties"
            startDir.set(project.layout.projectDirectory)
            repoRootDir.set(project.rootProject.layout.projectDirectory)
        }
    }

    private fun Project.registerCreateReleaseTagTask() {
        tasks.register<CreateReleaseTagTask>(CreateReleaseTagTask.TASK_NAME) {
            group = "release"
            description = "Create the component release git tag from version.properties"
            startDir.set(project.layout.projectDirectory)
            repoRootDir.set(project.rootProject.layout.projectDirectory)
        }
    }
}
