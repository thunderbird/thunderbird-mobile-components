/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import java.io.File

internal fun runGit(workingDir: File, vararg args: String): String {
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

internal fun initGitRepo(repoDir: File) {
    repoDir.mkdirs()
    runGit(repoDir, "init")
    runGit(repoDir, "config", "user.name", "Test User")
    runGit(repoDir, "config", "user.email", "test@thunderbird.net")
}
