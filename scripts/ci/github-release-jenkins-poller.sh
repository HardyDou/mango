#!/usr/bin/env bash
set -euo pipefail

command_name="${1:-discover}"
repository="${GITHUB_REPOSITORY:-HardyDou/mango}"
state_dir="${MANGO_RELEASE_STATE_DIR:-${JENKINS_HOME:-.}/release-state/mango}"
output_file="${MANGO_RELEASE_POLL_OUTPUT:-${state_dir}/candidate.properties}"
asset_name="mango-internal-release-request-v1.json"
feed_url="https://github.com/${repository}/releases.atom"
ledger="${state_dir}/ledger.tsv"

mkdir -p "${state_dir}"
touch "${ledger}"

validate_tag() {
  [[ "$1" =~ ^[0-9A-Za-z._+-]{1,160}$ ]]
}

known_tag() {
  awk -F '\t' -v tag="$1" '$4 == tag { found=1; exit } END { exit !found }' "${ledger}"
}

record_state() {
  local status="$1" release_id="$2" tag="$3" version="$4" sha="$5"
  printf '%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "${status}" "${release_id}" "${tag}" "${version}" "${sha}" >> "${ledger}"
}

fetch_tags() {
  curl --connect-timeout 10 --max-time 30 --retry 3 --retry-all-errors -fsSL "${feed_url}" |
    perl -ne 'while (/href="https:\/\/github\.com\/[^"\/]+\/[^"\/]+\/releases\/tag\/([^"?]+)"/g) { print "$1\n" }' |
    awk '!seen[$0]++'
}

case "${command_name}" in
  bootstrap)
    while IFS= read -r tag; do
      validate_tag "${tag}" || continue
      known_tag "${tag}" || record_state baseline 0 "${tag}" - -
    done < <(fetch_tags)
    echo "Release poller baseline created at ${ledger}."
    ;;

  discover)
    : > "${output_file}"
    while IFS= read -r tag; do
      validate_tag "${tag}" || continue
      known_tag "${tag}" && continue

      manifest="$(mktemp "${state_dir}/manifest.XXXXXX.json")"
      trap 'rm -f "${manifest:-}"' EXIT
      asset_url="https://github.com/${repository}/releases/download/${tag}/${asset_name}"
      http_status="$(curl -L --connect-timeout 10 --max-time 30 --retry 2 --retry-all-errors \
        -sS -o "${manifest}" -w '%{http_code}' "${asset_url}" || true)"
      if [[ "${http_status}" == "404" ]]; then
        rm -f "${manifest}"
        trap - EXIT
        continue
      fi
      if [[ "${http_status}" != "200" ]]; then
        echo "Unable to download release request for ${tag}: HTTP ${http_status}" >&2
        exit 1
      fi

      parsed="$(EXPECTED_REPOSITORY="${repository}" EXPECTED_TAG="${tag}" perl -MJSON::PP -0777 -e '
        my $data = decode_json(<>);
        die "invalid schema\n" unless ($data->{schema} // "") eq "mango.internal-release-request/v1";
        die "repository mismatch\n" unless ($data->{repository} // "") eq $ENV{EXPECTED_REPOSITORY};
        die "tag mismatch\n" unless ($data->{tag} // "") eq $ENV{EXPECTED_TAG};
        die "invalid release id\n" unless ($data->{releaseId} // "") =~ /^\d+$/;
        die "invalid source sha\n" unless ($data->{sourceSha} // "") =~ /^[0-9a-f]{40}$/;
        my $maven = $data->{components}->{maven};
        my $version = defined($maven) ? ($maven->{version} // "") : "";
        die "invalid Maven version\n" if $version ne "" && $version !~ /^\d+(?:\.\d+){2,}(?:[-+][0-9A-Za-z][0-9A-Za-z.-]*)?$/;
        my $run_tests = defined($maven) && $maven->{runTests} ? "true" : "false";
        print join("\t", $data->{releaseId}, $data->{sourceSha}, $version, $run_tests);
      ' < "${manifest}")"
      IFS=$'\t' read -r release_id source_sha maven_version run_tests <<< "${parsed}"

      if [[ -z "${maven_version}" ]]; then
        record_state ignored-no-maven "${release_id}" "${tag}" - "${source_sha}"
        rm -f "${manifest}"
        trap - EXIT
        continue
      fi

      last_success="$(awk -F '\t' '$2 == "success" { value=$5 } END { print value }' "${ledger}")"
      if [[ -n "${last_success}" && "${last_success}" != "-" ]]; then
        version_is_newer="$(LAST_VERSION="${last_success}" CANDIDATE_VERSION="${maven_version}" perl -e '
          sub parts { [map { 0 + $_ } split /\./, $_[0]] }
          my $left = parts($ENV{LAST_VERSION});
          my $right = parts($ENV{CANDIDATE_VERSION});
          my $newer = 0;
          for my $index (0..2) {
            if (($right->[$index] // 0) > ($left->[$index] // 0)) { $newer = 1; last }
            if (($right->[$index] // 0) < ($left->[$index] // 0)) { last }
          }
          print $newer ? "true" : "false";
        ')"
        if [[ "${version_is_newer}" != "true" ]]; then
          record_state blocked-rollback "${release_id}" "${tag}" "${maven_version}" "${source_sha}"
          echo "Blocked Maven release rollback: candidate=${maven_version}, last_success=${last_success}" >&2
          exit 1
        fi
      fi

      {
        echo "ACTION=release"
        echo "RELEASE_TAG=${tag}"
        echo "RELEASE_ID=${release_id}"
        echo "GIT_SHA=${source_sha}"
        echo "RELEASE_VERSION=${maven_version}"
        echo "RUN_TESTS=${run_tests}"
        echo "REQUEST_ID=github-release-${release_id}"
      } > "${output_file}"
      rm -f "${manifest}"
      trap - EXIT
      echo "Discovered Maven ${maven_version} from GitHub Release ${tag}."
      exit 0
    done < <(fetch_tags)
    echo "ACTION=none" > "${output_file}"
    echo "No unprocessed internal release request."
    ;;

  claim|success|failed)
    : "${RELEASE_ID:?RELEASE_ID is required}"
    : "${RELEASE_TAG:?RELEASE_TAG is required}"
    : "${RELEASE_VERSION:?RELEASE_VERSION is required}"
    : "${GIT_SHA:?GIT_SHA is required}"
    validate_tag "${RELEASE_TAG}" || { echo "Invalid release tag." >&2; exit 2; }
    if [[ "${command_name}" == "claim" ]]; then
      if known_tag "${RELEASE_TAG}"; then
        echo "Release was already claimed or completed: ${RELEASE_TAG}" >&2
        exit 3
      fi
      record_state claimed "${RELEASE_ID}" "${RELEASE_TAG}" "${RELEASE_VERSION}" "${GIT_SHA}"
    else
      if ! awk -F '\t' -v tag="${RELEASE_TAG}" '$2 == "claimed" && $4 == tag { found=1 } END { exit !found }' "${ledger}"; then
        echo "Release has not been claimed: ${RELEASE_TAG}" >&2
        exit 3
      fi
      record_state "${command_name}" "${RELEASE_ID}" "${RELEASE_TAG}" "${RELEASE_VERSION}" "${GIT_SHA}"
    fi
    echo "Recorded ${command_name}: ${RELEASE_TAG} (${RELEASE_VERSION})."
    ;;

  *)
    echo "Usage: $0 <bootstrap|discover|claim|success|failed>" >&2
    exit 2
    ;;
esac
