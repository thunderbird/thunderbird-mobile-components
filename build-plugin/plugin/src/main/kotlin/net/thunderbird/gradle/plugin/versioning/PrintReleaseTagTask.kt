package net.thunderbird.gradle.plugin.versioning

import net.thunderbird.gradle.plugin.versioning.internal.VersionManager
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class PrintReleaseTagTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val startDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val repoRootDir: DirectoryProperty

    @TaskAction
    fun print() {
        val versionManager = VersionManager(
            base = startDir.get().asFile,
            root = repoRootDir.get().asFile,
        )
        val version = versionManager.get()
        val versionFile = versionManager.sourceFile()
            ?: error("No version.properties file found to print the release tag.")

        logger.lifecycle("${versionFile.parentFile.name}-${version.toStringValue()}")
    }

    companion object {
        const val TASK_NAME = "printReleaseTag"
    }
}
