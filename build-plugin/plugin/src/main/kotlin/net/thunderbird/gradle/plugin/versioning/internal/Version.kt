package net.thunderbird.gradle.plugin.versioning.internal

internal data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val snapshot: Boolean,
) {
    fun toStringValue(): String = buildString {
        append(major).append('.').append(minor).append('.').append(patch)
        if (snapshot) append("-SNAPSHOT")
    }

    fun bumpMajor(): Version = copy(major = major + 1, minor = 0, patch = 0)
    fun bumpMinor(): Version = copy(minor = minor + 1, patch = 0)
    fun bumpPatch(): Version = copy(patch = patch + 1)
    fun toggleSnapshot(): Version = copy(snapshot = !snapshot)
}
