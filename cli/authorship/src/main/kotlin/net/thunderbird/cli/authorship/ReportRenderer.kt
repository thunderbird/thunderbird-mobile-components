/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import java.util.Locale
import kotlinx.serialization.json.Json

public object ReportRenderer {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    public fun generateCommitSnippet(
        sourceCommit: String,
        sourceRepoUrl: String,
        targetPaths: List<String> = emptyList(),
        allSafe: Boolean,
        unsafeAuthors: List<ContributorStats>,
        authorshipNotChecked: Boolean = false,
        isPartialImport: Boolean = false,
    ): String {
        val commitSha = sourceCommit.ifBlank { "<commit-sha>" }
        val repoBaseUrl = sourceRepoUrl.trim().trimEnd('/').removeSuffix(".git")

        val targetPath = targetPaths.firstOrNull()?.trimStart('/')?.trimEnd('/').orEmpty()
        val sourceRefUrl = if (targetPath.isNotBlank()) {
            "$repoBaseUrl/tree/$commitSha/$targetPath"
        } else {
            "$repoBaseUrl/tree/$commitSha"
        }

        val authorshipLine = when {
            authorshipNotChecked -> "Authorship not checked"

            allSafe && isPartialImport ->
                "Authorship matches configured safe author rules for imported files. " +
                    "Files that did not pass the authorship check were excluded."

            allSafe -> "Authorship matches configured safe author rules"

            else -> {
                val names = unsafeAuthors.joinToString(", ") { it.name }
                if (names.isNotBlank()) {
                    "Authorship includes unapproved contributors: $names"
                } else {
                    "Authorship includes unapproved contributors"
                }
            }
        }

        return "Extracted from $repoBaseUrl.\n" +
            "$authorshipLine\n" +
            "Source: $sourceRefUrl"
    }

    public fun generateMarkdownReport(
        targetPaths: List<String>,
        sourceCommit: String,
        sourceRepoUrl: String,
        contributors: Map<String, ContributorStats>,
        filesStats: Map<String, FileStats>,
        allSafe: Boolean,
    ): String {
        val unsafeContributors = contributors.values.filter { !it.isSafe }

        val sb = StringBuilder()
        sb.append("# Code Authorship & Migration Verification Report\n\n")
        val targetPathsStr = if (targetPaths.isNotEmpty()) targetPaths.joinToString(", ") { "`$it`" } else "`.`"
        sb.append("- **Target Paths**: $targetPathsStr\n")
        sb.append("- **Source Repository**: [$sourceRepoUrl]($sourceRepoUrl)\n")
        sb.append("- **Source Commit**: `${sourceCommit.ifBlank { "<commit-sha>" }}`\n")
        val statusText = if (allSafe) {
            "✅ **Configured Approvals Satisfied (All Safe)**"
        } else {
            "⚠️ **Action Required (Unapproved Contributors Detected)**"
        }
        sb.append("- **Verification Status**: $statusText\n\n")

        sb.append("## Executive Summary\n\n")
        if (allSafe) {
            sb.append(
                "All detected contributors across the audited files match the configured safe author rules.\n\n" +
                    "> **Note**: This report reflects configured safe author rules and does not constitute " +
                    "formal legal verification of license migration eligibility. Files deleted prior to the " +
                    "inspected commit and excluded build configurations (`build.gradle.kts`) are outside " +
                    "the audit scope.\n\n",
            )
        } else {
            sb.append(
                "One or more contributors were not found in the approved safe authors list. " +
                    "Manual review or contributor clearance is required.\n\n",
            )
        }

        sb.append(formatContributorDirectory(contributors))
        sb.append(formatFileBreakdown(filesStats))

        sb.append("\n## Git Transfer Commit Message\n\n")
        sb.append("Copy and paste the following snippet into the git commit message for this transfer:\n\n")
        sb.append("```\n")
        sb.append(
            generateCommitSnippet(
                sourceCommit = sourceCommit,
                sourceRepoUrl = sourceRepoUrl,
                targetPaths = targetPaths,
                allSafe = allSafe,
                unsafeAuthors = unsafeContributors,
            ),
        )
        sb.append("\n```\n")

        return sb.toString()
    }

