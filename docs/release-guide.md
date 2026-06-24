# Release Guide

This guide describes the release workflow for Thunderbird Mobile Components.

## Prerequisites

- Start from an up-to-date `main` branch.

## Release Workflow

1. Create a release branch from an up-to-date `main` branch. See [Release Branches](#release-branches).
2. Run the changelog update task for the component being released.
3. Review the generated `Unreleased` changelog entries.
4. Apply the release version changes.
5. Finalize the changelog for the release version.
6. Open a release pull request with the changelog and version changes.
7. Merge the release pull request.
8. Create the component release tag from the merged release commit.
9. Publish the release.
10. Bump the component version for the next development cycle.

## Release Branches

Release branch names must follow this pattern:

```text
release/<component>-<version>
```

Example:

```text
release/bom-1.0.0
```

For a coordinated release of multiple components, use a release-train branch name instead of encoding every component
and version in the branch name:

```text
release/tmc-YYYY-MM-DD
```

Example:

```text
release/tmc-2026-06-23
```

List the exact component versions in the release pull request description.

Validation regex:

```text
^release\/([a-zA-Z-]+-\d+\.\d+\.\d+|tmc-\d{4}-\d{2}-\d{2}(-\d+)?)$
```

## Changelog Tasks

For a component, run the changelog task on that component project. Example for `:components:bom`:

```bash
./gradlew :components:bom:updateChangelog
```

The changelog is written next to the component `version.properties` file.

After reviewing the generated entries, finalize the `Unreleased` section:

```bash
./gradlew :components:bom:finalizeChangelog -PreleaseVersion=1.0.0
```

To use a specific release date:

```bash
./gradlew :components:bom:finalizeChangelog -PreleaseVersion=1.0.0 -PreleaseDate=2026-06-18
```

The finalize task updates the changelog only. It does not create a git tag.

## Release Tags

After the release pull request has been merged, update `main` to the merged release commit and create the component
release tag. Example for `:components:bom`:

```bash
./gradlew :components:bom:createReleaseTag
```

The task reads the component version from `version.properties` and creates a local git tag in this format:

```text
<component>-<version>
```

For example:

```text
bom-1.0.0
```

The task fails if the tag already exists.

## Post-release Version Bump

After publishing a release, bump the component `version.properties` to the next patch version in a separate commit or
pull request:

```bash
./gradlew :components:bom:versionBumpPatch
```

If the next release later needs to become a minor or major release, promote the development version with the matching
version bump task before that release is prepared.

The version file stores only the base semantic version. Snapshot state is not persisted in `version.properties`;
non-release builds derive their snapshot version from the fact that the current commit is not tagged with the
component release tag. For example, after bumping from `1.0.0` to `1.0.1`, `printVersion` reports `1.0.1-SNAPSHOT`
until the matching release tag is created on a future release commit.

This post-release bump can be automated after a successful publish, but it should still be committed separately from
the release commit so the release tag continues to point at the exact released version.

## Review Checklist

Before merging the release pull request, verify:

- The changelog contains only entries for the release being prepared.
- Entries are grouped under the expected sections.
- The release version and changelog version match.
- A post-release version bump is planned or automated after publishing.

