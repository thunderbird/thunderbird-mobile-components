package net.thunderbird.gradle.plugin.changelog.internal.render

import net.thunderbird.gradle.plugin.changelog.internal.Changelog

/**
 * Renders a changelog model into a text representation.
 */
internal interface ChangelogRenderer {
    /**
     * Render the entire changelog file (header + all blocks).
     */
    fun render(changelog: Changelog): String
}
