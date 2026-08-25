package net.thunderbird.gradle.plugin.versioning.internal

import org.gradle.api.provider.Provider

internal class ProviderBackedVersion(
    private val provider: Provider<String>,
) {
    override fun toString(): String = provider.get()
}
