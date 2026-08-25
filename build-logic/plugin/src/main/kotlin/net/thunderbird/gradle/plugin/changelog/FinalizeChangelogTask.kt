package net.thunderbird.gradle.plugin.changelog

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
 * Finalizes the current Unreleased changelog section for a release version.
 */
@OptIn(ExperimentalTime::class)
abstract class FinalizeChangelogTask : DefaultTask() {

    @get:OutputFile
    abstract val changelogFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val releaseVersion: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val versionFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val releaseDate: Property<String>

    init {
        releaseDate.convention(
            project.provider {
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            },
        )
    }

    @TaskAction
    fun finalizeChangelog() {
        val version = resolveReleaseVersion()
        require(version.isNotBlank()) { "releaseVersion must not be blank." }
        require(!version.equals(UNRELEASED, ignoreCase = true)) { "releaseVersion must not be '$UNRELEASED'." }

        val date = LocalDate.parse(releaseDate.get().trim())
        val manager = ChangelogManager(changelogFile.get().asFile)
        val changelog = manager.get()
        val releases = changelog.releases.toMutableList()

        val unreleasedIndex = releases.indexOfFirst { it.version.equals(UNRELEASED, ignoreCase = true) }
        require(unreleasedIndex >= 0) { "No '$UNRELEASED' section found in ${changelogFile.get().asFile.path}." }
        require(
            releases.none {
                it.version == version
            },
        ) { "Release '$version' already exists in ${changelogFile.get().asFile.path}." }

        val unreleased = releases[unreleasedIndex]
        require(unreleased.sections.values.any { entries -> entries.isNotEmpty() }) {
            "'$UNRELEASED' section is empty in ${changelogFile.get().asFile.path}."
        }

        releases[unreleasedIndex] = Release(
            version = version,
            date = date,
            sections = unreleased.sections,
        )
        releases.add(
            unreleasedIndex,
            Release(
                version = UNRELEASED,
                date = null,
                sections = emptyMap(),
            ),
        )

        manager.update(changelog.copy(releases = releases))
        logger.lifecycle("[changelog] Finalized ${changelogFile.get().asFile.path} for $version ($date)")
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

    companion object {
        const val TASK_NAME = "finalizeChangelog"
        private const val UNRELEASED = "Unreleased"
    }
}
