/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import java.io.File
import java.io.IOException

public class GitRepoManager(
    private val workingDir: File = File("."),
) {
    public fun resolveRepository(
        sourceRepoUrl: String,
        sourceCommit: String,
        onProgress: (String) -> Unit = {},
    ): File {
        val tmpDir = File(workingDir, "build/tmp")
        if (!tmpDir.exists()) {
            tmpDir.mkdirs()
        }

        val repoFolderName = getRepoFolderName(sourceRepoUrl)
        val targetRepoDir = File(tmpDir, repoFolderName)

        val needsFreshClone = if (File(targetRepoDir, ".git").isDirectory) {
            val originResult = runGit(targetRepoDir, listOf("remote", "get-url", "origin"))
            originResult.exitCode != 0 || originResult.stdout.trim() != sourceRepoUrl.trim()
        } else {
            true
        }

        if (needsFreshClone) {
            cleanup(targetRepoDir)
            onProgress("Cloning $sourceRepoUrl into ${targetRepoDir.path}...")
            val cloneResult = runGit(
                workingDir = tmpDir,
                args = listOf("clone", sourceRepoUrl, repoFolderName),
            )
            if (cloneResult.exitCode != 0) {
                val err = cloneResult.stderr.ifBlank { cloneResult.stdout }.trim()
                fail("Failed to clone repository '$sourceRepoUrl':\n$err")
            }
        } else {
            onProgress("Updating repository in ${targetRepoDir.path}...")
            runRequiredGit(targetRepoDir, listOf("reset", "--hard", "HEAD"))
            runRequiredGit(targetRepoDir, listOf("clean", "-fdx"))
            val fetchResult = runGit(
                workingDir = targetRepoDir,
                args = listOf("fetch", "origin"),
            )
            if (fetchResult.exitCode != 0) {
                val err = fetchResult.stderr.ifBlank { fetchResult.stdout }.trim()
                fail("Failed to fetch updates for repository in ${targetRepoDir.path}:\n$err")
            }
        }

        runRequiredGit(targetRepoDir, listOf("reset", "--hard", "HEAD"))
        runRequiredGit(targetRepoDir, listOf("clean", "-fdx"))

        if (sourceCommit.isNotBlank()) {
            checkoutCommit(targetRepoDir, sourceCommit, onProgress)
        } else {
            checkoutDefaultBranch(targetRepoDir)
        }
        runRequiredGit(targetRepoDir, listOf("reset", "--hard", "HEAD"))
        runRequiredGit(targetRepoDir, listOf("clean", "-fdx"))

        return targetRepoDir
    }

    public fun cleanup(repoDir: File) {
        val tmpDir = File(workingDir, "build/tmp")
        if (repoDir.exists() && repoDir.canonicalPath.startsWith(tmpDir.canonicalPath)) {
            repoDir.deleteRecursively()
        }
    }

    private fun runRequiredGit(repoDir: File, args: List<String>): GitExecResult {
        val result = runGit(repoDir, args)
        if (result.exitCode != 0) {
            val err = result.stderr.ifBlank { result.stdout }.trim()
            fail("Git command failed in ${repoDir.path}: ${result.cmd}\n$err")
        }
        return result
    }

    private fun checkoutDefaultBranch(targetRepoDir: File) {
        val checkoutDefaultResult = runGit(targetRepoDir, listOf("checkout", "origin/main"))
        if (checkoutDefaultResult.exitCode != 0) {
            val fallbackMain = runGit(targetRepoDir, listOf("checkout", "main"))
            if (fallbackMain.exitCode != 0) {
                val err = fallbackMain.stderr.ifBlank { fallbackMain.stdout }.trim()
                fail("Failed to checkout default branch in ${targetRepoDir.path}:\n$err")
            }
        }
    }

    private fun checkoutCommit(
        repoDir: File,
        commit: String,
        onProgress: (String) -> Unit,
    ) {
        onProgress("Checking out $commit...")
        val checkoutResult = runGit(repoDir, listOf("checkout", commit))
        if (checkoutResult.exitCode != 0) {
            val fetchResult = runGit(repoDir, listOf("fetch", "origin", commit))
            if (fetchResult.exitCode != 0) {
                val err = fetchResult.stderr.ifBlank { fetchResult.stdout }.trim()
                fail("Failed to fetch commit '$commit' in ${repoDir.path}:\n$err")
            }
            val retryCheckout = runGit(repoDir, listOf("checkout", commit))
            if (retryCheckout.exitCode != 0) {
                val err = retryCheckout.stderr.ifBlank { retryCheckout.stdout }.trim()
                fail("Failed to checkout commit '$commit' in ${repoDir.path}:\n$err")
            }
        }
    }

    private fun getRepoFolderName(repoUrl: String): String {
        val clean = repoUrl.trim().trimEnd('/').removeSuffix(".git")
        val name = clean.substringAfterLast('/')
        return name.ifBlank { "source-repo" }
    }

    private fun fail(message: String): Nothing = error(message)

    private fun runGit(workingDir: File, args: List<String>): GitExecResult {
        val fullCmd = listOf("git") + args
        val cmdStr = fullCmd.joinToString(" ")
        return try {
            val process = ProcessBuilder(fullCmd)
                .directory(workingDir)
                .redirectErrorStream(false)
                .start()

            val stdout = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
            val stderr = process.errorStream.bufferedReader(Charsets.UTF_8).readText()
            val exitCode = process.waitFor()
            GitExecResult(cmd = cmdStr, exitCode = exitCode, stdout = stdout, stderr = stderr)
        } catch (e: IOException) {
            GitExecResult(cmd = cmdStr, exitCode = -1, stdout = "", stderr = e.message ?: "Execution failed")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            GitExecResult(cmd = cmdStr, exitCode = -1, stdout = "", stderr = e.message ?: "Execution interrupted")
        }
    }

    private data class GitExecResult(
        val cmd: String,
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
    )
}
