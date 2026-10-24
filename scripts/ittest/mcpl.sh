# shellcheck shell=bash
# MCProtocolLib (MCPL) builds for the presence client. Sourced by scripts/ittest/run; tested by
# scripts/ittest/tests/test_mcpl.py. Each lookup prints "<version> <repository>", where repository
# is the opencollab Maven repository holding that build, or says on stderr why there is none and
# returns non-zero. A missing build is never a reason to run without the client.

# Overridable so the tests can point it at a file:// fixture.
MCPL_REPOSITORY_ROOT="${MCPL_REPOSITORY_ROOT:-https://repo.opencollab.dev}"
MCPL_ARTIFACT_PATH=org/geysermc/mcprotocollib/protocol

# The reviewed build for a Minecraft version: its row in the mcProtocolLib table of artifacts.json.
mcpl_pinned_build() {
  local mc="$1" artifacts="$2"
  if ! jq -er --arg mc "${mc}" \
    '.mcProtocolLib[$mc] | select(.version) | "\(.version) \(.repository // "maven-releases")"' "${artifacts}"; then
    echo "No MCProtocolLib build for Minecraft ${mc}: add a reviewed row to mcProtocolLib in ${artifacts}." >&2
    return 1
  fi
}

# A repository's maven-metadata.xml for the protocol artifact, or for one of its snapshot versions
# when given "<version>-SNAPSHOT/". Fails when the repository cannot be read, so an unreachable
# repository never looks like "no build".
mcpl_metadata() {
  local repository="$1" directory="${2:-}"
  if ! curl --fail --silent --show-error --location --retry 3 --retry-delay 2 --retry-connrefused \
    -H "User-Agent: ${USER_AGENT}" "${MCPL_REPOSITORY_ROOT}/${repository}/${MCPL_ARTIFACT_PATH}/${directory}maven-metadata.xml"; then
    echo "Cannot read MCProtocolLib metadata from ${MCPL_REPOSITORY_ROOT}/${repository}." >&2
    return 1
  fi
}

mcpl_metadata_versions() {
  grep -oE '<version>[^<]+</version>' | sed -E 's|</?version>||g' || true
}

# The newest build for a Minecraft version; only the canary lane asks for it. MCPL names release
# builds "<mc>" or "<mc>-<build>" (maven-releases) and snapshots "<mc>-SNAPSHOT" (maven-snapshots
# only). The highest release build wins - a plain "<mc>" counts as build 0, other suffixes such as
# "-feature-..." are ignored. Without one, the newest snapshot, pinned to its timestamped build so
# the run manifest replays exactly. Returns 2 when neither repository has a build by that name, and
# 1 when a repository cannot be read.
mcpl_latest_build() {
  local mc="$1" esc metadata release timestamp number
  esc="${mc//./\\.}"
  metadata="$(mcpl_metadata maven-releases)" || return 1
  release="$(mcpl_metadata_versions <<<"${metadata}" | grep -E "^${esc}(-[0-9]+)?$" \
    | awk -v mc="${mc}" '{ b = substr($0, length(mc) + 2); print (b == "" ? 0 : b) "\t" $0 }' \
    | sort -n | tail -n1 | cut -f2 || true)"
  if [[ -n "${release}" ]]; then
    printf '%s maven-releases\n' "${release}"
    return 0
  fi
  metadata="$(mcpl_metadata maven-snapshots)" || return 1
  if ! mcpl_metadata_versions <<<"${metadata}" | grep -xF "${mc}-SNAPSHOT" >/dev/null; then
    echo "No MCProtocolLib build for Minecraft ${mc} in maven-releases or maven-snapshots." >&2
    return 2
  fi
  metadata="$(mcpl_metadata maven-snapshots "${mc}-SNAPSHOT/")" || return 1
  timestamp="$(sed -n 's|.*<timestamp>\([^<]*\)</timestamp>.*|\1|p' <<<"${metadata}" | head -n1)"
  number="$(sed -n 's|.*<buildNumber>\([^<]*\)</buildNumber>.*|\1|p' <<<"${metadata}" | head -n1)"
  if [[ -z "${timestamp}" || -z "${number}" ]]; then
    echo "MCProtocolLib ${mc}-SNAPSHOT lists no timestamped build in maven-snapshots." >&2
    return 1
  fi
  printf '%s-%s-%s maven-snapshots\n' "${mc}" "${timestamp}" "${number}"
}

# The canary's build: the newest build named after its Minecraft version, or, when MCPL has none by
# that name, the version's reviewed row. MCPL never names a build after a patch release that keeps
# its protocol (26.1-1 serves 26.1.2), so without the row the canary could not pass on one. An
# unreachable repository fails rather than falling back, and so does a version without a row.
mcpl_canary_build() {
  local mc="$1" artifacts="$2" status=0
  mcpl_latest_build "${mc}" || status=$?
  if [[ "${status}" -ne 2 ]]; then return "${status}"; fi
  echo "Falling back to the reviewed mcProtocolLib row for Minecraft ${mc}." >&2
  mcpl_pinned_build "${mc}" "${artifacts}"
}
