/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.importcode

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.github.ajalt.clikt.core.parse
import de.infix.testBalloon.framework.core.testSuite
import java.io.File
import net.thunderbird.components.core.testing.temporaryDirectoryFixture

private fun runGit(workingDir: File, vararg args: String): String {
    val fullCmd = listOf("git") + args.toList()
    val process = ProcessBuilder(fullCmd)
        .directory(workingDir)
        .redirectErrorStream(true)
        .start()

    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
        error("Git command failed (exit code $exitCode): ${fullCmd.joinToString(" ")}\nOutput: $output")
    }
    return output
}

private fun initGitRepo(repoDir: File) {
    repoDir.mkdirs()
    runGit(repoDir, "init", "--initial-branch=main")
    runGit(repoDir, "config", "user.name", "Test User")
    runGit(repoDir, "config", "user.email", "test@thunderbird.net")
}

val importCodeCommandTest by testSuite("ImportCodeCommand") {
    test("ImportCodeCommand has correct command name") {
        val command = ImportCodeCommand()
        assertThat(command.commandName).isEqualTo("import-code")
    }

    temporaryDirectoryFixture().asParameterForEach {
        test("imports only safe files and leaves out unsafe files and build gradle by default") { tmpPath ->
            val baseDir = File(tmpPath.toString())
            val repoDir = File(baseDir, "repo")
            val dstDir = File(baseDir, "dst")
            initGitRepo(repoDir)

            val safeFile = File(repoDir, "feature/src/Safe.kt")
            safeFile.parentFile.mkdirs()
            safeFile.writeText("class Safe")
            val gradleFile = File(repoDir, "feature/build.gradle.kts")
            gradleFile.writeText("plugins {}")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "safe commit", "--author=Safe Dev <safe@thunderbird.net>")

            val unsafeFile = File(repoDir, "feature/src/Unsafe.kt")
            unsafeFile.writeText("class Unsafe")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "unsafe commit", "--author=Unsafe Dev <unsafe@external.org>")

            val configFile = File(baseDir, "safe-authors.json")
            configFile.writeText(
                """
                {
                    "safeDomains": ["@thunderbird.net"],
                    "safeAuthors": []
                }
                """.trimIndent(),
            )

            val command = ImportCodeCommand()
            command.parse(
                listOf(
                    "--source-repo-url",
                    repoDir.absolutePath,
                    "-p",
                    "feature",
                    "-t",
                    dstDir.path,
                    "-s",
                    configFile.path,
                ),
            )

            assertThat(File(dstDir, "src/Safe.kt").exists()).isTrue()
            assertThat(File(dstDir, "src/Unsafe.kt").exists()).isFalse()
            assertThat(File(dstDir, "build.gradle.kts").exists()).isFalse()
        }

        test("imports build.gradle.kts when --include-build-gradle is specified and safe") {
            val baseDir = File(it.toString())
            val repoDir = File(baseDir, "repo")
            val dstDir = File(baseDir, "dst")
            initGitRepo(repoDir)

            val safeFile = File(repoDir, "feature/src/Safe.kt")
            safeFile.parentFile.mkdirs()
            safeFile.writeText("class Safe")
            val gradleFile = File(repoDir, "feature/build.gradle.kts")
            gradleFile.writeText("plugins {}")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "safe commit", "--author=Safe Dev <safe@thunderbird.net>")

            val configFile = File(baseDir, "safe-authors.json")
            configFile.writeText(
                """
                {
                    "safeDomains": ["@thunderbird.net"],
                    "safeAuthors": []
                }
                """.trimIndent(),
            )

            val command = ImportCodeCommand()
            command.parse(
                listOf(
                    "--source-repo-url", repoDir.absolutePath,
                    "-p", "feature",
                    "-t", dstDir.path,
                    "-s", configFile.path,
                    "--include-build-gradle",
                ),
            )

            assertThat(File(dstDir, "src/Safe.kt").exists()).isTrue()
            assertThat(File(dstDir, "build.gradle.kts").exists()).isTrue()
        }

        test("leaves out build.gradle.kts when --include-build-gradle is specified but author is unsafe") {
            val baseDir = File(it.toString())
            val repoDir = File(baseDir, "repo")
            val dstDir = File(baseDir, "dst")
            initGitRepo(repoDir)

            val safeFile = File(repoDir, "feature/src/Safe.kt")
            safeFile.parentFile.mkdirs()
            safeFile.writeText("class Safe")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "safe commit", "--author=Safe Dev <safe@thunderbird.net>")

            val gradleFile = File(repoDir, "feature/build.gradle.kts")
            gradleFile.writeText("plugins {}")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "unsafe gradle commit", "--author=Unsafe Dev <unsafe@external.org>")

            val configFile = File(baseDir, "safe-authors.json")
            configFile.writeText(
                """
                {
                    "safeDomains": ["@thunderbird.net"],
                    "safeAuthors": []
                }
                """.trimIndent(),
            )

            val command = ImportCodeCommand()
            command.parse(
                listOf(
                    "--source-repo-url", repoDir.absolutePath,
                    "-p", "feature",
                    "-t", dstDir.path,
                    "-s", configFile.path,
                    "--include-build-gradle",
                ),
            )

            assertThat(File(dstDir, "src/Safe.kt").exists()).isTrue()
            assertThat(File(dstDir, "build.gradle.kts").exists()).isFalse()
        }

        test("imports unsafe files when --skip-authorship-check is specified") {
            val baseDir = File(it.toString())
            val repoDir = File(baseDir, "repo")
            val dstDir = File(baseDir, "dst")
            initGitRepo(repoDir)

            val unsafeFile = File(repoDir, "feature/src/Unsafe.kt")
            unsafeFile.parentFile.mkdirs()
            unsafeFile.writeText("class Unsafe")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "unsafe commit", "--author=Unsafe Dev <unsafe@external.org>")

            val configFile = File(baseDir, "safe-authors.json")
            configFile.writeText(
                """
                {
                    "safeDomains": ["@thunderbird.net"],
                    "safeAuthors": []
                }
                """.trimIndent(),
            )

            val command = ImportCodeCommand()
            command.parse(
                listOf(
                    "--source-repo-url", repoDir.absolutePath,
                    "-p", "feature",
                    "-t", dstDir.path,
                    "-s", configFile.path,
                    "--skip-authorship-check",
                ),
            )

            assertThat(File(dstDir, "src/Unsafe.kt").exists()).isTrue()
        }

        test("dry-run mode does not write files to destination") {
            val baseDir = File(it.toString())
            val repoDir = File(baseDir, "repo")
            val dstDir = File(baseDir, "dst")
            initGitRepo(repoDir)

            val safeFile = File(repoDir, "feature/src/Safe.kt")
            safeFile.parentFile.mkdirs()
            safeFile.writeText("class Safe")
            runGit(repoDir, "add", ".")
            runGit(repoDir, "commit", "-m", "safe commit", "--author=Safe Dev <safe@thunderbird.net>")

            val configFile = File(baseDir, "safe-authors.json")
            configFile.writeText(
                """
                {
                    "safeDomains": ["@thunderbird.net"],
                    "safeAuthors": []
                }
                """.trimIndent(),
            )

            val command = ImportCodeCommand()
            command.parse(
                listOf(
                    "--source-repo-url", repoDir.absolutePath,
                    "-p", "feature",
                    "-t", dstDir.path,
                    "-s", configFile.path,
                    "--dry-run",
                ),
            )

            assertThat(File(dstDir, "src/Safe.kt").exists()).isFalse()
        }
    }
}
