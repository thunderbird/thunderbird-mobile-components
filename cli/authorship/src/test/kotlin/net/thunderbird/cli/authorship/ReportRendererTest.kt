/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import assertk.assertThat
import assertk.assertions.contains
import de.infix.testBalloon.framework.core.testSuite

val reportRendererTest by testSuite("ReportRenderer") {
    test("generates commit snippet with all safe authors") {
        val snippet = ReportRenderer.generateCommitSnippet(
            sourceCommit = "abcdef1234567890abcdef1234567890abcdef12",
            sourceRepoUrl = "https://github.com/thunderbird/thunderbird-android.git",
            targetPaths = listOf("core/logging"),
            allSafe = true,
            unsafeAuthors = emptyList(),
        )

        assertThat(snippet).contains("Extracted from https://github.com/thunderbird/thunderbird-android")
        assertThat(snippet).contains("Authorship matches configured safe author rules")
        assertThat(snippet).contains(
            "Source: https://github.com/thunderbird/thunderbird-android/tree/" +
                "abcdef1234567890abcdef1234567890abcdef12/core/logging",
        )
    }

    test("generates commit snippet for partial import with safe imported files") {
        val snippet = ReportRenderer.generateCommitSnippet(
            sourceCommit = "abcdef1234567890abcdef1234567890abcdef12",
            sourceRepoUrl = "https://github.com/thunderbird/thunderbird-android.git",
            targetPaths = listOf("core/logging"),
            allSafe = true,
            unsafeAuthors = emptyList(),
            isPartialImport = true,
        )

        assertThat(snippet).contains("Extracted from https://github.com/thunderbird/thunderbird-android")
        assertThat(snippet).contains(
            "Authorship matches configured safe author rules for imported files. " +
                "Files that did not pass the authorship check were excluded.",
        )
        assertThat(snippet).contains(
            "Source: https://github.com/thunderbird/thunderbird-android/tree/" +
                "abcdef1234567890abcdef1234567890abcdef12/core/logging",
        )
    }

    test("generates commit snippet with authorship not checked") {
        val snippet = ReportRenderer.generateCommitSnippet(
            sourceCommit = "abcdef1234567890abcdef1234567890abcdef12",
            sourceRepoUrl = "https://github.com/thunderbird/thunderbird-android.git",
            targetPaths = listOf("core/logging"),
            allSafe = false,
            unsafeAuthors = emptyList(),
            authorshipNotChecked = true,
        )

        assertThat(snippet).contains("Extracted from https://github.com/thunderbird/thunderbird-android")
        assertThat(snippet).contains("Authorship not checked")
        assertThat(snippet).contains(
            "Source: https://github.com/thunderbird/thunderbird-android/tree/" +
                "abcdef1234567890abcdef1234567890abcdef12/core/logging",
        )
    }

    test("generates commit snippet with unapproved contributors") {
        val unsafe = listOf(
            ContributorStats(
                name = "External Contributor",
                email = "external@example.org",
                isSafe = false,
                affiliation = "Unverified",
            ),
        )

        val snippet = ReportRenderer.generateCommitSnippet(
            sourceCommit = "1234567890abcdef1234567890abcdef12345678",
            sourceRepoUrl = "https://github.com/thunderbird/thunderbird-android.git",
            targetPaths = listOf("core/feature"),
            allSafe = false,
            unsafeAuthors = unsafe,
        )

        assertThat(snippet).contains("Authorship includes unapproved contributors: External Contributor")
    }

    test("renders full Markdown report with file statistics") {
        val contributors = mapOf(
            "Dev One <dev1@thunderbird.net>" to ContributorStats(
                name = "Dev One",
                email = "dev1@thunderbird.net",
                isSafe = true,
                affiliation = "Corporate Staff (@thunderbird.net)",
                blameLines = 10,
                commits = setOf("sha1", "sha2"),
            ),
        )
        val filesStats = mapOf(
            "src/Main.kt" to FileStats(
                path = "src/Main.kt",
                isSafe = true,
                totalLines = 10,
                authorsBlame = mapOf("Dev One <dev1@thunderbird.net>" to 10),
                authorsCommits = setOf("Dev One <dev1@thunderbird.net>"),
            ),
            "assets/logo.png" to FileStats(
                path = "assets/logo.png",
                isSafe = true,
                isBinary = true,
                totalLines = 0,
                authorsCommits = setOf("Dev One <dev1@thunderbird.net>"),
            ),
        )

        val report = ReportRenderer.generateMarkdownReport(
            contributors = contributors,
            filesStats = filesStats,
            sourceCommit = "0123456789012345678901234567890123456789",
            sourceRepoUrl = "https://github.com/thunderbird/thunderbird-android.git",
            targetPaths = listOf("src"),
            allSafe = true,
        )

        assertThat(report).contains("# Code Authorship & Migration Verification Report")
        assertThat(report).contains("Dev One")
        assertThat(report).contains("src/Main.kt")
        assertThat(report).contains("assets/logo.png")
        assertThat(report).contains("Binary")
        assertThat(report).contains("All Safe")
    }

    test("renders unsafe contributor annotations for unsafe files in Markdown report") {
        val contributors = mapOf(
            "Safe Dev <safe@thunderbird.net>" to ContributorStats(
                name = "Safe Dev",
                email = "safe@thunderbird.net",
                isSafe = true,
                affiliation = "Staff",
                blameLines = 10,
            ),
            "Unsafe Dev <unsafe@other.org>" to ContributorStats(
                name = "Unsafe Dev",
                email = "unsafe@other.org",
                isSafe = false,
                affiliation = "External",
                commitCount = 1,
            ),
        )
        val filesStats = mapOf(
            "src/File.kt" to FileStats(
                path = "src/File.kt",
                isSafe = false,
                totalLines = 10,
                authorsBlame = mapOf("Safe Dev <safe@thunderbird.net>" to 10),
                authorsCommits = setOf("Safe Dev <safe@thunderbird.net>", "Unsafe Dev <unsafe@other.org>"),
                unsafeContributors = setOf("Unsafe Dev <unsafe@other.org>"),
            ),
        )

        val report = ReportRenderer.generateMarkdownReport(
            contributors = contributors,
            filesStats = filesStats,
            sourceCommit = "0123456789012345678901234567890123456789",
            sourceRepoUrl = "https://github.com/thunderbird/thunderbird-android.git",
            targetPaths = listOf("src"),
            allSafe = false,
        )

        assertThat(report).contains("⚠️ Unapproved commit: Unsafe Dev")
    }

    test("renders JSON output with complete structure") {
        val contributors = mapOf(
            "Dev One <dev1@thunderbird.net>" to ContributorStats(
                name = "Dev One",
                email = "dev1@thunderbird.net",
                isSafe = true,
                affiliation = "Staff",
                blameLines = 5,
            ),
        )
        val filesStats = mapOf(
            "src/Main.kt" to FileStats(
                path = "src/Main.kt",
                isSafe = true,
                totalLines = 5,
            ),
        )

        val json = ReportRenderer.generateJsonReport(
            contributors = contributors,
            filesStats = filesStats,
            sourceCommit = "0123456789012345678901234567890123456789",
            sourceRepoUrl = "https://github.com/thunderbird/thunderbird-android.git",
            targetPaths = listOf("src"),
            allSafe = true,
        )

        assertThat(json).contains("\"allSafe\": true")
        assertThat(json).contains("\"sourceCommit\": \"0123456789012345678901234567890123456789\"")
    }
}
