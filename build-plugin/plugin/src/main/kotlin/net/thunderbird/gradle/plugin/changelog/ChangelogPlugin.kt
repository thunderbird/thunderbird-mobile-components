package net.thunderbird.gradle.plugin.changelog

import java.io.File
import net.thunderbird.gradle.plugin.ProjectConfig
import net.thunderbird.gradle.plugin.changelog.internal.fs.FileHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * Plugin that helps maintain a component-oriented CHANGELOG.md.
 *
 * It derives the component group from the directory that contains the nearest
 * version.properties (walking up from the current project directory). This way,
 * multiple modules that share the same nearest version.properties are grouped together.
 */
class ChangelogPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val start = project.projectDir
            val root = rootProject.projectDir
            val versionDir = FileHelper.locateNearestVersionDir(start, root)

            if (versionDir == null) {
                logger.warn(
                    "[changelog] No version.properties found between ${start.path} and ${root.path}. " +
                        "CHANGELOG.md will not be created or updated.",
                )
            } else {
                logger.debug("[changelog] Using folder at: ${versionDir.path} for component changelog")

                tasks.register<UpdateChangelogTask>(UpdateChangelogTask.TASK_NAME) {
                    group = "documentation"
                    description =
                        "Ensure component-local CHANGELOG.md (next to nearest version.properties) exists and has Unreleased sections"

                    versionFile.set(
                        project.layout.file(
                            project.provider { File(versionDir, FileHelper.VERSION_FILE) },
                        ),
                    )
                    changelogFile.set(
                        project.layout.file(
                            project.provider { File(versionDir, FileHelper.CHANGELOG_FILE) },
                        ),
                    )

                    repoRootDir.set(rootProject.layout.projectDirectory)
                    repoUrl.set(ProjectConfig.Publishing.url)
                }

                tasks.register<FinalizeChangelogTask>(FinalizeChangelogTask.TASK_NAME) {
                    group = "documentation"
                    description = "Finalize the component-local CHANGELOG.md Unreleased section for a release version"

                    changelogFile.set(
                        project.layout.file(
                            project.provider { File(versionDir, FileHelper.CHANGELOG_FILE) },
                        ),
                    )
                    versionFile.set(
                        project.layout.file(
                            project.provider { File(versionDir, FileHelper.VERSION_FILE) },
                        ),
                    )
                    releaseVersion.convention(providers.gradleProperty("releaseVersion"))
                    releaseDate.convention(providers.gradleProperty("releaseDate"))
                }

                tasks.register<WriteReleaseNotesTask>(WriteReleaseNotesTask.TASK_NAME) {
                    group = "documentation"
                    description = "Write GitHub release notes from the finalized component-local CHANGELOG.md section"

                    changelogFile.set(
                        project.layout.file(
                            project.provider { File(versionDir, FileHelper.CHANGELOG_FILE) },
                        ),
                    )
                    versionFile.set(
                        project.layout.file(
                            project.provider { File(versionDir, FileHelper.VERSION_FILE) },
                        ),
                    )
                    outputFile.convention(layout.buildDirectory.file("release/release-notes.md"))
                    providers.gradleProperty("releaseNotesFile").orNull?.let { releaseNotesFile ->
                        outputFile.set(layout.file(provider { File(releaseNotesFile) }))
                    }
                    releaseVersion.convention(providers.gradleProperty("releaseVersion"))
                }
            }
        }
    }
}
