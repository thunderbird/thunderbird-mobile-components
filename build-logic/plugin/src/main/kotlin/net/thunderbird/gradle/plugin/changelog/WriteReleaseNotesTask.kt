package net.thunderbird.gradle.plugin.changelog

import net.thunderbird.gradle.plugin.changelog.internal.ChangelogEntry
import net.thunderbird.gradle.plugin.changelog.internal.ChangelogManager
import net.thunderbird.gradle.plugin.changelog.internal.Release
import net.thunderbird.gradle.plugin.versioning.internal.VersionManager
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Writes the finalized component changelog section for the current release.
 */
abstract class WriteReleaseNotesTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val changelogFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val versionFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val releaseVersion: Property<String>

    @TaskAction
    fun writeReleaseNotes() {
        val version = resolveReleaseVersion()
        val changelog = ChangelogManager(changelogFile.get().asFile).get()
        val release = changelog.releases.firstOrNull { it.version == version }
            ?: error("Release '$version' was not found in ${changelogFile.get().asFile.path}.")

        require(release.sections.values.any { it.isNotEmpty() }) {
            "Release '$version' is empty in ${changelogFile.get().asFile.path}."
        }

        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(renderReleaseNotes(release))

        logger.lifecycle("[changelog] Wrote release notes to ${output.path}")
    }

    private fun resolveReleaseVersion(): String {
        val configuredVersion = VersionManager(
            base = versionFile.get().asFile.parentFile,
            root = versionFile.get().asFile.parentFile,
        ).get().toStringValue()
        val overrideVersion = releaseVersion.orNull?.trim()

        if (!releaseVersion.isPresent) {
            return configuredVersion
        }
        require(!overrideVersion.isNullOrBlank()) { "releaseVersion must not be blank." }
        require(overrideVersion == configuredVersion) {
            "releaseVersion '$overrideVersion' does not match version.properties '$configuredVersion'."
        }
        return overrideVersion
    }

    private fun renderReleaseNotes(release: Release): String = buildString {
        appendLine("## ${release.version}${release.date?.let { " - $it" }.orEmpty()}")
        release.sections.forEach { (sectionType, entries) ->
            if (entries.isNotEmpty()) {
                appendLine()
                appendLine("### ${sectionType.header}")
                appendLine()
                entries.forEach { appendEntry(it) }
            }
        }
    }

    private fun StringBuilder.appendEntry(entry: ChangelogEntry) {
        if (entry.text.isNotBlank()) {
            appendLine("- ${entry.text}")
        }
    }

    companion object {
        const val TASK_NAME = "writeReleaseNotes"
    }
}
