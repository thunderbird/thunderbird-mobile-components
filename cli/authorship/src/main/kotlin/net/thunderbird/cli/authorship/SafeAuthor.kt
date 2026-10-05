/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import java.text.Normalizer
import kotlinx.serialization.Serializable

internal fun String.normalizeText(): String = Normalizer.normalize(this.trim(), Normalizer.Form.NFC)

@Serializable
public data class SafeAuthor(
    val name: String,
    val aliases: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val affiliation: String = "Approved Contributor",
) {
    public fun matches(authorName: String, authorEmail: String): Boolean {
        val normEmail = authorEmail.normalizeText().lowercase()
        val normName = authorName.normalizeText().lowercase()
        val primaryName = name.normalizeText().lowercase()
        val allNames = listOf(primaryName) + aliases.map { it.normalizeText().lowercase() }
        val allEmails = emails.map { it.normalizeText().lowercase() }

        return if (allEmails.isNotEmpty()) {
            allEmails.contains(normEmail) && (allNames.contains(normName) || normName.isBlank())
        } else {
            allNames.contains(normName)
        }
    }
}
