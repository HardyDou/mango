#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
MAVEN_ROOT="${REPO_ROOT}/mango"

usage() {
  cat <<'EOF'
Usage: scripts/publish-maven-batch.sh <artifactId|module-path>... [options]

Publish multiple Maven reactor modules in one deploy command and verify the
published artifacts with one shared temporary Maven local repository.

Options:
  --revision <ver>      Maven CI-friendly version; default is 1.0.0-SNAPSHOT
  --release-version <ver>
                        Alias for --revision; publish a non-SNAPSHOT release version
  --run-tests           Run tests; default is -DskipTests
  --skip-verify         Skip dependency:get verification after deploy
  --verify-transitive   Resolve transitive dependencies during verification
  --verify-repo <path>  Shared verification local repo; default is
                        .runtime/maven-publish-verify-batch
  --dry-run             Print commands without running them
  -h, --help            Show help

Examples:
  scripts/publish-maven-batch.sh mango-auth-starter mango-auth-starter-remote
  scripts/publish-maven-batch.sh :mango-cms-starter :mango-cms-starter-remote
  scripts/publish-maven-batch.sh mango-platform/mango-cms/mango-cms-starter --revision 1.0.0
EOF
}

targets=()
skip_tests=true
dry_run=false
verify_publish=true
verify_transitive=false
revision="${MANGO_MAVEN_REVISION:-1.0.0-SNAPSHOT}"
verify_repo="${MANGO_MAVEN_VERIFY_REPO:-${REPO_ROOT}/.runtime/maven-publish-verify-batch}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --run-tests)
      skip_tests=false
      ;;
    --revision)
      if [[ $# -lt 2 || "$2" == -* ]]; then
        echo "Missing value for --revision." >&2
        usage
        exit 1
      fi
      revision="$2"
      shift
      ;;
    --revision=*)
      revision="${1#--revision=}"
      ;;
    --release-version)
      if [[ $# -lt 2 || "$2" == -* ]]; then
        echo "Missing value for --release-version." >&2
        usage
        exit 1
      fi
      revision="$2"
      shift
      ;;
    --release-version=*)
      revision="${1#--release-version=}"
      ;;
    --skip-verify)
      verify_publish=false
      ;;
    --verify-transitive)
      verify_transitive=true
      ;;
    --verify-repo)
      if [[ $# -lt 2 || "$2" == -* ]]; then
        echo "Missing value for --verify-repo." >&2
        usage
        exit 1
      fi
      verify_repo="$2"
      shift
      ;;
    --verify-repo=*)
      verify_repo="${1#--verify-repo=}"
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
      targets+=("$1")
      ;;
  esac
  shift
done

if [[ ${#targets[@]} -eq 0 ]]; then
  echo "Missing Maven module targets." >&2
  usage
  exit 1
fi
if [[ -z "${revision}" ]]; then
  echo "Revision must not be empty." >&2
  exit 1
fi

mvn_eval() {
  local pom_file="$1"
  local expression="$2"
  mvn -q -f "${pom_file}" help:evaluate "-Drevision=${revision}" -Dexpression="${expression}" -DforceStdout
}

normalize_project() {
  local target="$1"
  if [[ "${target}" == :* || -d "${MAVEN_ROOT}/${target}" ]]; then
    printf '%s' "${target}"
  else
    printf ':%s' "${target}"
  fi
}

resolve_module_dir() {
  local target="$1"
  local project basedir
  if [[ "${target}" != :* && -d "${MAVEN_ROOT}/${target}" ]]; then
    printf '%s' "${MAVEN_ROOT}/${target}"
    return 0
  fi
  project="$(normalize_project "${target}")"
  basedir="$(
    cd "${MAVEN_ROOT}"
    mvn -q -pl "${project}" help:evaluate "-Drevision=${revision}" -Dexpression=project.basedir -DforceStdout
  )"
  basedir="$(printf '%s\n' "${basedir}" | tail -n 1)"
  if [[ -n "${basedir}" && -f "${basedir}/pom.xml" ]]; then
    printf '%s' "${basedir}"
    return 0
  fi
  return 1
}

project_list=""
for target in "${targets[@]}"; do
  project="$(normalize_project "${target}")"
  if [[ -z "${project_list}" ]]; then
    project_list="${project}"
  else
    project_list="${project_list},${project}"
  fi
done

mvn_args=(-pl "${project_list}" -am deploy "-Drevision=${revision}")
if [[ "${skip_tests}" == "true" ]]; then
  mvn_args+=(-DskipTests)
fi

echo "Maven root: ${MAVEN_ROOT}"
echo "Publishing modules: ${project_list}"
echo "Revision: ${revision}"
echo "Mode: one reactor deploy for all selected modules and required upstream modules"
printf 'Command: mvn'
printf ' %q' "${mvn_args[@]}"
printf '\n'

if [[ "${dry_run}" == "false" ]]; then
  cd "${MAVEN_ROOT}"
  mvn "${mvn_args[@]}"
fi

if [[ "${verify_publish}" != "true" ]]; then
  exit 0
fi

verify_args=(-U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get "-Dmaven.repo.local=${verify_repo}")
if [[ "${verify_transitive}" == "true" ]]; then
  verify_args+=(-Dtransitive=true)
else
  verify_args+=(-Dtransitive=false)
fi

echo "Verification local repository: ${verify_repo}"
if [[ "${dry_run}" == "false" ]]; then
  rm -rf "${verify_repo}"
  mkdir -p "${verify_repo}"
fi

for target in "${targets[@]}"; do
  module_dir="$(resolve_module_dir "${target}")" || {
    echo "Unable to resolve module directory for publish verification: ${target}" >&2
    exit 1
  }
  group_id="$(mvn_eval "${module_dir}/pom.xml" project.groupId)"
  artifact_id="$(mvn_eval "${module_dir}/pom.xml" project.artifactId)"
  version="$(mvn_eval "${module_dir}/pom.xml" project.version)"
  packaging="$(mvn_eval "${module_dir}/pom.xml" project.packaging)"
  artifact_coordinates="${group_id}:${artifact_id}:${version}"
  if [[ "${packaging}" == "pom" ]]; then
    artifact_coordinates="${artifact_coordinates}:pom"
  fi

  echo "Verifying published Maven artifact: ${artifact_coordinates}"
  printf 'Command: mvn'
  printf ' %q' "${verify_args[@]}"
  printf ' %q' "-Dartifact=${artifact_coordinates}"
  printf '\n'

  if [[ "${dry_run}" == "true" ]]; then
    continue
  fi

  mvn "${verify_args[@]}" "-Dartifact=${artifact_coordinates}"
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
done
