#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
MAVEN_ROOT="${REPO_ROOT}/mango"
ARCHITECTURE_VERIFICATION_MODULE="mango-tools/mango-architecture-verification"
ARCHITECTURE_VERIFICATION_DIR="${MAVEN_ROOT}/${ARCHITECTURE_VERIFICATION_MODULE}"

usage() {
  cat <<'EOF'
Usage: scripts/publish-maven-batch.sh <artifactId|module-path>... [options]
       scripts/publish-maven-batch.sh --all-non-app [options]

Publish multiple Maven reactor modules in one deploy command and verify the
published artifacts.

Options:
  --all-non-app        Publish the complete backend reactor excluding
                       mango-app/** deployment entry modules and internal
                       *-test modules
  --include-apps       Allow explicit mango-app/** targets. App artifacts are
                       never included by --all-non-app
  --revision <ver>      Maven CI-friendly version; required unless
                        MANGO_MAVEN_REVISION is set
  --release-version <ver>
                        Alias for --revision
  --allow-snapshot      Allow an explicit *-SNAPSHOT revision
  --run-tests           Run tests; default is -DskipTests
  --verify-only        Verify already published artifacts without running deploy
  --skip-verify         Skip artifact verification after deploy
  --verify-mode <mode>  Verification mode: http or maven; default is http
  --verify-base-url <url>
                        HTTP verification repository URL; required unless
                        MANGO_MAVEN_VERIFY_BASE_URL is set
  --verify-transitive   Resolve transitive dependencies during Maven verification
  --verify-repo <path>  Shared verification local repo; default is
                        .runtime/maven-publish-verify-batch
  --dry-run             Print commands without running them
  -h, --help            Show help

Examples:
  scripts/publish-maven-batch.sh --all-non-app --release-version 1.0.10
  scripts/publish-maven-batch.sh mango-auth-starter mango-auth-starter-remote --release-version 1.0.2
  scripts/publish-maven-batch.sh :mango-cms-starter :mango-cms-starter-remote --release-version 1.0.2-rc.20250701113000
  scripts/publish-maven-batch.sh mango-platform/mango-cms/mango-cms-starter --revision 1.0.2-SNAPSHOT --allow-snapshot
EOF
}

targets=()
all_non_app=false
include_apps=false
skip_tests=true
dry_run=false
verify_publish=true
verify_mode="${MANGO_MAVEN_VERIFY_MODE:-http}"
verify_transitive=false
allow_snapshot=false
revision="${MANGO_MAVEN_REVISION:-}"
verify_repo="${MANGO_MAVEN_VERIFY_REPO:-${REPO_ROOT}/.runtime/maven-publish-verify-batch}"
verify_work_dir="${MANGO_MAVEN_VERIFY_WORK_DIR:-${REPO_ROOT}/.runtime/maven-publish-verify-work}"
verify_base_url="${MANGO_MAVEN_VERIFY_BASE_URL:-}"
verify_only=false

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
    --all-non-app)
      all_non_app=true
      ;;
    --include-apps)
      include_apps=true
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
    --skip-verify)
      verify_publish=false
      ;;
    --verify-only)
      verify_only=true
      ;;
    --verify-mode)
      if [[ $# -lt 2 || "$2" == -* ]]; then
        echo "Missing value for --verify-mode." >&2
        usage
        exit 1
      fi
      verify_mode="$2"
      shift
      ;;
    --verify-mode=*)
      verify_mode="${1#--verify-mode=}"
      ;;
    --verify-base-url)
      if [[ $# -lt 2 || "$2" == -* ]]; then
        echo "Missing value for --verify-base-url." >&2
        usage
        exit 1
      fi
      verify_base_url="$2"
      shift
      ;;
    --verify-base-url=*)
      verify_base_url="${1#--verify-base-url=}"
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

if [[ "${all_non_app}" == "true" && ${#targets[@]} -gt 0 ]]; then
  echo "--all-non-app cannot be combined with explicit module targets." >&2
  usage
  exit 1
fi
if [[ "${all_non_app}" != "true" && ${#targets[@]} -eq 0 ]]; then
  echo "Missing Maven module targets. Use --all-non-app for the default backend release batch." >&2
  usage
  exit 1
fi
validate_revision "${revision}"
if [[ "${verify_mode}" != "http" && "${verify_mode}" != "maven" ]]; then
  echo "Invalid verification mode: ${verify_mode}" >&2
  echo "Use --verify-mode http or --verify-mode maven." >&2
  exit 1
fi
if [[ "${verify_publish}" == "true" && "${verify_mode}" == "http" && -z "${verify_base_url}" ]]; then
  echo "HTTP verification requires --verify-base-url or MANGO_MAVEN_VERIFY_BASE_URL." >&2
  exit 1
fi
verify_base_url="${verify_base_url%/}"

mvn_eval() {
  local pom_file="$1"
  local expression="$2"
  mvn -q -f "${pom_file}" help:evaluate "-Drevision=${revision}" -Dexpression="${expression}" -DforceStdout
}

collect_target_coordinates() {
  local coordinates_file="$1"
  (
    cd "${MAVEN_ROOT}"
    mvn -q -pl "${selected_project_list}" "-Drevision=${revision}" \
      org.codehaus.mojo:exec-maven-plugin:3.5.0:exec \
      -Dexec.executable=echo \
      '-Dexec.args=${project.groupId}:${project.artifactId}:${project.version}:${project.packaging}'
  ) | grep -E "^[^:]+:[^:]+:${revision}:[^:]+$" > "${coordinates_file}"
}

deploy_architecture_verification_pom() {
  local pom_file="${ARCHITECTURE_VERIFICATION_DIR}/pom.xml"
  local flattened_pom="${ARCHITECTURE_VERIFICATION_DIR}/.flattened-pom.xml"
  local group_id artifact_id packaging repository_id repository_url
  local flatten_args deploy_args

  group_id="$(mvn_eval "${pom_file}" project.groupId)"
  artifact_id="$(mvn_eval "${pom_file}" project.artifactId)"
  packaging="$(mvn_eval "${pom_file}" project.packaging)"
  repository_id="$(mvn_eval "${pom_file}" project.distributionManagement.repository.id)"
  repository_url="$(mvn_eval "${pom_file}" project.distributionManagement.repository.url)"
  if [[ -z "${group_id}" || -z "${artifact_id}" || "${packaging}" != "pom" \
    || -z "${repository_id}" || -z "${repository_url}" ]]; then
    echo "Unable to resolve architecture verification POM publication coordinates." >&2
    exit 1
  fi

  flatten_args=(
    -f "${pom_file}"
    "-Drevision=${revision}"
    -DskipTests
    process-resources
  )
  deploy_args=(
    org.apache.maven.plugins:maven-deploy-plugin:3.1.4:deploy-file
    "-Dfile=${flattened_pom}"
    "-DpomFile=${flattened_pom}"
    "-DgroupId=${group_id}"
    "-DartifactId=${artifact_id}"
    "-Dversion=${revision}"
    "-Dpackaging=pom"
    -DgeneratePom=false
    "-DrepositoryId=${repository_id}"
    "-Durl=${repository_url}"
  )

  echo "Publishing self-verification POM without executing its full-Reactor verify phase"
  printf 'Command: mvn'
  printf ' %q' "${flatten_args[@]}"
  printf '\n'
  printf 'Command: mvn'
  printf ' %q' "${deploy_args[@]}"
  printf '\n'
  if [[ "${dry_run}" == "true" ]]; then
    return 0
  fi

  mvn "${flatten_args[@]}"
  if [[ ! -f "${flattened_pom}" ]]; then
    echo "Flattened architecture verification POM was not generated: ${flattened_pom}" >&2
    exit 1
  fi
  mvn "${deploy_args[@]}"
}

artifact_url_base() {
  local group_id="$1"
  local artifact_id="$2"
  local version="$3"
  local group_path
  group_path="$(printf '%s' "${group_id}" | tr . /)"
  printf '%s/%s/%s/%s/%s-%s' "${verify_base_url}" "${group_path}" "${artifact_id}" "${version}" "${artifact_id}" "${version}"
}

verify_http_url() {
  local url="$1"
  if [[ "${dry_run}" == "true" ]]; then
    printf 'Command: curl -fsIL --max-time 20 %q\n' "${url}"
    return 0
  fi
  curl -fsIL --max-time 20 "${url}" >/dev/null
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

discover_non_app_targets() {
  local pom_file rel_path module_path
  while IFS= read -r pom_file; do
    if [[ "${pom_file}" == "${MAVEN_ROOT}/mango-app/pom.xml" || "${pom_file}" == "${MAVEN_ROOT}/mango-app/"* ]]; then
      continue
    fi
    rel_path="${pom_file#${MAVEN_ROOT}/}"
    if [[ "${rel_path}" == "pom.xml" ]]; then
      module_path="."
    else
      module_path="${rel_path%/pom.xml}"
    fi
    if [[ "${module_path##*/}" == *-test ]]; then
      continue
    fi
    targets+=("${module_path}")
  done < <(find "${MAVEN_ROOT}" -name pom.xml -not -path '*/target/*' -print | sort)
}

