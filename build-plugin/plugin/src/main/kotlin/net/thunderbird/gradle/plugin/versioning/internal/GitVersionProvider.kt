package net.thunderbird.gradle.plugin.versioning.internal

import java.io.File
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

internal class GitVersionProvider(
    private val providers: ProviderFactory,
    private val resolver: GitVersionResolver = GitVersionResolver(),
) {

    fun resolve(
        repoRoot: File,
        versionFile: File,
        version: Version,
    ): Provider<String> {
        val tagName = resolver.tagName(versionFile, version)

        return providers.exec {
            commandLine("git", "-C", repoRoot.absolutePath, "tag", "--points-at", "HEAD", "--list", tagName)
            isIgnoreExitValue = true
        }.standardOutput.asText.map { tagOutput ->
            resolver.resolve(version, tagName, tagOutput)
        }
    }
}
