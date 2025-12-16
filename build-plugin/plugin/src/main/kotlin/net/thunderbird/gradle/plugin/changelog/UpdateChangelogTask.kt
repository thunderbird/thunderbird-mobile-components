package net.thunderbird.gradle.plugin.changelog

import net.thunderbird.gradle.plugin.changelog.internal.Changelog
import net.thunderbird.gradle.plugin.changelog.internal.ChangelogEntry
import net.thunderbird.gradle.plugin.changelog.internal.ChangelogManager
import net.thunderbird.gradle.plugin.changelog.internal.Release
import net.thunderbird.gradle.plugin.changelog.internal.SectionType
import net.thunderbird.gradle.plugin.changelog.internal.fs.FileHelper
import net.thunderbird.gradle.plugin.changelog.internal.git.GitClient
import net.thunderbird.gradle.plugin.changelog.internal.git.GitConventionalCommitParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Ensures the component-local CHANGELOG.md (written next to the nearest
 * version.properties) exists and contains an `## [Unreleased]` section with
 * conventional sub-sections.
 */
abstract class UpdateChangelogTask : DefaultTask() {

    @get:InputFile
    @get:Optional
    abstract val versionFile: RegularFileProperty

    @get:OutputFile
    abstract val changelogFile: RegularFileProperty

    /**
     * Repository root directory used for git commands.
     *
     * Git history is external task state; do not make Gradle snapshot the whole repository.
     */
    @get:Internal
    abstract val repoRootDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val repoUrl: Property<String>

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun update() {
        val versionPropsFile = versionFile.orNull?.asFile
        val changelogFile = changelogFile.get().asFile
        val root = repoRootDir.get().asFile

        val changelogManager = ChangelogManager(changelogFile)
        val changelog = changelogManager.get()

        // Component-relative path (from repo root) to limit git scan
        val componentDir = versionPropsFile?.parentFile ?: changelogFile.parentFile
        val relativePathRaw = root.toPath().relativize(componentDir.toPath()).toString()
        val relativePath = relativePathRaw.takeIf { it.isNotBlank() }

        val git = GitClient { msg -> logger.warn(msg) }
        val latestRelease = changelogManager.getLatestRelease(changelog)
        val startRef = if (latestRelease != null) {
            val tagCandidates = releaseTagCandidates(latestRelease.version, componentDir.name)
            git.firstExistingRef(root, tagCandidates)
                ?: error(
                    "No git tag found for latest changelog release '${latestRelease.version}'. " +
                        "Tried: ${tagCandidates.joinToString()}. " +
                        "Create a release tag before updating the changelog.",
                )
        } else {
            null
        }

        val subjects = git.logComponentSubjects(root, relativePath, startRef)

        val updated = updateChangelog(changelog, subjects)

        changelogManager.update(updated)
        logger.lifecycle("[changelog] Updated ${changelogFile.path} with ${subjects.size} commits")
    }

    private fun updateChangelog(
        changelog: Changelog,
        subjects: List<String>,
    ): Changelog {
        // Find or create Unreleased release
        val releases = changelog.releases.toMutableList()
        val idx = releases.indexOfFirst { it.version.equals("Unreleased", ignoreCase = true) }
        val unreleased = if (idx >= 0) {
            releases[idx]
        } else {
            Release(
                version = "Unreleased",
                date = null,
                sections = emptyMap(),
            )
        }

        // Start with current sections
        val sectionMap = linkedMapOf<SectionType, MutableList<ChangelogEntry>>()
        unreleased.sections.forEach { (k, v) -> sectionMap.getOrPut(k) { mutableListOf() }.addAll(v) }

        // Collect existing bullets to avoid duplicates
        val existingBullets = sectionMap.values.flatten().map { it.text }.toSet()

        // Parse and add subjects
        val ccParser = GitConventionalCommitParser()
        val url = repoUrl.orNull
        subjects.forEach { subj ->
            val cc = ccParser.parse(subj, url) ?: return@forEach
            if (existingBullets.contains(cc.description)) return@forEach
            sectionMap.getOrPut(cc.type) { mutableListOf() }.add(ChangelogEntry(text = cc.description))
        }

        // Keep only non-empty sections
        val newSections = sectionMap
            .filterValues { it.isNotEmpty() }
            .mapValues { it.value.toList() }

        val newUnreleased = unreleased.copy(sections = newSections)
        if (idx >= 0) releases[idx] = newUnreleased else releases.add(0, newUnreleased)

        return changelog.copy(releases = releases)
    }

    private fun releaseTagCandidates(version: String, componentPrefix: String): List<String> {
        val normalizedVersion = version.removePrefix("v")

        return listOf("$componentPrefix-$normalizedVersion")
    }

    companion object {
        const val TASK_NAME = "updateChangelog"
    }
}
