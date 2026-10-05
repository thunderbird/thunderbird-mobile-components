# Authorship CLI (`check-authorship`)

A command-line tool to verify git code authorship against configured approval rules when extracting and adapting code from [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android) (`thunderbird-android`) into Thunderbird Mobile Components (`thunderbird-mobile-components`).

## Audit Scope & Exclusions

- **Audit Scope**: The audit inspects all tracked files present at the selected revision within the requested `--source-path`.
- **Excluded Files**: Build configuration files (`build.gradle.kts` at any directory depth) are excluded from file enumeration and history analysis, as they represent project-specific build configurations. Excluded configuration files are not audited.
- **Deletions**: Files deleted prior to the inspected revision and historical contributions removed before that revision are not in scope.
- **Renames**: Historical commit contributions for audited files are tracked across renames.
- **Approval Matching**: Contributor matching uses the pinned approval rules referenced by `config/safe-authors-reference.json` and represents configured approvals.

## Workflow & Usage

The tool connects directly to the upstream GitHub repository by default, automatically caching the repository in a temporary `build/tmp/` folder at the repository root. When no `--source-commit` is specified, it updates to the latest upstream default branch.

### 1. Auditing Upstream Source Code (Default)

To verify the authorship of a component in `thunderbird-android`, specify the source path within the repository:

```sh
# Verify authorship of core/logging from the latest upstream main branch
./scripts/check-authorship -p core/logging

# Verify authorship at a specific source commit SHA
./scripts/check-authorship -p core/logging -c 071fa9962c

# Save a full Markdown audit report to a file
./scripts/check-authorship -p core/logging -o report.md
```

If any commit or line was authored by an unapproved contributor, the tool highlights the unapproved authors and exits with status code `1`.

### 2. Generating the Git Transfer Commit Snippet (`--commit-msg-only`)

To generate the standardized provenance attribution snippet for your migration git commit message:

```sh
./scripts/check-authorship -p core/logging -c 071fa9962c --commit-msg-only
```

Outputs:

```text
Extracted from https://github.com/thunderbird/thunderbird-android.
Authorship matches configured safe author rules
Source: https://github.com/thunderbird/thunderbird-android/tree/071fa9962c/core/logging
```

## Options

|              Option              |                                                      Description                                                      |
|----------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `-p, --source-path <path>`       | Subdirectory or file path(s) within the repository to inspect (can be specified multiple times).                      |
| `-c, --source-commit <sha>`      | Source commit SHA or ref to inspect (if not specified, auto-detected from repository HEAD/main).                      |
| `--source-repo-url <url>`        | URL of the upstream source git repository (default: `https://github.com/thunderbird/thunderbird-android.git`).        |
| `-s, --safe-authors-file <file>` | Path to a pinned private-repository reference or local approval JSON (default: `config/safe-authors-reference.json`). |
| `-o, --output <file>`            | Output file path to save the generated Markdown report.                                                               |
| `--commit-msg-only`              | Print only the git commit message snippet.                                                                            |
| `--json`                         | Output the analysis as JSON.                                                                                          |
| `--cleanup-tmp`                  | Remove the cloned repository from `build/tmp/` after execution.                                                       |
| `-h, --help`                     | Show the help message and exit.                                                                                       |

## Safe Authors Configuration

The committed `config/safe-authors-reference.json` contains `repoUrl` and a full, pinned Git commit `revision`. The tool fetches that revision and reads `safe-authors.json` from the private repository. Access requires Git credentials; if fetching fails, authorship checking fails rather than approving any contributors. Update the pinned revision when the private approvals change.

The private `safe-authors.json` contains `safe_domains` (approved email domains) and `safe_authors` (approved names, aliases, emails and affiliations). You can alternatively supply a local approval JSON with `--safe-authors-file`. Do not commit that file or expose approval details in logs or reports unintentionally.

