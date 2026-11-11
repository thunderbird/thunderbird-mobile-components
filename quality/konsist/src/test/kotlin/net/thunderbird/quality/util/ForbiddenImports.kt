/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.quality.util

import com.lemonappdev.konsist.api.container.KoScope

/**
 * Fails if any import in [scope] matches [isForbidden], except in [excludedFileNames].
 *
 * Reports violations grouped by import, with replacements and clickable source locations.
 */
fun assertNoForbiddenImports(
    message: String,
    replacementFor: (importName: String) -> String,
    excludedFileNames: Set<String> = emptySet(),
    scope: KoScope = projectScope,
    isForbidden: (importName: String) -> Boolean,
) {
    val violations = scope.files
        .filterNot { it.name in excludedFileNames }
        .flatMap { it.imports }
        .filter { isForbidden(it.name) }
    if (violations.isEmpty()) return

    val report = violations.groupBy { it.name }
        .toSortedMap()
        .entries
        .joinToString(separator = "\n") { (importName, imports) ->
            val locations = imports.map { "  file://${it.location}" }.sorted()
            (listOf("$importName -> ${replacementFor(importName)}") + locations).joinToString(separator = "\n")
        }

    throw AssertionError("${violations.size} forbidden imports. $message\n\n$report")
}
