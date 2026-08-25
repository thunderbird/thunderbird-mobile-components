package net.thunderbird.gradle.plugin.changelog.internal.fs

import java.io.File

/**
 * Shared helpers to resolve component files and metadata for changelog tasks.
 * Centralizes common file-handling logic to avoid duplication across tasks/classes.
 */
internal object FileHelper {

    /**
     * Locate the directory that contains the nearest version.properties walking up to the repo root.
     */
    fun locateNearestVersionDir(start: File, repoRoot: File): File? {
        var dir: File? = start
        while (dir != null) {
            val candidate = File(dir, VERSION_FILE)
            if (candidate.exists()) return dir
            if (dir == repoRoot) break
            dir = dir.parentFile
        }
        return null
    }

    internal const val VERSION_FILE = "version.properties"
    internal const val CHANGELOG_FILE = "CHANGELOG.md"
}
