/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.gradle.plugin.versioning.internal

internal data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
) {
    fun toStringValue(): String = buildString {
        append(major).append('.').append(minor).append('.').append(patch)
    }

    fun bumpMajor(): Version = copy(major = major + 1, minor = 0, patch = 0)
    fun bumpMinor(): Version = copy(minor = minor + 1, patch = 0)
    fun bumpPatch(): Version = copy(patch = patch + 1)
}
