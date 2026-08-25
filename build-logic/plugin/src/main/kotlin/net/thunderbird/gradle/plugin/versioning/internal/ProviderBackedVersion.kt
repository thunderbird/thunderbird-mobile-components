/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.gradle.plugin.versioning.internal

import org.gradle.api.provider.Provider

internal class ProviderBackedVersion(
    private val provider: Provider<String>,
) {
    override fun toString(): String = provider.get()
}
