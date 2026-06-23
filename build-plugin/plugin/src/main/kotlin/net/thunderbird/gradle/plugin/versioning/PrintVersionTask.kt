package net.thunderbird.gradle.plugin.versioning

import net.thunderbird.gradle.plugin.versioning.internal.GitVersionReader
import net.thunderbird.gradle.plugin.versioning.internal.VersionManager
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class PrintVersionTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val startDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val repoRootDir: DirectoryProperty

    @TaskAction
    fun print() {
        val base = startDir.asFile.get()
        val root = repoRootDir.asFile.get()
        val versionManager = VersionManager(
            base = base,
            root = root,
        ) { message -> logger.warn(message) }
        val version = versionManager.get()
        val versionFile = versionManager.sourceFile()
            ?: error("No version.properties file found to print the project version.")

        logger.lifecycle(GitVersionReader().read(root, versionFile, version))
    }

    companion object {
        const val TASK_NAME = "printVersion"
    }
}
