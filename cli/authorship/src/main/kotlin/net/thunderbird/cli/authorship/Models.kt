/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import java.io.File
import java.text.Normalizer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames

internal fun String.normalizeText(): String = Normalizer.normalize(this.trim(), Normalizer.Form.NFC)

@OptIn(ExperimentalSerializationApi::class)
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
public data class SafeAuthorsConfig(
    val description: String? = null,
    @JsonNames("safe_domains", "safeDomains")
    val safeDomains: List<String> = emptyList(),
    @JsonNames("safe_authors", "safeAuthors")
    val safeAuthors: List<SafeAuthor> = emptyList(),
) {
    public fun isSafe(name: String, email: String): Pair<Boolean, String> {
        val normEmail = email.normalizeText().lowercase()
        val normName = name.normalizeText()

        val matchedAuthor = safeAuthors.firstOrNull { it.matches(authorName = normName, authorEmail = normEmail) }
        if (matchedAuthor != null) {
            return true to matchedAuthor.affiliation
        }

        val domainPart = normEmail.substringAfter(delimiter = "@", missingDelimiterValue = "")
        if (domainPart.isNotEmpty()) {
            val matchedDomain = safeDomains.firstOrNull { rawDomain ->
                val cleanDomain = rawDomain.normalizeText().lowercase().removePrefix("@")
                domainPart == cleanDomain || domainPart.endsWith(".$cleanDomain")
            }
            if (matchedDomain != null) {
                return true to "Approved Domain ($matchedDomain)"
            }
        }

        return false to "External / Unverified"
    }

    public companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        public fun load(configFile: File): SafeAuthorsConfig {
            require(configFile.isFile) {
                "Safe authors configuration file does not exist: ${configFile.absolutePath}"
            }
            val content = configFile.readText(Charsets.UTF_8)
            return json.decodeFromString<SafeAuthorsConfig>(content)
        }
    }
}

@Serializable
public data class ContributorStats(
    val name: String,
    val email: String,
    val isSafe: Boolean,
    val affiliation: String,
    val commitCount: Int = 0,
    val blameLines: Int = 0,
    val commits: Set<String> = emptySet(),
) {
    public fun withIncrementedBlame(): ContributorStats = copy(blameLines = blameLines + 1)

    public fun withAddedCommit(commitHash: String): ContributorStats {
        val updatedCommits = commits + commitHash
        return copy(commits = updatedCommits, commitCount = updatedCommits.size)
    }
}

@Serializable
public data class FileStats(
    val path: String,
    val isSafe: Boolean,
    val totalLines: Int = 0,
    val isBinary: Boolean = false,
    val authorsBlame: Map<String, Int> = emptyMap(),
    val authorsCommits: Set<String> = emptySet(),
    val unsafeContributors: Set<String> = emptySet(),
)

@Serializable
public data class AuthorshipReport(
    val allSafe: Boolean,
    val sourceRepoUrl: String,
    val sourceCommit: String,
    val targetPaths: List<String>,
    val contributors: List<ContributorStats>,
    val filesStats: Map<String, FileStats>,
    val commitMessage: String,
)
