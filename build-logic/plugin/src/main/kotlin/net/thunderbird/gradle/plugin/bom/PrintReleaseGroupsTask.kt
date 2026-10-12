/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.gradle.plugin.bom

import java.io.File
import net.thunderbird.gradle.plugin.versioning.internal.VersionManager
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/**
 * Prints the release groups of a BOM release train, one per line:
 *
 * `<release tag> <version directory> <project path>...`
 *
 * The lines are also written to [outputFile] when it is set, so CI scripts can read them without
 * hiding the Gradle output.
 *
 * Projects are grouped by their nearest version.properties file, in the order they are declared.
 * Project directories are derived from project paths, relative to the repository root.
 * Fails if a project under [componentsDir] is missing from [projectPaths], so every component
 * that is published as a snapshot is also part of the BOM release train.
 */
@UntrackedTask(because = "Prints release metadata from the working tree")
abstract class PrintReleaseGroupsTask : DefaultTask() {

    @get:Input
    abstract val projectPaths: ListProperty<String>

    @get:Internal
    abstract val repoRootDir: DirectoryProperty

    @get:Internal
    abstract val componentsDir: DirectoryProperty

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun print() {
        val root = repoRootDir.get().asFile
        val missing = findProjectPaths(root, componentsDir.get().asFile) - projectPaths.get().toSet()
        require(missing.isEmpty()) {
            "Projects missing from the BOM: ${missing.sorted().joinToString()}. Add them as BOM constraints."
        }

        val groups = linkedMapOf<File, MutableList<String>>()

        for (path in projectPaths.get()) {
            val projectDir = File(root, path.removePrefix(":").replace(':', '/'))
            require(projectDir.resolve("build.gradle.kts").isFile) {
                "Project $path has no build.gradle.kts at ${projectDir.path}."
            }
            val versionFile = VersionManager(base = projectDir, root = root).sourceFile()
                ?: error("No version.properties file found for $path.")
            groups.getOrPut(versionFile.parentFile) { mutableListOf() } += path
        }

        val lines = groups.map { (versionDir, paths) ->
            val version = VersionManager(base = versionDir, root = root).get()
            val tag = "${versionDir.name}-${version.toStringValue()}"
            val relativeDir = versionDir.relativeTo(root).invariantSeparatorsPath
            "$tag $relativeDir ${paths.joinToString(" ")}"
        }
        lines.forEach(::println)
        outputFile.orNull?.asFile?.writeText(lines.joinToString(separator = "\n", postfix = "\n"))
    }

    private fun findProjectPaths(root: File, dir: File): Set<String> = dir.walkTopDown()
        .onEnter { it.name != "build" && !it.name.startsWith(".") }
        .filter { it.name == "build.gradle.kts" }
        .map { ":" + it.parentFile.relativeTo(root).invariantSeparatorsPath.replace('/', ':') }
        .toSet()

    companion object {
        const val TASK_NAME = "printReleaseGroups"
    }
}
