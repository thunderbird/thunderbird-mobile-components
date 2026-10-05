/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.importcode

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.io.IOException
import kotlinx.serialization.SerializationException
import net.thunderbird.cli.authorship.ContributorStats
import net.thunderbird.cli.authorship.FileStats
import net.thunderbird.cli.authorship.GitAuthorshipAnalyzer
import net.thunderbird.cli.authorship.GitRepoManager
import net.thunderbird.cli.authorship.ReportRenderer
import net.thunderbird.cli.authorship.SafeAuthorsConfig

public data class LeftOutFile(
    val path: String,
    val reason: String,
)

public data class ImportResult(
    val importedFiles: List<File>,
    val leftOutFiles: List<LeftOutFile>,
)

private data class ImportContext(
    val targetRepo: File,
    val destination: File,
    val filesStats: Map<String, FileStats>,
    val contributors: Map<String, ContributorStats>,
    val includeBuildGradle: Boolean,
    val skipAuthorshipCheck: Boolean,
    val isDryRun: Boolean,
)

public class ImportCodeCommand : CliktCommand(
    name = "import-code",
) {
    override fun help(context: Context): String =
        "Import code from the source repository to this repository and output git transfer commit message."

    private val sourcePath: String by option(
        "-p",
        "--source-path",
        help = "Subdirectory or file path within the source repository to import (e.g. core/logging).",
    ).required()

    private val targetPath: String by option(
        "-t",
        "-d",
        "--target-path",
        help = "Destination directory path within this repository (e.g. components/core/logging).",
    ).required()

    private val sourceCommit: String by option(
        "-c",
        "--source-commit",
        help = "Source commit SHA or ref to inspect and import from (if omitted, auto-detected from latest commit).",
    ).default("")

    private val sourceRepoUrl: String by option(
        "--source-repo-url",
        help = "URL of the source git repository (default: https://github.com/thunderbird/thunderbird-android.git).",
    ).default("https://github.com/thunderbird/thunderbird-android.git")

    private val includeBuildGradle: Boolean by option(
        "--include-build-gradle",
        help = "Include build.gradle.kts files when importing (excluded by default as project configuration).",
    ).flag(default = false)

    private val skipAuthorshipCheck: Boolean by option(
        "--skip-authorship-check",
        help = "Skip safe authors verification before importing.",
    ).flag(default = false)

    private val safeAuthorsFile: File by option(
        "-s",
        "--safe-authors-file",
        help = "Path to approval reference JSON (default: config/safe-authors-reference.json).",
    ).file(mustExist = true, canBeDir = false).default(File("config/safe-authors-reference.json"))

    private val cleanupTmp: Boolean by option(
        "--cleanup-tmp",
        help = "Remove the cloned repository from build/tmp/ after importing.",
    ).flag(default = true)

    private val dryRun: Boolean by option(
        "--dry-run",
        help = "Simulate the import operation without writing files.",
    ).flag(default = false)

    override fun run() {
        val repoManager = GitRepoManager()
        var targetRepo: File? = null
        try {
            targetRepo = resolveTargetRepo(repoManager)
            val analyzer = GitAuthorshipAnalyzer(targetRepo)
            val resolvedCommit = analyzer.getHeadCommit()

            val sourceFile = File(targetRepo, sourcePath)
            if (!sourceFile.exists()) {
                abort("Error: Source path '$sourcePath' does not exist in source repository at ${targetRepo.path}")
            }

            val (contributors, filesStats) = if (!skipAuthorshipCheck) {
                val config = resolveConfig()
                analyzer.analyze(
                    targetPaths = listOf(sourcePath),
                    config = config,
                    revision = resolvedCommit,
                    includeBuildGradle = includeBuildGradle,
                )
            } else {
                emptyMap<String, ContributorStats>() to emptyMap<String, FileStats>()
            }

            val destDir = File(targetPath)
            val context = ImportContext(
                targetRepo = targetRepo,
                destination = destDir,
                filesStats = filesStats,
                contributors = contributors,
                includeBuildGradle = includeBuildGradle,
                skipAuthorshipCheck = skipAuthorshipCheck,
                isDryRun = dryRun,
            )
            val result = importFiles(
                sourceFile = sourceFile,
                context = context,
            )

            printImportSummary(
                result = result,
                resolvedCommit = resolvedCommit,
            )
        } finally {
            if (cleanupTmp && targetRepo != null) {
                repoManager.cleanup(targetRepo)
            }
        }
    }

    private fun resolveTargetRepo(repoManager: GitRepoManager): File {
        return try {
            repoManager.resolveRepository(
                sourceRepoUrl = sourceRepoUrl,
                sourceCommit = sourceCommit,
                onProgress = { msg -> echo(msg) },
            )
        } catch (e: IllegalStateException) {
            abort("Error: ${e.message}")
        }
    }

    private fun printImportSummary(
        result: ImportResult,
        resolvedCommit: String,
    ) {
        if (result.importedFiles.isNotEmpty()) {
            val count = result.importedFiles.size
            if (skipAuthorshipCheck) {
                echo("Successfully imported $count file(s) from '$sourcePath' to '$targetPath'.")
            } else {
                echo("Successfully imported $count safe file(s) from '$sourcePath' to '$targetPath'.")
            }
        } else {
            echo("No files imported from '$sourcePath' to '$targetPath'.")
        }

        if (dryRun) {
            echo("(dry-run mode: no files were written to disk)")
        }

        if (result.leftOutFiles.isNotEmpty()) {
            echo("\nLeft out files (${result.leftOutFiles.size}):")
            for (leftOut in result.leftOutFiles) {
                echo("- ${leftOut.path}: ${leftOut.reason}")
            }
        }

        val hasExcludedUnsafeFiles = result.leftOutFiles.any {
            !it.reason.startsWith("Excluded build configuration")
        }

        val allImportedSafe = !skipAuthorshipCheck && result.importedFiles.isNotEmpty()

        val commitSnippet = ReportRenderer.generateCommitSnippet(
            sourceCommit = resolvedCommit,
            sourceRepoUrl = sourceRepoUrl,
            targetPaths = listOf(sourcePath),
            allSafe = allImportedSafe,
            unsafeAuthors = emptyList(),
            authorshipNotChecked = skipAuthorshipCheck,
            isPartialImport = hasExcludedUnsafeFiles,
        )

        echo("\nGit Transfer Commit Message:\n")
        echo(commitSnippet)
    }

    private fun resolveConfig(): SafeAuthorsConfig {
        if (!safeAuthorsFile.isFile) {
            abort("Error: Safe authors config file not found at ${safeAuthorsFile.path}")
        }

        return try {
            SafeAuthorsConfig.load(safeAuthorsFile)
        } catch (e: SerializationException) {
            abort("Error: Failed to parse safe authors config at ${safeAuthorsFile.absolutePath}: ${e.message}")
        } catch (e: IOException) {
            abort("Error: Failed to read safe authors config at ${safeAuthorsFile.absolutePath}: ${e.message}")
        }
    }

    private fun importFiles(
        sourceFile: File,
        context: ImportContext,
    ): ImportResult {
        val imported = mutableListOf<File>()
        val leftOut = mutableListOf<LeftOutFile>()

        if (sourceFile.isFile) {
            importSingleFile(
                sourceFile = sourceFile,
                context = context,
                imported = imported,
                leftOut = leftOut,
            )
            return ImportResult(importedFiles = imported, leftOutFiles = leftOut)
        }

        sourceFile.walkTopDown()
            .filter { it.isFile }
            .filterNot { isGitInternalFile(it, sourceFile) }
            .forEach { file ->
                processDirectoryFile(
                    file = file,
                    sourceFile = sourceFile,
                    context = context,
                    imported = imported,
                    leftOut = leftOut,
                )
            }

        return ImportResult(importedFiles = imported, leftOutFiles = leftOut)
    }

    private fun abort(message: String): Nothing {
        echo(message, err = true)
        throw ProgramResult(1)
    }
}

