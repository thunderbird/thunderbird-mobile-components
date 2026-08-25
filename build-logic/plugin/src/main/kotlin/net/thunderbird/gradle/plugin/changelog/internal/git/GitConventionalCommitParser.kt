package net.thunderbird.gradle.plugin.changelog.internal.git

import net.thunderbird.gradle.plugin.changelog.internal.SectionType

/**
 * Parser for Git conventional commit messages.
 */
internal class GitConventionalCommitParser {

    fun parse(message: String, repoUrl: String? = null): ConventionalCommit? {
        // Conventional Commits: type[scope][!]: description
        // - scope is optional
        // - breaking '!' may appear after type or after scope
        // - allow optional space after ':'
        val regex = Regex(
            pattern = "^(?<type>[a-zA-Z]+)(?:\\((?<scope>[^)]+)\\))?(?<breaking>!)?:\\s*(?<desc>.+)",
        )
        val match = regex.find(message) ?: return null
        val type = match.groups["type"]?.value?.lowercase()
        val scope = match.groups["scope"]?.value
        var description = match.groups["desc"]?.value?.trim() ?: message.trim()

        if (type == null) return null

        if (repoUrl != null) {
            description = linkPullRequests(description, repoUrl)
        }

        return ConventionalCommit(
            type = mapTypeToSectionType(type) ?: return null,
            scope = scope,
            description = description,
        )
    }

    private fun linkPullRequests(description: String, repoUrl: String): String {
        // Find (#123) and replace with ([#123](repoUrl/pull/123))
        val prRegex = Regex("\\(#(\\d+)\\)")
        val baseUrl = repoUrl.removeSuffix("/")
        return prRegex.replace(description) { matchResult ->
            val prNumber = matchResult.groupValues[1]
            "([#$prNumber]($baseUrl/pull/$prNumber))"
        }
    }

    private fun mapTypeToSectionType(type: String): SectionType? = when (type) {
        "feat" -> SectionType.Features
        "fix" -> SectionType.BugFixes
        "docs" -> SectionType.Documentation
        "style" -> SectionType.Styles
        "refactor", "perf" -> SectionType.Refactoring
        "test" -> SectionType.Tests
        "chore", "build", "ci", "deps" -> SectionType.Chores
        "revert" -> SectionType.Reverts
        else -> null
    }
}
