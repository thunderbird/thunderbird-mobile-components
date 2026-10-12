#!/usr/bin/env bash
set -euo pipefail

# Usage: publish-snapshot.sh [--dry-run]
# Publishes the BOM release train, the same projects as release-all.sh, as snapshots and writes
# the snapshot manifest to $RUNNER_TEMP/snapshot-manifest.md.
# --dry-run runs the same steps but skips publishing to Maven Central.
dry_run=false
if [[ "${1:-}" == "--dry-run" ]]; then
  dry_run=true
elif [[ $# -gt 0 ]]; then
  echo "Usage: $0 [--dry-run]" >&2
  exit 1
fi

work_dir="${RUNNER_TEMP:-/tmp}"
groups_file="${work_dir}/release-groups.txt"
manifest_file="${work_dir}/snapshot-manifest.md"

./gradlew :components:bom:printReleaseGroups -PreleaseGroupsFile="$groups_file"
read -r -a projects <<< "$(cut -d ' ' -f 3- "$groups_file" | tr '\n' ' ')"

./gradlew "${projects[@]/%/:validateSnapshotVersionForPublishing}"

if [[ "$dry_run" == true ]]; then
  echo "Dry run passed. Would publish snapshots for:"
  printf '%s\n' "${projects[@]}"
else
  ./gradlew "${projects[@]/%/:publishToMavenCentral}"
fi

scripts/ci/write-snapshot-manifest.sh "$groups_file" "$manifest_file"
