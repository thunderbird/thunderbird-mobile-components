# Release Guide

This guide is for maintainers preparing and publishing Thunderbird Mobile Components releases.

## Prerequisites

- Maven Central credentials and signing properties are available to the publishing environment.

The publishing workflows expect these repository secrets:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `SIGNING_IN_MEMORY_KEY`
- `SIGNING_IN_MEMORY_KEY_ID`
- `SIGNING_IN_MEMORY_KEY_PASSWORD`

## Release

Releases start with a release preparation pull request.

1. Create a release branch from `main`. See [Release Branches](#release-branches).
2. Verify that the component `version.properties` contains the intended release version. If the release should be
   promoted from the current patch development version to a minor or major version, update it before finalizing the
   changelog.
3. Update the component changelog:

```bash
./gradlew <component-path>:updateChangelog
```

4. Review `CHANGELOG.md` and keep only entries intended for this release.
5. Finalize the changelog:

```bash
./gradlew <component-path>:finalizeChangelog
```

The task uses the component version from `version.properties`.

To use a specific release date:

```bash
./gradlew <component-path>:finalizeChangelog -PreleaseDate=2026-06-18
```

6. Open the release pull request.
7. Before merging, verify:

- `version.properties` contains the intended release version.
- `CHANGELOG.md` contains the finalized release section.
- The finalized changelog version matches `version.properties`.
- The pull request contains no unrelated changes.

## Release Branches

For a single component release, use this branch name:

```text
release/<component>-<version>
```

For nested components, use the component path without the leading colon and replace `:` with `-`.

Examples:

```bash
git switch -c release/components-bom-1.0.0
git switch -c release/components-feature-sync-1.0.0
```

For a coordinated release of multiple components, use a dated release-train branch name instead of encoding every
component and version in the branch name:

```text
release/tmc-YYYY-MM-DD
```

Example:

```bash
git switch -c release/tmc-2026-06-23
```

List the exact component versions in the release pull request description.

Validation regex:

```text
^release\/([a-zA-Z-]+-\d+\.\d+\.\d+|tmc-\d{4}-\d{2}-\d{2}(-\d+)?)$
```

## Release Publishing

Publish a release only after the release pull request has merged into `main`.

The release tag must be created from the merged release commit. The tag format is:

```text
<component>-<version>
```

Example:

```text
<component>-1.0.0
```

Trigger the `Publish Release` workflow from `main` and provide the component path, for example
`:components:bom`.

The workflow creates the release tag locally, writes GitHub Release notes from the finalized component changelog,
publishes the component to Maven Central, then pushes the tag and creates the GitHub Release after publishing succeeds.

For a coordinated release of multiple components, run the `Publish Release` workflow once per component from the same
merged `main` commit. Each run creates the component-specific tag, GitHub Release, and Maven Central publication for
that component.

The workflow performs these Gradle steps:

```bash
./gradlew <component-path>:createReleaseTag
./gradlew <component-path>:writeReleaseNotes
./gradlew <component-path>:validateStableVersionForPublishing <component-path>:publishAndReleaseToMavenCentral
```

For local verification before publishing to Maven Central, publish to Maven Local instead:

```bash
./gradlew <component-path>:validateStableVersionForPublishing <component-path>:publishToMavenLocal
```

Before publishing, verify:

- The release pull request has been merged.
- The workflow is started from `main`.
- The component path input points at the component being released.
- For coordinated releases, each component workflow run uses the same merged `main` commit.

## Post-release Version Bump

After publishing a release, bump the component `version.properties` to the next patch version in a separate commit or
pull request:

```bash
./gradlew <component-path>:versionBumpPatch
```

If the next release later needs to become a minor or major release, promote the development version with the matching
version bump task before that release is prepared.

The version file stores only the base semantic version. Snapshot state is not persisted in `version.properties`;
non-release builds derive their snapshot version from the fact that the current commit is not tagged with the
component release tag. For example, after bumping from `1.0.0` to `1.0.1`, `printVersion` reports `1.0.1-SNAPSHOT`
until the matching release tag is created on a future release commit.

This post-release bump can be automated after a successful publish, but it should still be committed separately from
the release commit so the release tag continues to point at the exact released version.

## Snapshot Publishing

Snapshots are published from an untagged `main` commit. Do not create a release pull request, do not finalize the
changelog, and do not create a release tag for a snapshot.

The `Publish Snapshot` workflow is triggered manually from `main` and publishes all publishable components.

The workflow skips publishing when the mutable `snapshot/latest` marker tag already points at the current `main`
commit. After a successful publish, the workflow moves `snapshot/latest` to the published commit.

The workflow performs these Gradle steps:

```bash
./gradlew validateSnapshotVersionForPublishing
./gradlew publishToMavenCentral
```

For local verification, publish to Maven Local instead:

```bash
./gradlew validateSnapshotVersionForPublishing
./gradlew publishToMavenLocal
```

Before publishing a snapshot, verify:

- The job runs from the intended `main` commit.
- The commit is not tagged with the matching component release tag.

