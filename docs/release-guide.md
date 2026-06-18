# Release Guide

This guide describes the release workflow for Thunderbird Mobile Components.

## Prerequisites

- Start from an up-to-date `main` branch.

## Release Workflow

1. Run the changelog update task for the component being released.
2. Review the generated `Unreleased` changelog entries.
3. Apply the release version changes.
4. Finalize the changelog for the release version.
5. Open a release pull request with the changelog and version changes.
6. Merge the release pull request.
7. Create the component release tag from the merged release commit.
8. Publish the release.

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

The task fails if the component version is still a snapshot or the tag already exists.

## Review Checklist

Before merging the release pull request, verify:

- The changelog contains only entries for the release being prepared.
- Entries are grouped under the expected sections.
- The release version and changelog version match.

