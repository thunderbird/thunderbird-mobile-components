/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.gradle.plugin.versioning.internal

import java.io.File

internal class GitVersionReader(
    private val resolver: GitVersionResolver = GitVersionResolver(),
) {

    fun read(
        repoRoot: File,
        versionFile: File,
        version: Version,
    ): String {
        val tagName = resolver.tagName(versionFile, version)
        val tagOutput = tagsPointingAtHead(repoRoot, tagName)
        return resolver.resolve(version, tagName, tagOutput)
    }

    private fun tagsPointingAtHead(repoRoot: File, tagName: String): String {
        val command = listOf("git", "-C", repoRoot.absolutePath, "tag", "--points-at", "HEAD", "--list", tagName)
        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { reader ->
                val output = reader.readLines()
                if (process.waitFor() == 0) {
                    output.joinToString("\n")
                } else {
                    ""
                }
            }
        } catch (_: Exception) {
            ""
        }
    }
}
