# Import Code CLI (`import-code`)

CLI tool to import code from upstream repositories (such as `thunderbird-android`) into `thunderbird-mobile-components`, automatically verifying code authorship against configured safe author rules, excluding project-specific build configurations (`build.gradle.kts`) by default, and generating the standardized git transfer commit message snippet.

## Audit Scope & Exclusions

- **Scope**: Imports files present at the selected revision within `--source-path`.
- **Exclusions**: Build configuration files (`build.gradle.kts` at any directory depth) are excluded by default as project-specific configuration (use `--include-build-gradle` to override).
- **Authorship Verification**: Checks authorship of candidate files against `config/safe-authors.json` (configured approval rules), importing only files that meet safe author criteria while excluding any files with unapproved or unverified contributors (use `--skip-authorship-check` to override and import all files).

## Workflow & Usage

The tool connects directly to the upstream GitHub repository by default (caching into `build/tmp/`).

### 1. Importing a Component from Upstream

To import a component from `thunderbird-android` into this repository:

```sh
# Import core/logging from upstream main into components/core/logging
./scripts/import-code -p core/logging -t components/core/logging

# Import at a specific commit SHA
./scripts/import-code -p core/logging -t components/core/logging -c 071fa9962c

# Remove cached repository after import
./scripts/import-code -p core/logging -t components/core/logging --cleanup-tmp
```

### 2. Dry-Run Mode

Preview what files will be imported without making any modifications on disk:

```sh
./scripts/import-code -p core/logging -t components/core/logging --dry-run
```

## Options

|              Option              |                                                  Description                                                   |
|----------------------------------|----------------------------------------------------------------------------------------------------------------|
| `-p, --source-path <path>`       | Subdirectory or file path within the source repository to import (e.g. `core/logging`). REQUIRED.              |
| `-t, -d, --target-path <path>`   | Destination directory path within this repository (e.g. `components/core/logging`). REQUIRED.                  |
| `-c, --source-commit <sha>`      | Source commit SHA or ref to inspect and import from (if omitted, auto-detected from latest commit).            |
| `--source-repo-url <url>`        | URL of the upstream source git repository (default: `https://github.com/thunderbird/thunderbird-android.git`). |
| `--include-build-gradle`         | Include `build.gradle.kts` files when importing (excluded by default as project configuration).                |
| `--skip-authorship-check`        | Skip safe authors verification before importing.                                                               |
| `-s, --safe-authors-file <file>` | Path to safe authors JSON configuration file (default: `config/safe-authors.json`).                            |
| `--cleanup-tmp`                  | Remove the cloned repository from `build/tmp/` after importing.                                                |
| `--dry-run`                      | Simulate the import operation without writing files.                                                           |
| `-h, --help`                     | Show the help message and exit.                                                                                |