private fun isGitInternalFile(file: File, sourceFile: File): Boolean {
    val rel = file.relativeTo(sourceFile).path.replace(oldChar = '\\', newChar = '/')
    return rel.startsWith(".git") || rel.contains("/.git/")
}

private fun importSingleFile(
    sourceFile: File,
    context: ImportContext,
    imported: MutableList<File>,
    leftOut: MutableList<LeftOutFile>,
) {
    val repoRel = sourceFile.relativeTo(context.targetRepo).path.replace(oldChar = '\\', newChar = '/')
    val destination = context.destination
    val target = if (destination.isDirectory || destination.path.endsWith("/")) {
        File(destination, sourceFile.name)
    } else {
        destination
    }
    importFile(
        file = sourceFile,
        repoRel = repoRel,
        target = target,
        context = context,
        imported = imported,
        leftOut = leftOut,
    )
}

private fun processDirectoryFile(
    file: File,
    sourceFile: File,
    context: ImportContext,
    imported: MutableList<File>,
    leftOut: MutableList<LeftOutFile>,
) {
    val relFromSource = file.relativeTo(sourceFile).path.replace(oldChar = '\\', newChar = '/')
    val repoRel = file.relativeTo(context.targetRepo).path.replace(oldChar = '\\', newChar = '/')
    val target = File(context.destination, relFromSource)
    importFile(
        file = file,
        repoRel = repoRel,
        target = target,
        context = context,
        imported = imported,
        leftOut = leftOut,
    )
}