is_app_module_dir() {
  local module_dir="$1"
  [[ "${module_dir}" == "${MAVEN_ROOT}/mango-app" || "${module_dir}" == "${MAVEN_ROOT}/mango-app/"* ]]
}

if [[ "${all_non_app}" == "true" ]]; then
  discover_non_app_targets
fi

if [[ "${include_apps}" != "true" ]]; then
  for target in "${targets[@]}"; do
    module_dir="$(resolve_module_dir "${target}")" || {
      echo "Unable to resolve module directory: ${target}" >&2
      exit 1
    }
    if is_app_module_dir "${module_dir}"; then
      echo "Maven app artifact publish is blocked by default: ${target}" >&2
      echo "Use --all-non-app for the standard backend release batch, or pass --include-apps for an explicit deployment artifact release." >&2
      exit 1
    fi
  done
fi

selected_project_list=""
deploy_project_list=""
publish_architecture_verification=false
for target in "${targets[@]}"; do
  project="$(normalize_project "${target}")"
  if [[ -z "${selected_project_list}" ]]; then
    selected_project_list="${project}"
  else
    selected_project_list="${selected_project_list},${project}"
  fi
  module_dir="$(resolve_module_dir "${target}")" || {
    echo "Unable to resolve module directory: ${target}" >&2
    exit 1
  }
  if [[ "${module_dir}" == "${ARCHITECTURE_VERIFICATION_DIR}" ]]; then
    publish_architecture_verification=true
  elif [[ -z "${deploy_project_list}" ]]; then
    deploy_project_list="${project}"
  else
    deploy_project_list="${deploy_project_list},${project}"
  fi
