/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.gradle.plugin.publishing

import net.thunderbird.gradle.plugin.ProjectConfig
import org.gradle.api.Project

internal fun Project.configurePublishedGroup() {
    if (hasDefaultGroup()) {
        group = publishedGroup()
    }
}

private fun Project.hasDefaultGroup(): Boolean {
    val currentGroup = group.toString()
    return currentGroup == DEFAULT_GROUP || currentGroup == defaultGradleGroup()
}

@Suppress("UnstableApiUsage")
private fun Project.defaultGradleGroup(): String {
    val parentPath = path
        .split(":")
        .filter(String::isNotBlank)
        .dropLast(1)

    return (listOf(isolated.rootProject.name) + parentPath)
        .joinToString(".")
}

private fun Project.publishedGroup(): String {
    val parentSegments = path
        .split(":")
        .filter(String::isNotBlank)
        .dropLast(1)

    return (listOf(ProjectConfig.group) + parentSegments)
        .joinToString(".")
}

private const val DEFAULT_GROUP = "unspecified"
