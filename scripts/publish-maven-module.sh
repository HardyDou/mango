#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
MAVEN_ROOT="${REPO_ROOT}/mango"

usage() {
  cat <<'EOF'
Usage: scripts/publish-maven-module.sh <artifactId|module-path> [options]

Options:
  --also-make       Also build and deploy required upstream reactor modules
  --run-tests       Run tests; default is -DskipTests
  --skip-verify     Skip clean-repository dependency:get verification after deploy
  --dry-run         Print the Maven command without running it
  -h, --help        Show help

Examples:
  scripts/publish-maven-module.sh mango-file-api
  scripts/publish-maven-module.sh :mango-file-api
  scripts/publish-maven-module.sh mango-platform/mango-file/mango-file-api
  scripts/publish-maven-module.sh mango-file-core --also-make
EOF
}

if [[ $# -eq 0 ]]; then
  usage
  exit 1
fi

target=""
also_make=false
skip_tests=true
dry_run=false
verify_publish=true

while [[ $# -gt 0 ]]; do
  case "$1" in
    --also-make)
      also_make=true
      ;;
    --run-tests)
      skip_tests=false
      ;;
    --dry-run)
      dry_run=true
      ;;
    --skip-verify)
      verify_publish=false
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    -*)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
    *)
      if [[ -n "${target}" ]]; then
        echo "Only one module target is allowed." >&2
        usage
        exit 1
      fi
      target="$1"
      ;;
  esac
  shift
done

if [[ -z "${target}" ]]; then
  echo "Missing module target." >&2
  usage
  exit 1
fi

project_list="${target}"
if [[ "${target}" != :* && ! -d "${MAVEN_ROOT}/${target}" ]]; then
  project_list=":${target}"
fi

mvn_args=(-pl "${project_list}")
if [[ "${also_make}" == "true" ]]; then
  mvn_args+=(-am)
fi
mvn_args+=(deploy)
if [[ "${skip_tests}" == "true" ]]; then
  mvn_args+=(-DskipTests)
fi

echo "Maven root: ${MAVEN_ROOT}"
echo "Publishing module: ${project_list}"
if [[ "${also_make}" == "true" ]]; then
  echo "Mode: deploy selected module and required upstream modules"
else
  echo "Mode: deploy selected module only"
fi
printf 'Command: mvn'
printf ' %q' "${mvn_args[@]}"
printf '\n'

if [[ "${dry_run}" == "true" ]]; then
  exit 0
fi

cd "${MAVEN_ROOT}"
mvn "${mvn_args[@]}"

if [[ "${verify_publish}" == "true" ]]; then
  module_dir=""
  if [[ "${target}" != :* && -d "${MAVEN_ROOT}/${target}" ]]; then
    module_dir="${MAVEN_ROOT}/${target}"
  else
    artifact_id="${target#:}"
    while IFS= read -r -d '' pom_file; do
      pom_artifact_id="$(mvn -q -f "${pom_file}" help:evaluate -Dexpression=project.artifactId -DforceStdout)"
      if [[ "${pom_artifact_id}" == "${artifact_id}" ]]; then
        module_dir="$(dirname "${pom_file}")"
        break
      fi
    done < <(find "${MAVEN_ROOT}" -name pom.xml -not -path '*/target/*' -print0)
  fi
  if [[ -z "${module_dir}" || ! -f "${module_dir}/pom.xml" ]]; then
    echo "Unable to resolve module directory for publish verification: ${target}" >&2
    exit 1
  fi
  group_id="$(mvn -q -f "${module_dir}/pom.xml" help:evaluate -Dexpression=project.groupId -DforceStdout)"
  artifact_id="$(mvn -q -f "${module_dir}/pom.xml" help:evaluate -Dexpression=project.artifactId -DforceStdout)"
  version="$(mvn -q -f "${module_dir}/pom.xml" help:evaluate -Dexpression=project.version -DforceStdout)"
  packaging="$(mvn -q -f "${module_dir}/pom.xml" help:evaluate -Dexpression=project.packaging -DforceStdout)"
  verify_repo="${REPO_ROOT}/.runtime/maven-publish-verify-${artifact_id}"
  rm -rf "${verify_repo}"
  mkdir -p "${verify_repo}"
  echo "Verifying published Maven artifact: ${group_id}:${artifact_id}:${version}"
  mvn -U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get \
    -Dmaven.repo.local="${verify_repo}" \
    -Dartifact="${group_id}:${artifact_id}:${version}" \
    -Dtransitive=false
  if [[ "${packaging}" == "jar" ]]; then
    artifact_path="$(find "${verify_repo}" -path "*/${artifact_id}/${version}/*.jar" -print | sort | tail -n 1)"
    if [[ -z "${artifact_path}" || ! -f "${artifact_path}" ]]; then
      echo "Published Maven jar was not downloaded for ${group_id}:${artifact_id}:${version}" >&2
      exit 1
    fi
    if [[ -f "${module_dir}/src/main/resources/META-INF/mango/resource-manifest.json" ]]; then
      if ! jar tf "${artifact_path}" | grep -q '^META-INF/mango/resource-manifest.json$'; then
        echo "Published Maven jar is missing META-INF/mango/resource-manifest.json: ${artifact_path}" >&2
        exit 1
      fi
    fi
  fi
fi