done

declare -a mvn_args=()
if [[ -n "${deploy_project_list}" ]]; then
  mvn_args=(-pl "${deploy_project_list}" -am deploy "-Drevision=${revision}")
  if [[ "${skip_tests}" == "true" ]]; then
    mvn_args+=(-DskipTests)
  fi
fi

echo "Maven root: ${MAVEN_ROOT}"
if [[ "${all_non_app}" == "true" ]]; then
  echo "Publish scope: all non-app Maven modules"
else
  echo "Publish scope: explicit Maven modules"
fi
echo "Selected modules: ${selected_project_list}"
echo "Reactor deploy modules: ${deploy_project_list:-none}"
echo "Revision: ${revision}"
echo "Allow SNAPSHOT: ${allow_snapshot}"
echo "Include app artifacts: ${include_apps}"
if [[ "${verify_only}" == "true" ]]; then
  echo "Mode: verification only; deploy is skipped"
else
  echo "Mode: one reactor deploy for all selected modules and required upstream modules"
fi
if [[ "${verify_publish}" == "true" ]]; then
  echo "Verification mode: ${verify_mode}"
fi
if [[ "${verify_only}" == "false" && -n "${deploy_project_list}" ]]; then
  printf 'Command: mvn'
  printf ' %q' "${mvn_args[@]}"
  printf '\n'
fi

if [[ "${verify_only}" == "false" ]]; then
  if [[ "${dry_run}" == "false" && -n "${deploy_project_list}" ]]; then
    cd "${MAVEN_ROOT}"
    mvn "${mvn_args[@]}"
  fi
  if [[ "${publish_architecture_verification}" == "true" ]]; then
    deploy_architecture_verification_pom
  fi
fi

if [[ "${verify_publish}" != "true" ]]; then
  exit 0
fi

if [[ "${verify_mode}" == "http" ]]; then
  if [[ "${dry_run}" == "false" ]]; then
    rm -rf "${verify_work_dir}"
    mkdir -p "${verify_work_dir}"
  fi
  coordinates_file="${verify_work_dir}/coordinates.txt"
  if [[ "${dry_run}" == "true" ]]; then
    coordinates_file="$(mktemp "${TMPDIR:-/tmp}/mango-maven-coordinates.XXXXXX")"
    trap 'rm -f "${coordinates_file}"' EXIT
  fi
  collect_target_coordinates "${coordinates_file}"

  echo "Verification repository URL: ${verify_base_url}"
  while IFS=: read -r group_id artifact_id version packaging; do
    artifact_coordinates="${group_id}:${artifact_id}:${version}"
    if [[ "${packaging}" == "pom" ]]; then
      artifact_coordinates="${artifact_coordinates}:pom"
    fi
    echo "Verifying published Maven artifact by HTTP: ${artifact_coordinates}"
    base_url="$(artifact_url_base "${group_id}" "${artifact_id}" "${version}")"
    verify_http_url "${base_url}.pom" || {
      echo "Published Maven pom is not reachable: ${base_url}.pom" >&2
      exit 1
    }
    if [[ "${packaging}" != "pom" ]]; then
      verify_http_url "${base_url}.jar" || {
        echo "Published Maven jar is not reachable: ${base_url}.jar" >&2
        exit 1
      }
    fi
  done < "${coordinates_file}"
  exit 0
fi

verify_args=(-U org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get "-Dmaven.repo.local=${verify_repo}")
if [[ "${verify_transitive}" == "true" ]]; then
  verify_args+=(-Dtransitive=true)
else
  verify_args+=(-Dtransitive=false)
fi

echo "Verification local repository: ${verify_repo}"
echo "Verification Maven work directory: ${verify_work_dir}"
if [[ "${dry_run}" == "false" ]]; then
  rm -rf "${verify_repo}"
  mkdir -p "${verify_repo}"
  rm -rf "${verify_work_dir}"
  mkdir -p "${verify_work_dir}"
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

  (
    cd "${verify_work_dir}"
    mvn "${verify_args[@]}" "-Dartifact=${artifact_coordinates}"
  )
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
