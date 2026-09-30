/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package net.thunderbird.cli.authorship

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import de.infix.testBalloon.framework.core.testSuite
import java.io.File
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerializationException
import net.thunderbird.components.core.testing.temporaryDirectoryFixture

val safeAuthorsConfigTest by testSuite("SafeAuthorsConfig") {
    test("SafeAuthor matches by email and name with Unicode normalization") {
        val author = SafeAuthor(
            name = "Alice Test-René",
            emails = listOf("alice@thunderbird.net", "alice@example.com"),
            affiliation = "Staff",
        )

        assertThat(author.matches(authorName = "Alice Test-René", authorEmail = "alice@thunderbird.net")).isTrue()
        assertThat(author.matches(authorName = "Alice Test-René", authorEmail = "alice@example.com")).isTrue()
        // NFD normalized variation
        assertThat(author.matches(authorName = "Alice Test-Rene\u0301", authorEmail = "alice@thunderbird.net")).isTrue()
        assertThat(author.matches(authorName = "Alice Test-René", authorEmail = "other@example.com")).isFalse()
        assertThat(author.matches(authorName = "Sample Contributor", authorEmail = "sample@example.com")).isFalse()
    }

    test("SafeAuthorsConfig matches approved domain") {
        val config = SafeAuthorsConfig(
            safeDomains = listOf("@thunderbird.net", "@mozilla.com"),
            safeAuthors = emptyList(),
        )

        val (isSafe1, affiliation1) = config.isSafe(name = "Any Developer", email = "dev@thunderbird.net")
        assertThat(isSafe1).isTrue()
        assertThat(affiliation1).isEqualTo("Approved Domain (@thunderbird.net)")

        val (isSafe2, _) = config.isSafe(name = "Other Dev", email = "dev@external.org")
        assertThat(isSafe2).isFalse()
    }

    test("SafeAuthorsConfig matches approved author with custom email") {
        val config = SafeAuthorsConfig(
            safeDomains = listOf("@thunderbird.net"),
            safeAuthors = listOf(
                SafeAuthor(
                    name = "Staff Member",
                    emails = listOf("member@personal.com"),
                    affiliation = "Contractor",
                ),
            ),
        )

        val (isSafe, affiliation) = config.isSafe(name = "Staff Member", email = "member@personal.com")
        assertThat(isSafe).isTrue()
        assertThat(affiliation).isEqualTo("Contractor")
    }

    test("SafeAuthor matches by alias") {
        val author = SafeAuthor(
            name = "Ashley Soucar",
            aliases = listOf("Ashley"),
            emails = listOf("ashley.soucar@gmail.com", "ashley@thunderbird.net"),
            affiliation = "Staff",
        )

        assertThat(author.matches(authorName = "Ashley", authorEmail = "ashley.soucar@gmail.com")).isTrue()
        assertThat(author.matches(authorName = "Ashley Soucar", authorEmail = "ashley.soucar@gmail.com")).isTrue()
        assertThat(author.matches(authorName = "Other Person", authorEmail = "ashley.soucar@gmail.com")).isFalse()
    }

    temporaryDirectoryFixture().asParameterForEach {
        test("loads valid configuration file with snake_case keys") { tmpPath ->
            val dir = File(tmpPath.toString())
            val configFile = File(dir, "safe-authors.json")
            configFile.writeText(
                """
                {
                    "safe_domains": ["@thunderbird.net"],
                    "safe_authors": [
                        {
                            "name": "Staff Dev",
                            "aliases": ["Staff"],
                            "emails": ["staff@personal.org"],
                            "affiliation": "Staff"
                        }
                    ]
                }
                """.trimIndent(),
            )

            val loaded = SafeAuthorsConfig.load(configFile)
            assertThat(loaded.safeDomains).isEqualTo(listOf("@thunderbird.net"))
            assertThat(loaded.safeAuthors.size).isEqualTo(1)
            assertThat(loaded.safeAuthors[0].name).isEqualTo("Staff Dev")
            assertThat(loaded.safeAuthors[0].aliases).isEqualTo(listOf("Staff"))
        }

        test("loads valid configuration file with camelCase keys") { tmpPath ->
            val dir = File(tmpPath.toString())
            val configFile = File(dir, "safe-authors.json")
            configFile.writeText(
                """
                {
                    "safeDomains": ["@thunderbird.net"],
                    "safeAuthors": [
                        {
                            "name": "Staff Dev",
                            "emails": ["staff@personal.org"],
                            "affiliation": "Staff"
                        }
                    ]
                }
                """.trimIndent(),
            )

            val loaded = SafeAuthorsConfig.load(configFile)
            assertThat(loaded.safeDomains).isEqualTo(listOf("@thunderbird.net"))
            assertThat(loaded.safeAuthors.size).isEqualTo(1)
            assertThat(loaded.safeAuthors[0].name).isEqualTo("Staff Dev")
        }

        test("fails on invalid json configuration file") { tmpPath ->
            val dir = File(tmpPath.toString())
            val configFile = File(dir, "invalid.json")
            configFile.writeText("{ invalid json }")

            assertFailsWith<SerializationException> {
                SafeAuthorsConfig.load(configFile)
            }
        }
    }
}
