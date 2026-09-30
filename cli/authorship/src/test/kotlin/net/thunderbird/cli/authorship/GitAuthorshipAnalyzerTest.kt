/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import de.infix.testBalloon.framework.core.testSuite
import java.io.File
import kotlin.test.assertFailsWith
import net.thunderbird.components.core.testing.temporaryDirectoryFixture

val gitAuthorshipAnalyzerTest by testSuite("GitAuthorshipAnalyzer") {
    test("incomplete blame or missing author attribution is marked unsafe") {
        val authorsBlame = mutableMapOf<String, Int>()
        val uncreditedPorcelainBlame = """
            1111111111111111111111111111111111111111 1 1 1
            author 
            author-mail <>
            filename Test.kt
            	val incompleteLine = 1
        """.trimIndent()

        val (linesCount, isSafe) = parseBlameOutput(
            stdout = uncreditedPorcelainBlame,
            authorsBlame = authorsBlame,
            onContributorBlame = { name, mail ->
                ContributorStats(name = name, email = mail, isSafe = false, affiliation = "External")
            },
        )

        assertThat(linesCount).isEqualTo(1)
        assertThat(isSafe).isFalse()
        assertThat(authorsBlame.containsKey("Missing Attribution <unattributed@git.internal>")).isTrue()
    }

    test("two-header blame with missing author on second header fails closed and does not inherit previous author") {
        val authorsBlame = mutableMapOf<String, Int>()
        val twoHeaderBlame = """
            1111111111111111111111111111111111111111 1 1 1
            author Safe Dev
            author-mail <safe@thunderbird.net>
            filename Test.kt
            	val firstLine = 1
            2222222222222222222222222222222222222222 2 2 1
            filename Test.kt
            	val secondLine = 2
        """.trimIndent()

        val (linesCount, isSafe) = parseBlameOutput(
            stdout = twoHeaderBlame,
            authorsBlame = authorsBlame,
            onContributorBlame = { name, mail ->
                val safe = mail.endsWith("@thunderbird.net")
                val affiliation = if (safe) "Corporate Staff (@thunderbird.net)" else "Unverified"
                ContributorStats(name = name, email = mail, isSafe = safe, affiliation = affiliation)
            },
        )

        assertThat(linesCount).isEqualTo(2)
        assertThat(isSafe).isFalse()
        assertThat(authorsBlame["Safe Dev <safe@thunderbird.net>"]).isEqualTo(1)
        assertThat(authorsBlame["Missing Attribution <unattributed@git.internal>"]).isEqualTo(1)
    }

    test("empty or unparseable git log history is marked unsafe with unverified contributor") {
        val authorsCommits = mutableSetOf<String>()
        val isSafe = parseFileLogOutput(
            stdout = "",
            exitCode = 0,
            authorsCommits = authorsCommits,
            onContributorCommit = { hash, name, mail ->
                ContributorStats(name = name, email = mail, isSafe = false, affiliation = "Unverified")
            },
        )

        assertThat(isSafe).isFalse()
        assertThat(authorsCommits.contains("Missing Attribution <unattributed@git.internal>")).isTrue()
    }

    test("rename-only R100 commit is ignored in favor of code authors") {
        val authorsCommits = mutableSetOf<String>()
        val logOutput = """
            COMMIT|sha1|Build Tooling Dev|tooling@external.org
            R100	path/to/OriginalFile.kt	path/to/RenamedFile.kt
            COMMIT|sha2|Safe Dev|safe@thunderbird.net
            M	path/to/OriginalFile.kt
        """.trimIndent()

        val isSafe = parseFileLogOutput(
            stdout = logOutput,
            exitCode = 0,
            authorsCommits = authorsCommits,
            onContributorCommit = { hash, name, mail ->
                val safe = mail.endsWith("@thunderbird.net")
                ContributorStats(
                    name = name,
                    email = mail,
                    isSafe = safe,
                    affiliation = if (safe) "Staff" else "External",
                )
            },
        )

        assertThat(isSafe).isTrue()
        assertThat(authorsCommits.contains("Safe Dev <safe@thunderbird.net>")).isTrue()
        assertThat(authorsCommits.contains("Build Tooling Dev <tooling@external.org>")).isFalse()
    }

    test("copy commit C100 is retained as content author even when later edited") {
        val authorsCommits = mutableSetOf<String>()
        val logOutput = """
            COMMIT|sha1|Editor Dev|safe@thunderbird.net
            M	path/to/CopiedFile.kt
            COMMIT|sha2|Copier Dev|copier@external.org
            C100	path/to/OriginalFile.kt	path/to/CopiedFile.kt
        """.trimIndent()

        val isSafe = parseFileLogOutput(
            stdout = logOutput,
            exitCode = 0,
            authorsCommits = authorsCommits,
            onContributorCommit = { hash, name, mail ->
                val safe = mail.endsWith("@thunderbird.net")
                ContributorStats(
                    name = name,
                    email = mail,
                    isSafe = safe,
                    affiliation = if (safe) "Staff" else "External",
                )
            },
        )

        assertThat(isSafe).isFalse()
        assertThat(authorsCommits.contains("Editor Dev <safe@thunderbird.net>")).isTrue()
        assertThat(authorsCommits.contains("Copier Dev <copier@external.org>")).isTrue()
    }

    test("rename with edits R090 is retained as content author") {
        val authorsCommits = mutableSetOf<String>()
        val logOutput = """
            COMMIT|sha1|Renamer With Edits|unsafe@external.org
            R090	path/to/OriginalFile.kt	path/to/RenamedFile.kt
            COMMIT|sha2|Safe Dev|safe@thunderbird.net
            A	path/to/OriginalFile.kt
        """.trimIndent()

        val isSafe = parseFileLogOutput(
            stdout = logOutput,
            exitCode = 0,
            authorsCommits = authorsCommits,
            onContributorCommit = { hash, name, mail ->
                val safe = mail.endsWith("@thunderbird.net")
                ContributorStats(
                    name = name,
                    email = mail,
                    isSafe = safe,
                    affiliation = if (safe) "Staff" else "External",
                )
            },
        )

        assertThat(isSafe).isFalse()
        assertThat(authorsCommits.contains("Safe Dev <safe@thunderbird.net>")).isTrue()
        assertThat(authorsCommits.contains("Renamer With Edits <unsafe@external.org>")).isTrue()
    }

    test("commit with ambiguous or missing status is conservatively retained") {
        val authorsCommits = mutableSetOf<String>()
        val logOutput = """
            COMMIT|sha1|Ambiguous Dev|ambiguous@external.org
            COMMIT|sha2|Safe Dev|safe@thunderbird.net
            M	path/to/OriginalFile.kt
        """.trimIndent()

        val isSafe = parseFileLogOutput(
            stdout = logOutput,
            exitCode = 0,
            authorsCommits = authorsCommits,
            onContributorCommit = { hash, name, mail ->
                val safe = mail.endsWith("@thunderbird.net")
                ContributorStats(
                    name = name,
                    email = mail,
                    isSafe = safe,
                    affiliation = if (safe) "Staff" else "External",
                )
            },
        )

        assertThat(isSafe).isFalse()
        assertThat(authorsCommits.contains("Ambiguous Dev <ambiguous@external.org>")).isTrue()
    }

    temporaryDirectoryFixture().asParameterForEach {
        test("invalid path or empty audit scope fails closed with exception") { tmpPath ->
            val repoDir = File(tmpPath.toString(), "repo")
            initGitRepo(repoDir)
            val dummyFile = File(repoDir, "init.txt")
            dummyFile.writeText("init")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "init")

            val analyzer = GitAuthorshipAnalyzer(repoDir)
            val config = SafeAuthorsConfig(safeDomains = listOf("@thunderbird.net"))

            assertFailsWith<IllegalStateException> {
                analyzer.analyze(listOf("nonexistent/path"), config)
            }
        }

        test("git failure fails closed reporting command and error details") { tmpPath ->
            val repoDir = File(tmpPath.toString(), "repo")
            initGitRepo(repoDir)
            val dummyFile = File(repoDir, "init.txt")
            dummyFile.writeText("init")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "init")

            val analyzer = GitAuthorshipAnalyzer(repoDir)
            val exception = assertFailsWith<IllegalStateException> {
                analyzer.runRequiredGitCommand(listOf("invalid-git-subcommand"))
            }
            assertThat(exception.message.orEmpty()).contains("Git command failed")
            assertThat(exception.message.orEmpty()).contains("invalid-git-subcommand")
        }

        test("configuration-only commits on build gradle are excluded from authorship report") { tmpPath ->
            val repoDir = File(tmpPath.toString(), "repo")
            initGitRepo(repoDir)

            val codeFile = File(repoDir, "feature/Source.kt")
            codeFile.parentFile.mkdirs()
            codeFile.writeText("package feature\nclass Source")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "feat: initial", "--author=Safe Dev <safe@thunderbird.net>")

            val gradleFile = File(repoDir, "feature/build.gradle.kts")
            gradleFile.writeText("plugins {}")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "chore: deps", "--author=External Dev <ext@other.com>")

            val analyzer = GitAuthorshipAnalyzer(repoDir)
            val config = SafeAuthorsConfig(safeDomains = listOf("@thunderbird.net"))
            val (contributors, filesStats) = analyzer.analyze(listOf("feature"), config)

            assertThat(contributors.values.all { it.isSafe }).isTrue()
            assertThat(filesStats.containsKey("feature/build.gradle.kts")).isFalse()
            assertThat(filesStats.containsKey("feature/Source.kt")).isTrue()
        }

        test("deleted-file history does not affect authorship of remaining files") { tmpPath ->
            val repoDir = File(tmpPath.toString(), "repo")
            initGitRepo(repoDir)

            val deletedFile = File(repoDir, "feature/OldDeleted.kt")
            deletedFile.parentFile.mkdirs()
            deletedFile.writeText("package feature.old\n// Historical deleted implementation\nclass OldDeleted")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "feat: old file", "--author=External Dev <ext@other.com>")

            val activeFile = File(repoDir, "feature/Active.kt")
            activeFile.writeText("package feature.active\n// Current active implementation\nclass Active")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "feat: active file", "--author=Safe Dev <safe@thunderbird.net>")

            runGit(repoDir, "rm", "feature/OldDeleted.kt")
            runGit(repoDir, "commit", "-m", "chore: delete old file", "--author=Safe Dev <safe@thunderbird.net>")

            val analyzer = GitAuthorshipAnalyzer(repoDir)
            val config = SafeAuthorsConfig(safeDomains = listOf("@thunderbird.net"))
            val (contributors, filesStats) = analyzer.analyze(listOf("feature"), config)

            assertThat(contributors.values.all { it.isSafe }).isTrue()
            assertThat(contributors.keys.any { it.contains("Safe Dev") }).isTrue()
            assertThat(contributors.keys.any { it.contains("External Dev") }).isFalse()
            assertThat(filesStats.containsKey("feature/Active.kt")).isTrue()
            assertThat(filesStats.containsKey("feature/OldDeleted.kt")).isFalse()
        }

        test("renamed files preserve historical contributor authorship across rename") { tmpPath ->
            val repoDir = File(tmpPath.toString(), "repo")
            initGitRepo(repoDir)

            val oldFile = File(repoDir, "lib/OldName.kt")
            oldFile.parentFile.mkdirs()
            oldFile.writeText("package lib\nclass Original")
            runGit(repoDir, "add", ".")
            runGit(
                repoDir,
                "commit",
                "-m",
                "feat: create original",
                "--author=Original Author <original@thunderbird.net>",
            )

            runGit(repoDir, "mv", "lib/OldName.kt", "lib/NewName.kt")
            runGit(repoDir, "commit", "-m", "refactor: rename", "--author=Renamer <renamer@thunderbird.net>")

            val analyzer = GitAuthorshipAnalyzer(repoDir)
            val config = SafeAuthorsConfig(safeDomains = listOf("@thunderbird.net"))
            val (contributors, filesStats) = analyzer.analyze(listOf("lib"), config)

            assertThat(contributors.size).isEqualTo(1)
            assertThat(contributors.keys.any { it.contains("Original Author") }).isTrue()
            assertThat(filesStats.containsKey("lib/NewName.kt")).isTrue()
        }

        test("revision selection matches exact HEAD SHA") { tmpPath ->
            val repoDir = File(tmpPath.toString(), "repo")
            initGitRepo(repoDir)

            val file = File(repoDir, "Test.kt")
            file.writeText("class Test")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "initial", "--author=Dev <dev@thunderbird.net>")

            val analyzer = GitAuthorshipAnalyzer(repoDir)
            val headSha = analyzer.getHeadCommit()
            assertThat(headSha.length).isEqualTo(40)
        }

        test("revision-scoped audit accurately inspects tree and history at specified past revision") { tmpPath ->
            val repoDir = File(tmpPath.toString(), "repo")
            initGitRepo(repoDir)

            val file = File(repoDir, "feature/File.kt")
            file.parentFile.mkdirs()
            file.writeText("package feature\nclass SafeFile")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "feat: initial safe", "--author=Safe Dev <safe@thunderbird.net>")

            val analyzer = GitAuthorshipAnalyzer(repoDir)
            val firstCommitSha = analyzer.getHeadCommit()

            val unsafeFile = File(repoDir, "feature/NewUnsafe.kt")
            unsafeFile.writeText("package feature\nclass Unsafe")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "feat: unsafe add", "--author=External Dev <ext@other.com>")

            val config = SafeAuthorsConfig(safeDomains = listOf("@thunderbird.net"))

            // Audit at past revision
            val (pastContributors, pastFiles) = analyzer.analyze(listOf("feature"), config, revision = firstCommitSha)
            assertThat(pastContributors.values.all { it.isSafe }).isTrue()
            assertThat(pastContributors.size).isEqualTo(1)
            assertThat(pastFiles.containsKey("feature/File.kt")).isTrue()
            assertThat(pastFiles.containsKey("feature/NewUnsafe.kt")).isFalse()

            // Audit at latest HEAD
            val (headContributors, headFiles) = analyzer.analyze(listOf("feature"), config)
            assertThat(headContributors.values.all { it.isSafe }).isFalse()
            assertThat(headFiles.containsKey("feature/NewUnsafe.kt")).isTrue()
        }

        test("copied file with subsequent line edits retains copier authorship in file stats") { tmpPath ->
            val repoDir = File(tmpPath.toString(), "repo")
            initGitRepo(repoDir)

            val baseFile = File(repoDir, "feature/Base.kt")
            baseFile.parentFile.mkdirs()
            baseFile.writeText("package feature\nclass Base {\n    val value = 1\n}\n")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "feat: base", "--author=Safe Author <safe@thunderbird.net>")

            // Unsafe contributor copies the file without changing bytes
            val copiedFile = File(repoDir, "feature/Copied.kt")
            baseFile.copyTo(copiedFile)
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "feat: copy file", "--author=External Copier <copier@other.org>")

            // Safe contributor replaces lines in the copied file
            copiedFile.writeText("package feature\nclass BaseModified {\n    val updated = 2\n}\n")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "feat: update copy", "--author=Safe Editor <editor@thunderbird.net>")

            val analyzer = GitAuthorshipAnalyzer(repoDir)
            val config = SafeAuthorsConfig(safeDomains = listOf("@thunderbird.net"))
            val (contributors, filesStats) = analyzer.analyze(listOf("feature/Copied.kt"), config)

            assertThat(contributors.values.all { it.isSafe }).isFalse()
            assertThat(contributors.keys.any { it.contains("External Copier") }).isTrue()
            assertThat(filesStats["feature/Copied.kt"]?.isSafe).isEqualTo(false)
        }

        test("binary file is verified by commit authors and not line blame") { tmpPath ->
            val repoDir = File(tmpPath.toString(), "repo")
            initGitRepo(repoDir)

            val binaryFile = File(repoDir, "assets/icon.png")
            binaryFile.parentFile.mkdirs()
            binaryFile.writeBytes(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00),
            )
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "feat: add icon", "--author=Safe Author <safe@thunderbird.net>")

            val analyzer = GitAuthorshipAnalyzer(repoDir)
            val config = SafeAuthorsConfig(safeDomains = listOf("@thunderbird.net"))
            val (contributors, filesStats) = analyzer.analyze(listOf("assets/icon.png"), config)

            assertThat(contributors.values.all { it.isSafe }).isTrue()
            assertThat(contributors.size).isEqualTo(1)
            val iconStats = filesStats["assets/icon.png"]
            assertThat(iconStats?.isSafe).isEqualTo(true)
            assertThat(iconStats?.isBinary).isEqualTo(true)
            assertThat(iconStats?.totalLines).isEqualTo(0)
            assertThat(iconStats?.authorsBlame?.isEmpty()).isEqualTo(true)
            assertThat(iconStats?.authorsCommits?.contains("Safe Author <safe@thunderbird.net>")).isEqualTo(true)
        }
    }
}
