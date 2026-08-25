package net.thunderbird.gradle.plugin.versioning

import java.io.File
import net.thunderbird.gradle.plugin.versioning.internal.VersionManager
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Creates the component release tag for the version resolved from version.properties.
 */
abstract class CreateReleaseTagTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val startDir: DirectoryProperty

    @get:Internal
    abstract val repoRootDir: DirectoryProperty

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun createReleaseTag() {
        val versionManager = VersionManager(
            base = startDir.get().asFile,
            root = repoRootDir.get().asFile,
        ) { message -> logger.warn(message) }
        val version = versionManager.get()

        val versionFile = versionManager.sourceFile()
            ?: error("No version.properties file found to create a release tag.")
        val tagName = "${versionFile.parentFile.name}-${version.toStringValue()}"
        val repoRoot = repoRootDir.get().asFile

        require(!tagExists(repoRoot, tagName)) {
            "Release tag '$tagName' already exists."
        }

        runGit(repoRoot, "tag", tagName)

        logger.lifecycle("[versioning] Created release tag $tagName")
    }

    private fun tagExists(repoRoot: File, tagName: String): Boolean {
        val command =
            listOf("git", "-C", repoRoot.absolutePath, "rev-parse", "--verify", "--quiet", "refs/tags/$tagName")
        return ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
    }

    private fun runGit(repoRoot: File, vararg args: String) {
        val command = listOf("git", "-C", repoRoot.absolutePath) + args
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        process.inputStream.bufferedReader().use { reader ->
            val output = reader.readText()
            val exitCode = process.waitFor()
            check(exitCode == 0) {
                "Command failed ($exitCode): ${command.joinToString(" ")}\n$output"
            }
        }
    }

    companion object {
        const val TASK_NAME = "createReleaseTag"
    }
}
