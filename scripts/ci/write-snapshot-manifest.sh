#!/usr/bin/env bash
set -euo pipefail

output="${1:-/dev/stdout}"
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

while IFS= read -r build_file; do
  project_dir="${build_file%/build.gradle.kts}"
  project_path=":${project_dir//\//:}"
  artifact_id="${project_dir##*/}"
  parent_path="${project_dir%/*}"
  group_id="net.thunderbird.${parent_path//\//.}"

  version_dir="${project_dir}"
  version_file=""
  while [[ "${version_dir}" != "." && "${version_dir}" != "/" ]]; do
    if [[ -f "${version_dir}/version.properties" ]]; then
      version_file="${version_dir}/version.properties"
      break
    fi
    version_dir="$(dirname "${version_dir}")"
  done

  if [[ -z "${version_file}" ]]; then
    echo "No version.properties found for ${project_path}" >&2
    exit 1
  fi

  # shellcheck disable=SC1090
  source "${version_file}"
  version="${MAJOR}.${MINOR}.${PATCH}-SNAPSHOT"

  metadata_path="${group_id//.//}/${artifact_id}/${version}/maven-metadata.xml"
  metadata_url="${repository_url%/}/${metadata_path}"
  echo "| \`${project_path}\` | [\`${group_id}:${artifact_id}:${version}\`](${metadata_url}) |" >> "${output}"
done < <(find components -name build.gradle.kts -print | sort)
