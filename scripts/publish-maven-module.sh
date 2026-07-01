#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
MAVEN_ROOT="${REPO_ROOT}/mango"

usage() {
  cat <<'EOF'
Usage: scripts/publish-maven-module.sh <artifactId|module-path> [options]

Use scripts/publish-maven-batch.sh for release batches that publish more than
one Maven artifact. This single-module helper is for one-off module publication.

Options:
  --also-make       Also build and deploy required upstream reactor modules
  --revision <ver>  Maven CI-friendly version; required unless MANGO_MAVEN_REVISION is set
  --release-version <ver>
                    Alias for --revision
  --allow-snapshot  Allow an explicit *-SNAPSHOT revision
  --run-tests       Run tests; default is -DskipTests
  --skip-verify     Skip clean-repository dependency:get verification after deploy
  --dry-run         Print the Maven command without running it
  -h, --help        Show help

Examples:
  scripts/publish-maven-module.sh mango-file-api --release-version 1.0.2
  scripts/publish-maven-module.sh :mango-file-api --release-version 1.0.2-rc.20250701113000
  scripts/publish-maven-module.sh mango-platform/mango-file/mango-file-api --release-version 1.0.2
  scripts/publish-maven-module.sh mango-file-core --also-make --release-version 1.0.2
  scripts/publish-maven-module.sh mango-platform/mango-payment/mango-payment-api --also-make --revision 1.0.2-SNAPSHOT --allow-snapshot
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
allow_snapshot=false
revision="${MANGO_MAVEN_REVISION:-}"

validate_revision() {
  local value="$1"
  if [[ -z "${value}" ]]; then
    echo "Maven revision is required. Pass --release-version <version> or set MANGO_MAVEN_REVISION." >&2
    echo "Examples: 1.0.2, 1.0.2-rc.20250701113000, 1.0.2-dev.20250701113000" >&2
    exit 1
  fi
  if [[ ! "${value}" =~ ^[0-9]+(\.[0-9]+){2,}([-+][0-9A-Za-z][0-9A-Za-z.-]*)?$ ]]; then
    echo "Invalid Maven revision: ${value}" >&2
    echo "Use a stable SemVer-like version such as 1.0.2 or an explicit prerelease such as 1.0.2-rc.20250701113000." >&2
    exit 1
  fi
  if [[ "${value}" == *-SNAPSHOT && "${allow_snapshot}" != "true" ]]; then
    echo "SNAPSHOT publish is blocked by default: ${value}" >&2
    echo "Use a release/prerelease version, or pass --allow-snapshot when publishing an intentional Maven snapshot." >&2
    exit 1
  fi
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --also-make)
      also_make=true
      ;;
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
    --allow-snapshot)
      allow_snapshot=true
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
validate_revision "${revision}"

project_list="${target}"
if [[ "${target}" != :* && ! -d "${MAVEN_ROOT}/${target}" ]]; then
  project_list=":${target}"
fi

mvn_args=(-pl "${project_list}")
if [[ "${also_make}" == "true" ]]; then
  mvn_args+=(-am)
fi
mvn_args+=(deploy)
mvn_args+=("-Drevision=${revision}")
if [[ "${skip_tests}" == "true" ]]; then
  mvn_args+=(-DskipTests)
fi

echo "Maven root: ${MAVEN_ROOT}"
echo "Publishing module: ${project_list}"
echo "Revision: ${revision}"
echo "Allow SNAPSHOT: ${allow_snapshot}"
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
  mvn_eval() {
    local pom_file="$1"
    local expression="$2"
    mvn -q -f "${pom_file}" help:evaluate "-Drevision=${revision}" -Dexpression="${expression}" -DforceStdout
  }

  module_dir=""
  if [[ "${target}" != :* && -d "${MAVEN_ROOT}/${target}" ]]; then
    module_dir="${MAVEN_ROOT}/${target}"
  else
    artifact_id="${target#:}"
    while IFS= read -r -d '' pom_file; do
      pom_artifact_id="$(mvn_eval "${pom_file}" project.artifactId)"
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
  group_id="$(mvn_eval "${module_dir}/pom.xml" project.groupId)"
  artifact_id="$(mvn_eval "${module_dir}/pom.xml" project.artifactId)"
  version="$(mvn_eval "${module_dir}/pom.xml" project.version)"
  packaging="$(mvn_eval "${module_dir}/pom.xml" project.packaging)"
  verify_repo="${REPO_ROOT}/.runtime/maven-publish-verify-${artifact_id}"
  rm -rf "${verify_repo}"
  mkdir -p "${verify_repo}"
  artifact_coordinates="${group_id}:${artifact_id}:${version}"
  if [[ "${packaging}" == "pom" ]]; then
    artifact_coordinates="${artifact_coordinates}:pom"
  fi
  echo "Verifying published Maven artifact: ${artifact_coordinates}"
  mvn -U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get \
    -Dmaven.repo.local="${verify_repo}" \
    -Dartifact="${artifact_coordinates}" \
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
