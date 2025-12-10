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
 * Configuration-cache–friendly task to bump version parts in the nearest version.properties.
 */
abstract class VersionBumpTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val startDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val repoRootDir: DirectoryProperty

    /** major | minor | patch */
    @get:Input
    abstract val part: Property<String>

    @TaskAction
    fun bump() {
        val base = startDir.asFile.get()
        val root = repoRootDir.asFile.get()
        val versionManager = VersionManager(
            base = base,
            root = root,
        )

        val version = versionManager.get()

        val bumped = when (part.get().lowercase()) {
            "major" -> version.bumpMajor()
            "minor" -> version.bumpMinor()
            "patch" -> version.bumpPatch()
            else -> throw IllegalArgumentException(
                "Invalid part to bump: ${part.get()}. Expected: major, minor, or patch.",
            )
        }

        versionManager.update(bumped)

        logger.lifecycle("[versioning] Bumped ${part.get()} -> ${bumped.toStringValue()}")
    }
}
