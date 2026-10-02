/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import java.io.File
import java.io.IOException

private const val COMMIT_LOG_PARTS_LIMIT = 4
private const val COMMIT_SHA_LENGTH = 40
private const val AUTHOR_PREFIX_LEN = 7
private const val AUTHOR_MAIL_PREFIX_LEN = 12

public class GitAuthorshipAnalyzer(
    private val repoDir: File = File("."),
) {
    public fun getCurrentGitUser(): Pair<String, String> {
        val name = runGitCommand(listOf("config", "user.name")).stdout.trim()
        val email = runGitCommand(listOf("config", "user.email")).stdout.trim()
        return name to email
    }

    public fun getHeadCommit(): String {
        val result = runRequiredGitCommand(listOf("rev-parse", "HEAD"))
        return result.stdout.trim()
    }

    public fun getLatestCommitForPaths(
        targetPaths: List<String>,
        includeBuildGradle: Boolean = false,
    ): String {
        val args = mutableListOf("log", "-n", "1", "--format=%H")
        if (targetPaths.isNotEmpty()) {
            args.add("--")
            args.addAll(targetPaths)
            if (!includeBuildGradle) {
                args.add(":(exclude)*build.gradle.kts")
            }
        } else {
            args.add("--")
            args.add(".")
            if (!includeBuildGradle) {
                args.add(":(exclude)*build.gradle.kts")
            }
        }
        val result = runRequiredGitCommand(args)
        val commit = result.stdout.trim()
        return commit.ifBlank { getHeadCommit() }
    }

    public fun getTrackedFiles(
        targetPaths: List<String>,
        revision: String = getHeadCommit(),
        includeBuildGradle: Boolean = false,
    ): List<String> {
        val args = mutableListOf("ls-tree", "-r", "--name-only", revision)
        if (targetPaths.isNotEmpty()) {
            args.add("--")
            args.addAll(targetPaths)
        }
        val result = runRequiredGitCommand(args)

        return result.stdout.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { !includeBuildGradle && (it == "build.gradle.kts" || it.endsWith("/build.gradle.kts")) }
            .distinct()
            .sorted()
    }

    public fun analyze(
        targetPaths: List<String>,
        config: SafeAuthorsConfig,
        revision: String = getHeadCommit(),
        includeBuildGradle: Boolean = false,
    ): Pair<Map<String, ContributorStats>, Map<String, FileStats>> {
        val files = getTrackedFiles(
            targetPaths = targetPaths,
            revision = revision,
            includeBuildGradle = includeBuildGradle,
        )
        if (files.isEmpty()) {
            val pathStr = if (targetPaths.isNotEmpty()) targetPaths.joinToString(", ") else "."
            error("No tracked files found for target path(s): $pathStr. Audit scope cannot be empty.")
        }

        if (targetPaths.isNotEmpty()) {
            for (targetPath in targetPaths) {
                val normalizedTarget = targetPath.trimStart('/').trimEnd('/')
                val matched = files.any { it == normalizedTarget || it.startsWith("$normalizedTarget/") }
                if (!matched) {
                    error("Target path '$targetPath' does not match any tracked files in the repository.")
                }
            }
        }

        val (localUserName, localUserEmail) = getCurrentGitUser()
        val contributors = mutableMapOf<String, ContributorStats>()
        val filesStats = mutableMapOf<String, FileStats>()

        fun resolveContributor(name: String, email: String): ContributorStats {
            val (normName, normEmail) = normalizeAuthorInfo(name, email, localUserName, localUserEmail)
            val key = "$normName <$normEmail>"
            return contributors.getOrPut(key) {
                val (safe, affiliation) = config.isSafe(name = normName, email = normEmail)
                ContributorStats(name = normName, email = normEmail, isSafe = safe, affiliation = affiliation)
            }
        }

        for (filePath in files) {
            val fileStat = analyzeFile(
                filePath = filePath,
                revision = revision,
                onContributorBlame = { author, mail ->
                    val contrib = resolveContributor(name = author, email = mail)
                    val key = "${contrib.name} <${contrib.email}>"
                    contributors[key] = contrib.withIncrementedBlame()
                    contrib
                },
                onContributorCommit = { hash, author, mail ->
                    val contrib = resolveContributor(name = author, email = mail)
                    val key = "${contrib.name} <${contrib.email}>"
                    contributors[key] = contrib.withAddedCommit(hash)
                    contrib
                },
            )
            filesStats[filePath] = fileStat
        }

        return contributors to filesStats
    }

    private fun analyzeFile(
        filePath: String,
        revision: String,
        onContributorBlame: (author: String, mail: String) -> ContributorStats,
        onContributorCommit: (hash: String, author: String, mail: String) -> ContributorStats,
    ): FileStats {
        var fileIsSafe = true
        var totalLines = 0
        var isBinary = false
        val authorsBlame = mutableMapOf<String, Int>()
        val authorsCommits = mutableSetOf<String>()
        val unsafeContributors = mutableSetOf<String>()

        val fileBytesResult = runGitCommandBytes(listOf("cat-file", "-p", "$revision:$filePath"))
        if (fileBytesResult.exitCode == 0) {
            val fileBytes = fileBytesResult.stdout
            if (isBinaryContent(fileBytes)) {
                isBinary = true
            } else {
                val blameResult = runBlameForFile(filePath, revision)
                val (blameLinesCount, blameSafe) = parseBlameOutput(
                    stdout = blameResult,
                    authorsBlame = authorsBlame,
                    unsafeContributors = unsafeContributors,
                    onContributorBlame = onContributorBlame,
                )
                totalLines = blameLinesCount
                val expectedLines = countTextLines(fileBytes)
                if (!blameSafe || (fileBytes.isNotEmpty() && totalLines != expectedLines)) {
                    fileIsSafe = false
                    if (totalLines != expectedLines) {
                        val contrib = onContributorBlame("Missing Attribution", "unattributed@git.internal")
                        val key = "${contrib.name} <${contrib.email}>"
                        authorsBlame[key] = (authorsBlame[key] ?: 0) + (expectedLines - totalLines).coerceAtLeast(1)
                        unsafeContributors.add(key)
                    }
                }
            }
        } else {
            fileIsSafe = false
            error("Failed to read file '$filePath' at revision '$revision': ${fileBytesResult.stderr}")
        }

        val fLogResult = runRequiredGitCommand(
            listOf("log", "--follow", "--name-status", "--format=COMMIT|%H|%an|%ae", revision, "--", filePath),
        )
        val logSafe = parseFileLogOutput(
            stdout = fLogResult.stdout,
            exitCode = fLogResult.exitCode,
            authorsCommits = authorsCommits,
            unsafeContributors = unsafeContributors,
            onContributorCommit = onContributorCommit,
        )
        if (!logSafe) fileIsSafe = false

        return FileStats(
            path = filePath,
            isSafe = fileIsSafe,
            totalLines = totalLines,
            isBinary = isBinary,
            authorsBlame = authorsBlame,
            authorsCommits = authorsCommits,
            unsafeContributors = unsafeContributors,
        )
    }

    private fun runBlameForFile(filePath: String, revision: String): String {
        val blameArgs = listOf("blame", "--line-porcelain", revision, "--", filePath)
        val result = runRequiredGitCommand(blameArgs)
        return result.stdout
    }

    public fun runRequiredGitCommand(args: List<String>): GitExecResult {
        val result = runGitCommand(args)
        if (result.exitCode != 0) {
            val err = result.stderr.ifBlank { result.stdout }.trim()
            error("Git command failed (exit code ${result.exitCode}): '${result.cmd}'\nError: $err")
        }
        return result
    }

    public fun runGitCommand(args: List<String>): GitExecResult {
        val fullCmd = listOf("git") + args
        val cmdStr = fullCmd.joinToString(" ")
        return try {
            val process = ProcessBuilder(fullCmd)
                .directory(repoDir)
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

    public fun runGitCommandBytes(args: List<String>): GitExecBytesResult {
        val fullCmd = listOf("git") + args
        val cmdStr = fullCmd.joinToString(" ")
        return try {
            val process = ProcessBuilder(fullCmd)
                .directory(repoDir)
                .redirectErrorStream(false)
                .start()

            val stdout = process.inputStream.readBytes()
            val stderr = process.errorStream.bufferedReader(Charsets.UTF_8).readText()
            val exitCode = process.waitFor()
            GitExecBytesResult(cmd = cmdStr, exitCode = exitCode, stdout = stdout, stderr = stderr)
        } catch (e: IOException) {
            GitExecBytesResult(
                cmd = cmdStr,
                exitCode = -1,
                stdout = byteArrayOf(),
                stderr = e.message ?: "Execution failed",
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            GitExecBytesResult(
                cmd = cmdStr,
                exitCode = -1,
                stdout = byteArrayOf(),
                stderr = e.message ?: "Execution interrupted",
            )
        }
    }

    public data class GitExecResult(
        val cmd: String,
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
    )

    public data class GitExecBytesResult(
        val cmd: String,
        val exitCode: Int,
        val stdout: ByteArray,
        val stderr: String,
    )
}

internal fun isBinaryContent(bytes: ByteArray): Boolean {
    val checkLen = minOf(bytes.size, 8000)
    for (i in 0 until checkLen) {
        if (bytes[i] == 0.toByte()) {
            return true
        }
    }
    return false
}

internal fun countTextLines(bytes: ByteArray): Int {
    if (bytes.isEmpty()) return 0
    var count = 0
    for (b in bytes) {
        if (b == '\n'.code.toByte()) {
            count++
        }
    }
    if (bytes.last() != '\n'.code.toByte() && bytes.last() != '\r'.code.toByte()) {
        count++
    }
    return count
}

private fun normalizeAuthorInfo(
    name: String,
    email: String,
    localUserName: String,
    localUserEmail: String,
): Pair<String, String> {
    var normName = name.normalizeText()
    var normEmail = email.normalizeText().lowercase()

    if (normEmail == "not.committed.yet" || normName == "Not Committed Yet") {
        if (localUserEmail.isNotBlank()) {
            normEmail = localUserEmail.normalizeText().lowercase()
            normName = if (localUserName.isNotBlank()) localUserName.normalizeText() else normName
        }
    }
    return normName to normEmail
}

internal fun parseBlameOutput(
    stdout: String,
    authorsBlame: MutableMap<String, Int>,
    unsafeContributors: MutableSet<String> = mutableSetOf(),
    onContributorBlame: (author: String, mail: String) -> ContributorStats,
): Pair<Int, Boolean> {
    var isSafe = true
    var linesCount = 0
    val commitAuthors = mutableMapOf<String, Pair<String, String>>()
    var currentCommit = ""
    var currentAuthor = ""
    var currentMail = ""

    for (bLine in stdout.lines()) {
        val firstToken = bLine.substringBefore(' ')
        if (isShaHash(firstToken)) {
            currentCommit = firstToken
            currentAuthor = ""
            currentMail = ""
            commitAuthors[currentCommit]?.let { (cachedAuthor, cachedMail) ->
                currentAuthor = cachedAuthor
                currentMail = cachedMail
            }
        } else if (bLine.startsWith("author ")) {
            currentAuthor = bLine.substring(AUTHOR_PREFIX_LEN).trim()
            updateCachedAuthor(
                commitAuthors = commitAuthors,
                commit = currentCommit,
                author = currentAuthor,
                mail = currentMail,
            )
        } else if (bLine.startsWith("author-mail ")) {
            val rawMail = bLine.substring(AUTHOR_MAIL_PREFIX_LEN).trim()
            currentMail = rawMail.removeSurrounding(prefix = "<", suffix = ">").lowercase()
            updateCachedAuthor(
                commitAuthors = commitAuthors,
                commit = currentCommit,
                author = currentAuthor,
                mail = currentMail,
            )
        } else if (bLine.startsWith("\t")) {
            linesCount++
            val lineSafe = processBlameCodeLine(
                currentAuthor = currentAuthor,
                currentMail = currentMail,
                currentCommit = currentCommit,
                commitAuthors = commitAuthors,
                authorsBlame = authorsBlame,
                unsafeContributors = unsafeContributors,
                onContributorBlame = onContributorBlame,
            )
            if (!lineSafe) isSafe = false
            currentAuthor = ""
            currentMail = ""
            currentCommit = ""
        }
    }

    return linesCount to isSafe
}

private fun isShaHash(token: String): Boolean {
    return token.length == COMMIT_SHA_LENGTH && token.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
}

private fun updateCachedAuthor(
    commitAuthors: MutableMap<String, Pair<String, String>>,
    commit: String,
    author: String,
    mail: String,
) {
    if (commit.isNotEmpty()) {
        val existing = commitAuthors[commit]
        val effectiveAuthor = author.ifBlank { existing?.first.orEmpty() }
        val effectiveMail = mail.ifBlank { existing?.second.orEmpty() }
        commitAuthors[commit] = effectiveAuthor to effectiveMail
    }
}

private fun processBlameCodeLine(
    currentAuthor: String,
    currentMail: String,
    currentCommit: String,
    commitAuthors: Map<String, Pair<String, String>>,
    authorsBlame: MutableMap<String, Int>,
    unsafeContributors: MutableSet<String>,
    onContributorBlame: (author: String, mail: String) -> ContributorStats,
): Boolean {
    val effectiveAuthor = currentAuthor.ifBlank { commitAuthors[currentCommit]?.first.orEmpty() }
    val effectiveMail = currentMail.ifBlank { commitAuthors[currentCommit]?.second.orEmpty() }

    return if (effectiveAuthor.isNotBlank()) {
        val contrib = onContributorBlame(effectiveAuthor, effectiveMail)
        val key = "${contrib.name} <${contrib.email}>"
        authorsBlame[key] = (authorsBlame[key] ?: 0) + 1
        if (!contrib.isSafe) {
            unsafeContributors.add(key)
        }
        contrib.isSafe
    } else {
        val contrib = onContributorBlame("Missing Attribution", "unattributed@git.internal")
        val key = "${contrib.name} <${contrib.email}>"
        authorsBlame[key] = (authorsBlame[key] ?: 0) + 1
        unsafeContributors.add(key)
        false
    }
}

private data class FileCommitEntry(
    val hash: String,
    val author: String,
    val mail: String,
    val isRenameOnly: Boolean = false,
)

internal fun parseFileLogOutput(
    stdout: String,
    exitCode: Int,
    authorsCommits: MutableSet<String>,
    unsafeContributors: MutableSet<String> = mutableSetOf(),
    onContributorCommit: (hash: String, author: String, mail: String) -> ContributorStats,
): Boolean {
    if (exitCode != 0) {
        val contrib = onContributorCommit("", "Missing Attribution", "unattributed@git.internal")
        val key = "${contrib.name} <${contrib.email}>"
        authorsCommits.add(key)
        unsafeContributors.add(key)
        return false
    }

    val entries = mutableListOf<FileCommitEntry>()
    var currentEntry: FileCommitEntry? = null
    var hasStatusLines = false
    var allStatusesAreRenameOnly = true

    fun flushCurrentEntry() {
        val entry = currentEntry ?: return
        val isRenameOnly = hasStatusLines && allStatusesAreRenameOnly
        entries.add(entry.copy(isRenameOnly = isRenameOnly))
        currentEntry = null
        hasStatusLines = false
        allStatusesAreRenameOnly = true
    }

    for (rawLine in stdout.lines()) {
        val line = rawLine.trim()
        if (line.startsWith("COMMIT|")) {
            flushCurrentEntry()
            val parts = line.split("|", limit = COMMIT_LOG_PARTS_LIMIT)
            val hash = parts.getOrNull(1)?.trim().orEmpty()
            val author = parts.getOrNull(2)?.trim().orEmpty()
            val mail = parts.getOrNull(3)?.trim().orEmpty()
            currentEntry = FileCommitEntry(hash = hash, author = author, mail = mail)
        } else if (currentEntry != null && line.isNotEmpty()) {
            hasStatusLines = true
            val statusType = line.split(Regex("\\s+"), limit = 2).firstOrNull()?.trim().orEmpty()
            if (statusType != "R100") {
                allStatusesAreRenameOnly = false
            }
        }
    }
    flushCurrentEntry()

    if (entries.isEmpty()) {
        val contrib = onContributorCommit("", "Missing Attribution", "unattributed@git.internal")
        val key = "${contrib.name} <${contrib.email}>"
        authorsCommits.add(key)
        unsafeContributors.add(key)
        return false
    }

    val effectiveEntries = entries.filterNot { it.isRenameOnly }
        .ifEmpty { entries }

    var isSafe = true
    for (entry in effectiveEntries) {
        if (entry.author.isNotBlank()) {
            val contrib = onContributorCommit(entry.hash, entry.author, entry.mail)
            val key = "${contrib.name} <${contrib.email}>"
            authorsCommits.add(key)
            if (!contrib.isSafe) {
                isSafe = false
                unsafeContributors.add(key)
            }
        } else {
            isSafe = false
            val contrib = onContributorCommit(entry.hash, "Missing Attribution", "unattributed@git.internal")
            val key = "${contrib.name} <${contrib.email}>"
            authorsCommits.add(key)
            unsafeContributors.add(key)
        }
    }

    return isSafe
}
