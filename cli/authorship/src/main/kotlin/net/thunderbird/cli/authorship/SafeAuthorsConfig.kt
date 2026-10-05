/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

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
            val document = json.parseToJsonElement(content).jsonObject
            return if ("repoUrl" in document || "revision" in document) {
                val reference = json.decodeFromJsonElement<SafeAuthorsReference>(document)
                loadFromRepository(reference)
            } else {
                json.decodeFromJsonElement<SafeAuthorsConfig>(document)
            }
        }

        private fun loadFromRepository(reference: SafeAuthorsReference): SafeAuthorsConfig {
            require(Regex("[0-9a-fA-F]{40}").matches(reference.revision)) {
                "Safe authors revision must be a full Git commit SHA"
            }
            require(reference.repoUrl.isNotBlank()) { "Safe authors repository URL must not be blank" }
            require(reference.path == "safe-authors.json") {
                "Only safe-authors.json is supported in the private repository"
            }

            val directory = Files.createTempDirectory("tmc-safe-authors-").toFile()
            try {
                runGit(directory, "init")
                runGit(
                    directory,
                    "-c",
                    "credential.helper=",
                    "fetch",
                    "--depth=1",
                    reference.repoUrl,
                    reference.revision,
                )
                val fetchedCommit = runGit(directory, "rev-parse", "FETCH_HEAD").trim()
                if (!fetchedCommit.equals(reference.revision, ignoreCase = true)) {
                    throw IOException("Fetched safe authors revision differs from the pinned commit")
                }
                val blob = runGit(directory, "show", "FETCH_HEAD:${reference.path}")
                return json.decodeFromString<SafeAuthorsConfig>(blob)
            } finally {
                directory.deleteRecursively()
            }
        }

        private fun runGit(directory: File, vararg args: String): String {
            val process = ProcessBuilder(listOf("git") + args)
                .directory(directory)
                .redirectErrorStream(true)
                .apply { environment()["GIT_TERMINAL_PROMPT"] = "0" }
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (process.waitFor() !=
                0
            ) {
                throw IOException("Unable to load safe authors configuration from private repository")
            }
            return output
        }
    }
}