private fun importFile(
    file: File,
    repoRel: String,
    target: File,
    context: ImportContext,
    imported: MutableList<File>,
    leftOut: MutableList<LeftOutFile>,
) {
    if (isBuildGradleFile(repoRel) && !context.includeBuildGradle) {
        leftOut.add(LeftOutFile(path = repoRel, reason = "Excluded build configuration (${file.name})"))
        return
    }

    val fileStat = context.filesStats[repoRel]
    if (!context.skipAuthorshipCheck && fileStat?.isSafe != true) {
        leftOut.add(LeftOutFile(path = repoRel, reason = extractUnsafeReason(fileStat, context.contributors)))
        return
    }

    if (!context.isDryRun) {
        target.parentFile?.mkdirs()
        file.copyTo(target, overwrite = true)
    }
    imported.add(target)
}

private fun isBuildGradleFile(repoRel: String): Boolean {
    return repoRel == "build.gradle.kts" || repoRel.endsWith("/build.gradle.kts")
}

private fun extractUnsafeReason(
    fileStat: FileStats?,
    contributors: Map<String, ContributorStats>,
): String {
    if (fileStat == null) return "File not in audit scope"
    if (fileStat.unsafeContributors.isNotEmpty()) {
        return "Unapproved contributor(s): ${fileStat.unsafeContributors.joinToString(", ")}"
    }
    val unsafeAuthors = mutableSetOf<String>()
    for (authorKey in fileStat.authorsBlame.keys) {
        if (contributors[authorKey]?.isSafe == false || authorKey.startsWith("Unknown") ||
            authorKey.startsWith("Missing")
        ) {
            unsafeAuthors.add(authorKey)
        }
    }
    for (authorKey in fileStat.authorsCommits) {
        if (contributors[authorKey]?.isSafe == false || authorKey.startsWith("Unknown") ||
            authorKey.startsWith("Missing")
        ) {
            unsafeAuthors.add(authorKey)
        }
    }
    return if (unsafeAuthors.isNotEmpty()) {
        "Unapproved contributor(s): ${unsafeAuthors.joinToString(", ")}"
    } else {
        "Unverified git attribution"
    }
}
