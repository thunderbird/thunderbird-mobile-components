#!/usr/bin/env bash
set -euo pipefail

# Usage: release-all.sh [--dry-run]
# --dry-run runs every Gradle step of the release (including the stable version check) but
# skips publishing, pushing tags, and creating GitHub Releases. Local tags it creates are
# deleted on exit. It can run from any branch.
dry_run=false
if [[ "${1:-}" == "--dry-run" ]]; then
  dry_run=true
elif [[ $# -gt 0 ]]; then
  echo "Usage: $0 [--dry-run]" >&2
  exit 1
fi

if [[ "$dry_run" != true && "${GITHUB_REF_NAME:-}" != "main" ]]; then
  echo "Release all must run from main." >&2
  exit 1
fi

work_dir="${RUNNER_TEMP:-/tmp}"
groups_file="${work_dir}/release-groups.txt"
pending_file="${work_dir}/release-pending.txt"
: > "$pending_file"

if [[ "$dry_run" == true ]]; then
  delete_local_tags() {
    while read -r tag _; do
      git tag -d "$tag" >/dev/null 2>&1 || true
    done < "$pending_file"
  }
  trap delete_local_tags EXIT
fi

# One line per version group: <tag> <version dir> <project path>..., BOM last.
./gradlew :components:bom:printReleaseGroups -PreleaseGroupsFile="$groups_file"

# Preflight every group before publishing anything. A group whose tag already exists is
# skipped if nothing in its version dir changed since; otherwise its version must be bumped.
# Pending groups are tagged locally, because a project only resolves to its release version
# when HEAD carries its tag. Tags are pushed only after the group is published.
while read -r tag version_dir paths <&3; do
  read -r -a projects <<< "$paths"
  notes_file="${work_dir}/release-notes-${tag}.md"

  if git rev-parse -q --verify "refs/tags/${tag}" >/dev/null; then
    if ! git merge-base --is-ancestor "$tag" HEAD || ! git diff --quiet "$tag" HEAD -- "$version_dir"; then
      echo "${version_dir} changed since ${tag}; bump its version before releasing." >&2
      exit 1
    fi
    if ! gh release view "$tag" >/dev/null 2>&1; then
      ./gradlew "${projects[0]}:writeReleaseNotes" -PreleaseNotesFile="$notes_file"
      if [[ "$dry_run" == true ]]; then
        echo "Dry run: would create missing GitHub Release ${tag}."
      else
        gh release create "$tag" --title "$tag" --verify-tag --notes-file "$notes_file"
      fi
    fi
    echo "${tag} already released; skipping."
    continue
  fi

  ./gradlew "${projects[0]}:writeReleaseNotes" -PreleaseNotesFile="$notes_file" "${projects[0]}:createReleaseTag"
  echo "$tag $version_dir $paths" >> "$pending_file"
done 3< "$groups_file"

if [[ ! -s "$pending_file" ]]; then
  echo "All groups already released."
  exit 0
fi

read -r -a pending_projects <<< "$(cut -d ' ' -f 3- "$pending_file" | tr '\n' ' ')"
./gradlew "${pending_projects[@]/%/:validateStableVersionForPublishing}"

if [[ "$dry_run" == true ]]; then
  echo "Dry run passed. Would publish, push the tag, and create the GitHub Release for:"
  cut -d ' ' -f 1 "$pending_file"
  exit 0
fi

# Maven Central rejects versions that already exist, so a rerun cannot republish a group.
# Push each tag right after publishing its group so a rerun skips it.
while read -r tag version_dir paths <&3; do
  read -r -a projects <<< "$paths"
  ./gradlew "${projects[@]/%/:publishAndReleaseToMavenCentral}"
  git push origin "refs/tags/${tag}"
  gh release create "$tag" --title "$tag" --verify-tag \
    --notes-file "${work_dir}/release-notes-${tag}.md"
done 3< "$pending_file"
