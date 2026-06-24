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