    private fun formatContributorDirectory(contributors: Map<String, ContributorStats>): String {
        val totalBlameLines = contributors.values.sumOf { it.blameLines }
        val sb = StringBuilder()
        sb.append("## Contributor Directory\n\n")
        sb.append("| Contributor | Email | Safe Status | Affiliation | Commits | Blame Lines | Blame Share |\n")
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n")

        val sortedContributors = contributors.values.sortedWith(
            compareByDescending<ContributorStats> { it.blameLines }.thenByDescending { it.commitCount },
        )

        for (c in sortedContributors) {
            val statusIcon = if (c.isSafe) "✅ Safe" else "❌ Unsafe"
            val share = if (totalBlameLines > 0) {
                String.format(Locale.ROOT, "%.1f%%", (c.blameLines.toDouble() / totalBlameLines * 100))
            } else {
                "N/A"
            }
            sb.append(
                "| **${c.name}** | `${c.email}` | $statusIcon | ${c.affiliation} | " +
                    "${c.commitCount} | ${c.blameLines} | $share |\n",
            )
        }
        return sb.toString()
    }

    private fun formatFileBreakdown(filesStats: Map<String, FileStats>): String {
        val sb = StringBuilder()
        sb.append("\n## File-by-File Attribution Breakdown\n\n")
        sb.append("| Target File | Status | Total Lines | Top Authors (Blame Lines) |\n")
        sb.append("| :--- | :--- | :--- | :--- |\n")

        for ((fPath, fStat) in filesStats.toSortedMap()) {
            val statusIcon = if (fStat.isSafe) "✅ Safe" else "⚠️ Unsafe"
            val lineCountStr = if (fStat.isBinary) "Binary" else fStat.totalLines.toString()
            val authorsStr = formatFileAuthors(fStat)
            sb.append("| `$fPath` | $statusIcon | $lineCountStr | $authorsStr |\n")
        }
        return sb.toString()
    }

    private fun formatFileAuthors(fStat: FileStats): String {
        val primaryAttribution = when {
            fStat.authorsBlame.isNotEmpty() -> {
                fStat.authorsBlame.entries
                    .sortedByDescending { it.value }
                    .joinToString(", ") { (author, count) ->
                        val cleanAuthor = author.substringBefore("<").trim()
                        "$cleanAuthor (${count}L)"
                    }
            }

            fStat.authorsCommits.isNotEmpty() -> {
                fStat.authorsCommits
                    .map { it.substringBefore("<").trim() }
                    .distinct()
                    .joinToString(", ") { "$it (commit)" }
            }

            else -> "*No git history*"
        }

        if (fStat.isSafe) {
            return primaryAttribution
        }

        val unsafeList = fStat.unsafeContributors.map { it.substringBefore("<").trim() }.distinct()
        return if (unsafeList.isNotEmpty()) {
            val unapprovedStr = unsafeList.joinToString(", ")
            if (fStat.authorsBlame.isNotEmpty()) {
                val blameAuthorNames = fStat.authorsBlame.keys.map { it.substringBefore("<").trim() }.toSet()
                val extraCommitUnsafe = unsafeList.filterNot { it in blameAuthorNames }
                if (extraCommitUnsafe.isNotEmpty()) {
                    "$primaryAttribution — ⚠️ Unapproved commit: ${extraCommitUnsafe.joinToString(", ")}"
                } else {
                    "$primaryAttribution — ⚠️ Unapproved: $unapprovedStr"
                }
            } else {
                "$primaryAttribution — ⚠️ Unapproved: $unapprovedStr"
            }
        } else {
            "$primaryAttribution — ⚠️ Unapproved"
        }
    }

    public fun generateJsonReport(
        targetPaths: List<String>,
        sourceCommit: String,
        sourceRepoUrl: String,
        contributors: Map<String, ContributorStats>,
        filesStats: Map<String, FileStats>,
        allSafe: Boolean,
    ): String {
        val unsafeContributors = contributors.values.filter { !it.isSafe }
        val commitMessage = generateCommitSnippet(
            sourceCommit = sourceCommit,
            sourceRepoUrl = sourceRepoUrl,
            targetPaths = targetPaths,
            allSafe = allSafe,
            unsafeAuthors = unsafeContributors,
        )
        val report = AuthorshipReport(
            allSafe = allSafe,
            sourceRepoUrl = sourceRepoUrl,
            sourceCommit = sourceCommit,
            targetPaths = targetPaths,
            contributors = contributors.values.toList(),
            filesStats = filesStats,
            commitMessage = commitMessage,
        )
        return json.encodeToString(AuthorshipReport.serializer(), report)
    }
}
