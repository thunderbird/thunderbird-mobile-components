package net.thunderbird.gradle.plugin.versioning

import java.io.File
import net.thunderbird.gradle.plugin.versioning.internal.VersionManager
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Configuration-cache–friendly task to toggle the SNAPSHOT flag in the nearest version.properties.
 */
abstract class ToggleSnapshotTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val startDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val repoRootDir: DirectoryProperty

    @TaskAction
    fun toggle() {
        val base = startDir.asFile.get()
        val root = repoRootDir.asFile.get()
        val versionManager = VersionManager(
            base = base,
            root = root,
        )

        val version = versionManager.get()
        val toggled = version.toggleSnapshot()

        versionManager.update(toggled)

        logger.lifecycle("[versioning] Set SNAPSHOT=${toggled.snapshot} for (version=${toggled.toStringValue()})")
    }

    private fun relativeToRoot(file: File, root: File): String = try {
        file.relativeTo(root).path
    } catch (_: IllegalArgumentException) {
        file.path
    }
}
