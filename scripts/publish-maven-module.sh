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
