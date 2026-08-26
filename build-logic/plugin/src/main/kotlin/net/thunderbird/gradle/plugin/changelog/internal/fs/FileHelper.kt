/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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
