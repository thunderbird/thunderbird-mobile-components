/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.github.ajalt.clikt.core.parse
import de.infix.testBalloon.framework.core.testSuite
import java.io.File
import net.thunderbird.components.core.testing.temporaryDirectoryFixture

val checkAuthorshipCommandTest by testSuite("CheckAuthorshipCommand") {
    temporaryDirectoryFixture().asParameterForEach {
        test("CLI source-commit ref option is resolved and reported as full 40-character SHA") { tmpPath ->
            val baseDir = File(tmpPath.toString())
            val repoDir = File(baseDir, "repo")
            initGitRepo(repoDir)

            val file = File(repoDir, "src/Test.kt")
            file.parentFile.mkdirs()
            file.writeText("class Test")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "initial", "--author=Safe Dev <safe@thunderbird.net>")

            val analyzer = GitAuthorshipAnalyzer(repoDir)
            val expectedSha = analyzer.getHeadCommit()
            assertThat(expectedSha.length).isEqualTo(40)

            val configFile = File(baseDir, "safe-authors.json")
            configFile.writeText(
                """
                {
                    "safeDomains": ["@thunderbird.net"],
                    "safeAuthors": []
                }
                """.trimIndent(),
            )

            val reportFile = File(baseDir, "output-report.md")
            val command = CheckAuthorshipCommand()
            command.parse(
                listOf(
                    "--source-repo-url", repoDir.absolutePath,
                    "-p", "src",
                    "-c", "HEAD",
                    "-s", configFile.path,
                    "-o", reportFile.path,
                ),
            )

            assertThat(reportFile.exists()).isTrue()
            val reportContent = reportFile.readText()
            assertThat(reportContent).contains(expectedSha)
            assertThat(
                reportContent,
            ).contains("Source: ${repoDir.absolutePath}/tree/$expectedSha/src")
        }
    }
}
