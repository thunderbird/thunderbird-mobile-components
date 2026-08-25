/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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
