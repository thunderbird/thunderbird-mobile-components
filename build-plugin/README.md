# Build plugins

The `build-plugin` project provides a set of Gradle plugins that act as the single source of truth for
project-wide build configuration. This avoids duplicated build script setups and centralizes common build logic.

## Background

We use an included build to host our build logic as real Gradle plugins (written in Kotlin), not as legacy
`*.gradle.kts` convention scripts. The included build is located at `build-plugin/plugin` and registers plugins
via the Gradle `gradlePlugin` block in `build-plugin/plugin/build.gradle.kts`.

You apply these plugins in modules using their fully-qualified plugin IDs. Each plugin focuses on a single
responsibility; one-off configuration should stay in the module’s own `build.gradle.kts`.

## Available plugins

- `net.thunderbird.gradle.plugin.app.kmp.compose` — Configures common options for Kotlin Multiplatform Compose apps
- `net.thunderbird.gradle.plugin.library.kmp` — Configures common options for Kotlin Multiplatform libraries
- `net.thunderbird.gradle.plugin.library.kmp.compose` — Configures common options for KMP Compose libraries

Supportive/quality and tooling plugins:

- `net.thunderbird.gradle.plugin.dependency.check` — [Gradle Versions Plugin](https://github.com/ben-manes/gradle-versions-plugin)
  - Run `./gradlew dependencyUpdates` to generate a dependency update report
- `net.thunderbird.gradle.plugin.quality.detekt` — [Detekt](https://detekt.dev/) static analysis for Kotlin
  - Run `./gradlew detekt` to analyze, and `./gradlew detektBaseline` if you can’t fix issues yet
- `net.thunderbird.gradle.plugin.quality.spotless` — [Spotless](https://github.com/diffplug/spotless) with
  [Ktlint](https://pinterest.github.io/ktlint/) for formatting
  - Run `./gradlew spotlessCheck` to verify and `./gradlew spotlessApply` to format
- `net.thunderbird.gradle.plugin.quality.coverage` — Configures [Kover](https://github.com/Kotlin/kotlinx-kover)
  code coverage with sensible defaults
  - Common tasks: `./gradlew koverHtmlReport`, `./gradlew koverXmlReport`, `./gradlew koverVerify`
- `net.thunderbird.gradle.plugin.publishing` — Configures publishing (Maven coordinates, POM metadata, and common
  repositories like `mavenLocal()` and a local build repo under `build/maven-repo`)
- `net.thunderbird.gradle.plugin.versioning` — Reads and updates component versions from the nearest
  `version.properties`.
  - Version resolution: builds from a commit tagged with `<component>-<version>` use `<version>`;
    all other builds use `<version>-SNAPSHOT`.
  - Tasks:
    - `versionBumpMajor`, `versionBumpMinor`, `versionBumpPatch`: update the nearest `version.properties`.
    - `printVersion`: prints the version resolved from the nearest `version.properties`.
    - `createReleaseTag`: creates the component release git tag from `version.properties`.
      - Format: `<component>-<version>` (for example `bom-1.0.0`)
      - Validation: fails for existing tags.
  - Release tag command:
    ```bash
    ./gradlew :components:bom:createReleaseTag
    ```

- `net.thunderbird.gradle.plugin.changelog` — Maintains component‑local `CHANGELOG.md` files next to each component’s
  nearest `version.properties` (modules that share the same nearest `version.properties` directory are grouped together).
  - Task:
    - `updateChangelog`: ensures a component‑local `CHANGELOG.md` exists and auto‑populates the
      `## Unreleased` section from Conventional Commit titles in git.
      - Scope: only commits that touch the component directory (relative to the repository root) are considered.
      - History: entries are read from first-parent history on `origin/main`/`main`, so pull request branch commits
        are ignored.
      - Release boundary: the task uses an exact component tag range based on the latest released changelog version
        (for example `bom-1.0.0..main`) and fails when the expected previous release tag does not exist.
      - Grouping: entries are grouped by type into sections (`Features`, `Bug Fixes`, `Documentation`, `Styles`,
        `Refactoring`, `Tests`, `Chores`, `Reverts`).
      - De‑duplication: existing bullets are preserved and duplicates from git subjects are avoided.
      - Workflow: run changelog updates during release preparation, not during normal builds. See the
        [Release Guide](../docs/release-guide.md).
    - `finalizeChangelog`: finalizes the current `## Unreleased` section for a release version and inserts a new
      empty `## Unreleased` section above it.
      - Version source: nearest `version.properties`
      - Optional property: `-PreleaseVersion=<version>` (must match `version.properties` when provided)
      - Optional property: `-PreleaseDate=YYYY-MM-DD` (defaults to the current local date)
      - Validation: fails when `Unreleased` is missing, empty, or when the target release already exists.
    - `writeReleaseNotes`: writes the finalized release section to a markdown file for GitHub Releases.
      - Version source: nearest `version.properties`
      - Optional property: `-PreleaseNotesFile=<path>` (defaults to `build/release/release-notes.md`)
  - Usage:
    ```kotlin
    // In any project that should contribute a component changelog
    plugins {
        id("net.thunderbird.gradle.plugin.changelog")
    }
    ```

    Command:
    ```bash
    # Run for a single component (example: BOM)
    ./gradlew :components:bom:updateChangelog

    # Finalize the changelog for a release
    ./gradlew :components:bom:finalizeChangelog
    ```

### Applying a plugin

In any module’s `build.gradle.kts`:

```
plugins {
    id("net.thunderbird.gradle.plugin.library.kmp")
    // or
    id("net.thunderbird.gradle.plugin.app.kmp.compose")
}
```

These IDs also work in the root build script when a plugin is designed to be applied at the root (for example,
`publishing` or top-level quality configurations):

```
plugins {
    id("net.thunderbird.gradle.plugin.publishing")
}
```

### Code coverage configuration

When using `net.thunderbird.gradle.plugin.quality.coverage` you can tune or disable coverage checks:

- Disable via Gradle property: `-PcodeCoverageDisabled=true`
- Disable via environment variable: `CODE_COVERAGE_DISABLED=true`

Configure thresholds in a module’s `build.gradle.kts` using the `codeCoverage` extension:

```
codeCoverage {
    disabled.set(false)
    lineCoverage.set(80)
    branchCoverage.set(70)
}
```

Reports and verification are provided by Kover (JaCoCo backend is pinned by the plugin).

### Publishing configuration

The `net.thunderbird.gradle.plugin.publishing` plugin:

- Applies the Vanniktech Maven Publish plugin
- Sets Maven coordinates from the project and configures POM metadata
- Adds local repositories: `mavenLocal()` and `${rootProject}/build/maven-repo`
- Configures publishing to Maven Central and signs all publications
- Adds release-oriented validation tasks:
    - `validateStableVersionForPublishing`: validates a non-`SNAPSHOT` version before release publishing.
    - `validateSnapshotVersionForPublishing`: validates a `SNAPSHOT` version before snapshot publishing.
- Adds release tag tasks:
    - `printReleaseTag`: prints the component release tag derived from `version.properties`.
    - `createReleaseTag`: creates the component release tag locally.

Signing properties can be supplied from a file at `${rootProject}/.signing/signing.properties` with keys:

- `signing.keyId`
- `signing.password`
- `signing.secretKeyRingFile`

Alternatively, you may provide equivalent Gradle properties via other supported mechanisms.

### Included build wiring

These plugins are provided by an included build. The root `settings.gradle.kts` contains:

```
includeBuild("build-plugin")
```

## Creating a new build plugin

1. Create a Kotlin plugin class under `build-plugin/plugin/src/main/kotlin/...`, implementing `Plugin<Project>`.
2. Register it in `build-plugin/plugin/build.gradle.kts` inside the `gradlePlugin { plugins { ... } }` block with a
   unique ID and the `implementationClass` pointing to your class.
3. Dependencies:
   - Add versions and aliases to the version catalog `gradle/libs.versions.toml` (if not present yet).
   - Add the dependency to `build-plugin/plugin/build.gradle.kts`.
     - Plugin dependency: `implementation(plugin(libs.plugins.YOUR_PLUGIN))`
     - Library dependency: `implementation(libs.YOUR_LIBRARY)`

After that, apply your plugin by its ID in any module’s `plugins` block.

## Acknowledgments

- [Herding Elephants | Square Corner Blog](https://developer.squareup.com/blog/herding-elephants/)
- [Idiomatic Gradle: How do I idiomatically structure a large build with Gradle](https://github.com/jjohannes/idiomatic-gradle#idiomatic-build-logic-structure)

