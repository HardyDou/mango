#!/usr/bin/env bash
set -euo pipefail

required_commands=(curl jq)
for command_name in "${required_commands[@]}"; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Required command is not available: ${command_name}" >&2
    exit 2
  fi
done

required_variables=(
  JENKINS_URL
  JENKINS_USER
  JENKINS_API_TOKEN
  GIT_SHA
  RELEASE_VERSION
  REQUEST_ID
)
for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Required environment variable is empty: ${variable_name}" >&2
    exit 2
  fi
done

JENKINS_JOB="${JENKINS_JOB:-mango-maven-release}"
DRY_RUN="${DRY_RUN:-true}"
RUN_TESTS="${RUN_TESTS:-false}"
TIMEOUT_SECONDS="${JENKINS_TIMEOUT_SECONDS:-10800}"
POLL_SECONDS="${JENKINS_POLL_SECONDS:-5}"

if [[ ! "${JENKINS_URL}" =~ ^https?://[^[:space:]]+$ ]]; then
  echo "JENKINS_URL must be an HTTP or HTTPS URL." >&2
  exit 2
fi
if [[ "${JENKINS_URL#*://}" == *@* ]]; then
  echo "JENKINS_URL must not embed credentials." >&2
  exit 2
fi
if [[ ! "${JENKINS_JOB}" =~ ^[A-Za-z0-9._/-]+$ ]] || [[ "${JENKINS_JOB}" == /* ]] || [[ "${JENKINS_JOB}" == */ ]]; then
  echo "JENKINS_JOB contains unsupported characters or path structure." >&2
  exit 2
fi
IFS='/' read -r -a job_segments <<< "${JENKINS_JOB}"
for job_segment in "${job_segments[@]}"; do
  if [[ -z "${job_segment}" || "${job_segment}" == "." || "${job_segment}" == ".." ]]; then
    echo "JENKINS_JOB contains an invalid path segment." >&2
    exit 2
  fi
done
if [[ ! "${GIT_SHA}" =~ ^[0-9a-fA-F]{40}$ ]]; then
  echo "GIT_SHA must be a full 40-character Git commit SHA." >&2
  exit 2
fi
if [[ ! "${RELEASE_VERSION}" =~ ^[0-9]+(\.[0-9]+){2,}([-+][0-9A-Za-z][0-9A-Za-z.-]*)?$ ]]; then
  echo "RELEASE_VERSION must be a SemVer-like Maven version." >&2
  exit 2
fi
if [[ ! "${REQUEST_ID}" =~ ^[A-Za-z0-9._-]{1,128}$ ]]; then
  echo "REQUEST_ID must contain 1-128 letters, digits, dots, underscores, or hyphens." >&2
  exit 2
fi
if [[ "${DRY_RUN}" != "true" && "${DRY_RUN}" != "false" ]]; then
  echo "DRY_RUN must be true or false." >&2
  exit 2
fi
if [[ "${RUN_TESTS}" != "true" && "${RUN_TESTS}" != "false" ]]; then
  echo "RUN_TESTS must be true or false." >&2
  exit 2
fi
if [[ ! "${TIMEOUT_SECONDS}" =~ ^[0-9]+$ ]] || (( TIMEOUT_SECONDS < 60 || TIMEOUT_SECONDS > 21600 )); then
  echo "JENKINS_TIMEOUT_SECONDS must be between 60 and 21600." >&2
  exit 2
fi
if [[ ! "${POLL_SECONDS}" =~ ^[0-9]+$ ]] || (( POLL_SECONDS < 1 || POLL_SECONDS > 60 )); then
  echo "JENKINS_POLL_SECONDS must be between 1 and 60." >&2
  exit 2
fi
if [[ ! "${JENKINS_USER}" =~ ^[A-Za-z0-9._@-]+$ ]] || [[ "${JENKINS_API_TOKEN}" == *$'\n'* || "${JENKINS_API_TOKEN}" == *' '* ]]; then
  echo "Jenkins credentials contain unsupported whitespace." >&2
  exit 2
fi

runtime_dir="${RUNNER_TEMP:-${TMPDIR:-/tmp}}"
netrc_file="$(mktemp "${runtime_dir%/}/mango-jenkins-netrc.XXXXXX")"
headers_file="$(mktemp "${runtime_dir%/}/mango-jenkins-headers.XXXXXX")"
body_file="$(mktemp "${runtime_dir%/}/mango-jenkins-body.XXXXXX")"
chmod 600 "${netrc_file}" "${headers_file}" "${body_file}"

jenkins_host="${JENKINS_URL#*://}"
jenkins_host="${jenkins_host%%/*}"
jenkins_host="${jenkins_host%%:*}"
printf 'machine %s login %s password %s\n' \
  "${jenkins_host}" "${JENKINS_USER}" "${JENKINS_API_TOKEN}" > "${netrc_file}"

queue_url=""
build_url=""
build_finished=false

curl_jenkins() {
  curl --silent --show-error --netrc-file "${netrc_file}" "$@"
}

cancel_jenkins_work() {
  if [[ "${build_finished}" == "true" ]]; then
    return
  fi
  if [[ -n "${build_url}" ]]; then
    echo "Cancelling Jenkins build: ${build_url}"
    curl_jenkins --request POST "${build_url%/}/stop" >/dev/null 2>&1 || true
    return
  fi
  if [[ "${queue_url}" =~ /queue/item/([0-9]+)/?$ ]]; then
    echo "Cancelling Jenkins queue item: ${BASH_REMATCH[1]}"
    curl_jenkins --request POST \
      "${JENKINS_URL%/}/queue/cancelItem?id=${BASH_REMATCH[1]}" >/dev/null 2>&1 || true
  fi
}

on_exit() {
  local exit_code=$?
  if (( exit_code != 0 )); then
    cancel_jenkins_work
  fi
  rm -f "${netrc_file}" "${headers_file}" "${body_file}"
}
trap on_exit EXIT
trap 'exit 130' INT TERM

url_encode() {
  jq -nr --arg value "$1" '$value | @uri'
}

job_path=""
for job_segment in "${job_segments[@]}"; do
  job_path+="/job/$(url_encode "${job_segment}")"
done
job_url="${JENKINS_URL%/}${job_path}"

echo "Triggering Jenkins job '${JENKINS_JOB}' for commit ${GIT_SHA} (version=${RELEASE_VERSION}, dry_run=${DRY_RUN}, request_id=${REQUEST_ID})."
http_status="$(curl_jenkins \
  --request POST \
  --dump-header "${headers_file}" \
  --output "${body_file}" \
  --write-out '%{http_code}' \
  "${job_url}/buildWithParameters" \
  --data-urlencode "GIT_SHA=${GIT_SHA}" \
  --data-urlencode "RELEASE_VERSION=${RELEASE_VERSION}" \
  --data-urlencode "REQUEST_ID=${REQUEST_ID}" \
  --data-urlencode "DRY_RUN=${DRY_RUN}" \
  --data-urlencode "RUN_TESTS=${RUN_TESTS}")"

if [[ "${http_status}" != "201" ]]; then
  echo "Jenkins rejected the build request with HTTP ${http_status}." >&2
  if [[ -s "${body_file}" ]]; then
    sed -n '1,20p' "${body_file}" >&2
  fi
  exit 3
fi

queue_url="$(awk 'tolower($1) == "location:" { print $2 }' "${headers_file}" | tr -d '\r' | tail -1)"
if [[ -z "${queue_url}" ]]; then
  echo "Jenkins response did not include a queue Location header." >&2
  exit 3
fi
if [[ "${queue_url}" == /* ]]; then
  queue_url="${JENKINS_URL%/}${queue_url}"
fi
echo "Jenkins queue item: ${queue_url}"

deadline=$(( $(date +%s) + TIMEOUT_SECONDS ))
last_progress_at=0
while [[ -z "${build_url}" ]]; do
  if (( $(date +%s) >= deadline )); then
    echo "Timed out waiting for Jenkins to start the queued build." >&2
    exit 4
  fi
  queue_json="$(curl_jenkins "${queue_url%/}/api/json")"
  if ! jq -e . >/dev/null 2>&1 <<< "${queue_json}"; then
    echo "Jenkins returned invalid queue JSON." >&2
    exit 3
  fi
  if [[ "$(jq -r '.cancelled // false' <<< "${queue_json}")" == "true" ]]; then
    echo "Jenkins cancelled the queued build." >&2
    exit 5
  fi
  build_url="$(jq -r '.executable.url // empty' <<< "${queue_json}")"
  if [[ -z "${build_url}" ]]; then
    now="$(date +%s)"
    if (( now - last_progress_at >= 30 )); then
      why="$(jq -r '.why // "waiting for an executor"' <<< "${queue_json}")"
      echo "Jenkins queue status: ${why}"
      last_progress_at="${now}"
    fi
    sleep "${POLL_SECONDS}"
  fi
done

echo "Jenkins build started: ${build_url}"
last_progress_at=0
while true; do
  if (( $(date +%s) >= deadline )); then
    echo "Timed out waiting for Jenkins build completion." >&2
    exit 4
  fi
  build_json="$(curl_jenkins "${build_url%/}/api/json")"
  if ! jq -e . >/dev/null 2>&1 <<< "${build_json}"; then
    echo "Jenkins returned invalid build JSON." >&2
    exit 3
  fi
  if [[ "$(jq -r '.building // false' <<< "${build_json}")" == "false" ]]; then
    build_finished=true
    break
  fi
  now="$(date +%s)"
  if (( now - last_progress_at >= 30 )); then
    display_name="$(jq -r '.fullDisplayName // .displayName // "Jenkins build"' <<< "${build_json}")"
    echo "Jenkins build is running: ${display_name}"
    last_progress_at="${now}"
  fi
  sleep "${POLL_SECONDS}"
done

jenkins_result="$(jq -r '.result // "UNKNOWN"' <<< "${build_json}")"
build_number="$(jq -r '.number // empty' <<< "${build_json}")"
if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "jenkins_build_url=${build_url}"
    echo "jenkins_build_number=${build_number}"
    echo "jenkins_result=${jenkins_result}"
  } >> "${GITHUB_OUTPUT}"
fi

echo "Jenkins build completed with result ${jenkins_result}: ${build_url}"
if [[ "${jenkins_result}" != "SUCCESS" ]]; then
  exit 6
fi
