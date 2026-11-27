# Contributing

Welcome to the Thunderbird Mobile Components project! We're excited to have you here and welcome
your contributions.

## Getting Started

Before you start contributing, please take a moment to familiarize yourself with the following:

- **Mozilla Community Participation Guidelines:** [https://www.mozilla.org/en-US/about/governance/policies/participation/](https://www.mozilla.org/en-US/about/governance/policies/participation/)

### Git Commit Guide

We follow [Conventional Commits](https://www.conventionalcommits.org/). Please read our
[Git Commit Guide](docs/commit-guide.md) for examples, allowed types, scopes, and best practices.

### Release Guide

Maintainers and release owners should read the [Release Guide](docs/release-guide.md) for the
release-preparation workflow, changelog generation, and tagging conventions.

## Bug Reports and Feature Requests

If you encounter a bug or have a feature request, please follow these steps:

- Search the [existing issues](https://github.com/thunderbird/thunderbird-mobile-components/issues?q=is%3Aissue) to see if your issue or feature has already been reported.
- If you can't find an existing issue, please [open a new issue](https://github.com/thunderbird/thunderbird-mobile-components/issues/new/choose) on GitHub.

## Translations

English source strings are maintained in this repository. Translations are managed through Weblate and stored in the
[localization repository](https://github.com/thunderbird/thunderbird-mobile-components-l10n).

After source changes merge, maintainers run the localization repository's
[Import from Source workflow](https://github.com/thunderbird/thunderbird-mobile-components-l10n/actions/workflows/l10n-sync-from-source.yml)
and review and merge its import pull request. The import records the files and keys belonging to `main` in
`l10n-sync-manifest.json`.

The [Translation Update workflow](https://github.com/thunderbird/thunderbird-mobile-components/actions/workflows/l10n-translation-update.yml)
runs weekly or manually. It uses the localization repository's released `scripts/l10n-sync` launcher to export only
translations for `main`, without changing English source files, and opens a pull request. The pull request records the
localization revision and includes contributor attribution in `Co-authored-by` trailers.

Translation edits should be made through Weblate, not directly in this source repository. For tooling setup and
component management, see the localization repository's README.

## Contributing Code

> [!IMPORTANT]
> The Thunderbird Mobile Components project is currently in the early stages of development.
> We welcome contributions, but please be aware that the codebase is subject to change and may
> not be fully stable yet.

Thank you for your willingness to contribute code! Here's how you can get started:

**1. Find an issue:**

- Check the issue tracker for [open issues](https://github.com/thunderbird/thunderbird-mobile-components/issues?q=is%3Aissue+is%3Aopen+-label%3Aunconfirmed+-label%3Atb-team).
- Look for issues labeled [good first issue](https://github.com/thunderbird/thunderbird-mobile-components/labels/good%20first%20issue) for a good starting point.
- Propose a new feature by [opening a new issue](https://github.com/thunderbird/thunderbird-mobile-components/issues/new/choose)
- Avoid issues labeled [unconfirmed](https://github.com/thunderbird/thunderbird-mobile-components/labels/unconfirmed) or [tb-team](https://github.com/thunderbird/thunderbird-mobile-components/labels/tb-team) as they are not yet ready for contributions.

**2. Discuss your plan:**

- Leave a comment on the issue you want to work on, explaining what you plan to do. This helps avoid duplicate work and gets you feedback from the team.

**3. Fork the repository:**

- Create your own [fork](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/fork-a-repo) of the Thunderbird Mobile Components repository on GitHub.

**4. Create a branch:**

- Start a new branch from the `main` branch to keep your changes separate.
- Name your branch descriptively (e.g., `fix-issue-123` or `add-feature-xyz`).

**5. Make your changes:**

- Write your code and commit it to your branch.

**6. Test your changes:**

- Run the project's tests to make sure everything works and that your changes don't introduce any regressions.
- If applicable, write new tests to cover your changes.

**7. Push your changes:**

- Upload your branch to your forked repository.

**8. Open a pull request:**

- Create a pull request to merge your changes into the main project.
- Provide a clear and concise description of your changes, including:
  - A reference to the issue you're addressing.
  - A summary of the changes you made.
  - Any relevant screenshots or testing results.

