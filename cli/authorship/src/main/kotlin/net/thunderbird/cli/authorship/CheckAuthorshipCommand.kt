/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.io.IOException
import kotlinx.serialization.SerializationException

public class CheckAuthorshipCommand : CliktCommand(
    name = "check-authorship",
) {
    override fun help(context: Context): String =
        "Verify git code authorship and generate migration report with git commit message snippet."

    private val sourcePaths: List<String> by option(
        "-p",
        "--source-path",
        help = "Subdirectory or file path(s) within the repository to inspect (can be specified multiple times).",
    ).multiple()

    private val sourceCommit: String by option(
        "-c",
        "--source-commit",
        help = "Source commit SHA or ref to inspect (if not specified, auto-detected from repository HEAD/main).",
    ).default("")

    private val sourceRepoUrl: String by option(
        "--source-repo-url",
        help = "URL of the source git repository (default: https://github.com/thunderbird/thunderbird-android.git).",
    ).default("https://github.com/thunderbird/thunderbird-android.git")

    private val safeAuthorsFile: File by option(
        "-s",
        "--safe-authors-file",
        help = "Path to safe authors JSON configuration file (defaults to config/safe-authors.json).",
    ).file(mustExist = true, canBeDir = false).default(File("config/safe-authors.json"))

    private val output: File? by option(
        "-o",
        "--output",
        help = "Output file path to save the generated Markdown report.",
    ).file()

    private val commitMsgOnly: Boolean by option(
        "--commit-msg-only",
        help = "Print only the git commit message snippet.",
    ).flag(default = false)

    private val jsonOutput: Boolean by option(
        "--json",
        help = "Output the analysis as a JSON structure.",
    ).flag(default = false)

    private val cleanupTmp: Boolean by option(
        "--cleanup-tmp",
        help = "Remove the cloned repository from build/tmp/ after execution.",
    ).flag(default = false)

    override fun run() {
        val config = resolveConfig()
        val repoManager = GitRepoManager()
        var targetRepo: File? = null
        try {
            targetRepo = try {
                repoManager.resolveRepository(
                    sourceRepoUrl = sourceRepoUrl,
                    sourceCommit = sourceCommit,
                    onProgress = { msg ->
                        if (!commitMsgOnly && !jsonOutput) {
                            echo(msg)
                        }
                    },
                )
            } catch (e: IllegalStateException) {
                abort("Error: ${e.message}")
            }

            val analyzer = GitAuthorshipAnalyzer(targetRepo)
            val resolvedCommit = analyzer.getHeadCommit()

            val (contributors, filesStats) = analyzer.analyze(
                targetPaths = sourcePaths,
                config = config,
                revision = resolvedCommit,
            )

            val allSafe = contributors.values.all { it.isSafe } && filesStats.values.all { it.isSafe }
            outputReport(contributors, filesStats, resolvedCommit, allSafe)

            if (!allSafe) {
                throw ProgramResult(1)
            }
        } finally {
            if (cleanupTmp && targetRepo != null) {
                repoManager.cleanup(targetRepo)
            }
        }
    }

    private fun resolveConfig(): SafeAuthorsConfig {
        if (!safeAuthorsFile.isFile) {
            abort("Error: Safe authors config file not found at ${safeAuthorsFile.path}")
        }

        val config = try {
            SafeAuthorsConfig.load(safeAuthorsFile)
        } catch (e: SerializationException) {
            abort("Error: Failed to parse safe authors config at ${safeAuthorsFile.absolutePath}: ${e.message}")
        } catch (e: IOException) {
            abort("Error: Failed to read safe authors config at ${safeAuthorsFile.absolutePath}: ${e.message}")
        }
        return config
    }

    private fun abort(message: String): Nothing {
        echo(message, err = true)
        throw ProgramResult(1)
    }

    private fun outputReport(
        contributors: Map<String, ContributorStats>,
        filesStats: Map<String, FileStats>,
        resolvedCommit: String,
        allSafe: Boolean,
    ) {
        val unsafeContributors = contributors.values.filter { !it.isSafe }
        val outputText = when {
            commitMsgOnly -> ReportRenderer.generateCommitSnippet(
                sourceCommit = resolvedCommit,
                sourceRepoUrl = sourceRepoUrl,
                targetPaths = sourcePaths,
                allSafe = allSafe,
                unsafeAuthors = unsafeContributors,
            )

            jsonOutput -> ReportRenderer.generateJsonReport(
                targetPaths = sourcePaths,
                sourceCommit = resolvedCommit,
                sourceRepoUrl = sourceRepoUrl,
                contributors = contributors,
                filesStats = filesStats,
                allSafe = allSafe,
            )

            else -> ReportRenderer.generateMarkdownReport(
                targetPaths = sourcePaths,
                sourceCommit = resolvedCommit,
                sourceRepoUrl = sourceRepoUrl,
                contributors = contributors,
                filesStats = filesStats,
                allSafe = allSafe,
            )
        }

        val targetOutput = output
        if (targetOutput != null) {
            targetOutput.parentFile?.mkdirs()
            targetOutput.writeText(outputText, Charsets.UTF_8)
            echo("Report saved to ${targetOutput.absolutePath}")
        } else {
            echo(outputText)
        }
    }
}
