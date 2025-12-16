package net.thunderbird.gradle.plugin.changelog.internal.git

import java.io.File

/**
 * Small utility wrapper around invoking git for changelog purposes.
 * Encapsulates command construction and error handling.
 */
internal class GitClient(
    private val logWarn: (String) -> Unit = {},
) {

    /**
     * Returns commit subjects for commits that changed files within the given component path.
     *
     * It searches first-parent history from the given `startRef` (exclusive) to the mainline
     * ref. If `startRef` is not given, it defaults to the mainline ref.
     *
     * First-parent history keeps merge commits and direct commits from the release branch,
     * but excludes individual commits from merged PR branches.
     *
     * @param repoRoot absolute repository root directory
     * @param relativePath path relative to repoRoot to restrict the log
     * @param startRef optional exclusive lower bound; when present, logs `startRef..mainlineRef`
     */
    fun logComponentSubjects(
        repoRoot: File,
        relativePath: String?,
        startRef: String?,
        refCandidates: List<String> = DEFAULT_MAINLINE_REF_CANDIDATES,
    ): List<String> {
        val mainlineRef = resolveMainlineRef(repoRoot, refCandidates) ?: return emptyList()
        val revision = if (startRef != null) "$startRef..$mainlineRef" else mainlineRef
        val args = mutableListOf(
            "git",
            "-C",
            repoRoot.absolutePath,
            "log",
            "--first-parent",
            "--min-parents=1",
            "--pretty=%s",
            revision,
        )
        if (!relativePath.isNullOrBlank()) {
            args.add("--")
            args.add(relativePath)
        }
        return run(args)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    fun firstExistingRef(repoRoot: File, refCandidates: List<String>): String? {
        return refCandidates.firstOrNull { ref -> refExists(repoRoot, ref) }
    }

    private fun resolveMainlineRef(repoRoot: File, refCandidates: List<String>): String? {
        val mainlineRef = refCandidates.firstOrNull { ref ->
            refExists(repoRoot, ref)
        }
        if (mainlineRef == null) {
            logWarn("[changelog] No mainline git ref found. Tried: ${refCandidates.joinToString()}")
        }
        return mainlineRef
    }

    private fun refExists(repoRoot: File, ref: String): Boolean {
        val cmd = listOf("git", "-C", repoRoot.absolutePath, "rev-parse", "--verify", "--quiet", "$ref^{commit}")
        return try {
            ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()
                .waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun run(cmd: List<String>): List<String> = try {
        val pb = ProcessBuilder(cmd)
        pb.redirectErrorStream(true)
        val proc = pb.start()
        val out = proc.inputStream.bufferedReader().readLines()
        val exit = proc.waitFor()
        if (exit != 0) {
            logWarn("[changelog] Command failed ($exit): ${cmd.joinToString(" ")}")
            emptyList()
        } else {
            out
        }
    } catch (e: Exception) {
        logWarn("[changelog] Failed to run ${cmd.joinToString(" ")}: ${e.message}")
        emptyList()
    }

    private companion object {
        val DEFAULT_MAINLINE_REF_CANDIDATES = listOf("origin/main", "main", "HEAD")
    }
}
