#!/usr/bin/env bash
set -euo pipefail

# Usage: write-snapshot-manifest.sh <release groups file> [output]
# The release groups file is the output of `./gradlew -q :components:bom:printReleaseGroups`.
groups_file="$1"
output="${2:-/dev/stdout}"
repository_url="${SNAPSHOT_REPOSITORY_URL:-https://central.sonatype.com/repository/maven-snapshots/}"
commit="${GITHUB_SHA:-$(git rev-parse HEAD)}"

{
  echo "# Published snapshot"
  echo
  echo "- Commit: ${commit}"
  echo "- Repository: ${repository_url}"
  echo
  echo "| Gradle path | Coordinates |"
  echo "| --- | --- |"
} > "${output}"

while read -r tag _ paths; do
  version="${tag##*-}-SNAPSHOT"
  for project_path in ${paths}; do
    artifact_id="${project_path##*:}"
    parent_path="${project_path%:*}"
    group_id="net.thunderbird${parent_path//:/.}"
    metadata_url="${repository_url%/}/${group_id//.//}/${artifact_id}/${version}/maven-metadata.xml"
    echo "| \`${project_path}\` | [\`${group_id}:${artifact_id}:${version}\`](${metadata_url}) |" >> "${output}"
  done
done < "${groups_file}"
