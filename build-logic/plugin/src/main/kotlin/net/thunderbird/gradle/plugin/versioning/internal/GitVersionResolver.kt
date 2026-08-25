/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.gradle.plugin.versioning.internal

import java.io.File

internal class GitVersionResolver {

    fun resolve(
        version: Version,
        tagName: String,
        tagOutput: String,
    ): String {
        val releaseVersion = version.toStringValue()
        return if (tagOutput.lineSequence().any { it.trim() == tagName }) {
            releaseVersion
        } else {
            "$releaseVersion-SNAPSHOT"
        }
    }

    fun tagName(
        versionFile: File,
        version: Version,
    ): String {
        return "${versionFile.parentFile.name}-${version.toStringValue()}"
    }
}
