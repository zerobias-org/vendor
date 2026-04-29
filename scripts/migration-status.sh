#!/usr/bin/env bash
#
# Regenerate MIGRATION_STATUS.md — a snapshot of which vendor packages
# have been migrated to the gradle (zb.content) pipeline and which are
# still on the lerna-era 1.x line.
#
# A vendor is considered migrated when its directory contains a
# build.gradle.kts marker tracked on origin/main. Pre-flight URL/code
# checks mirror what the gate's contentValidator enforces; vendors with
# obvious schema problems are flagged so they can be triaged before
# they reach the matrix publish job.
#
# Usage:
#   ./scripts/migration-status.sh             # writes MIGRATION_STATUS.md
#   ./scripts/migration-status.sh --check     # exit non-zero if file out of date
#
# Run from the repo root.

set -euo pipefail
export LC_ALL=C

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

OUT="MIGRATION_STATUS.md"
TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

# Vendors migrated on origin/main (build.gradle.kts present)
mapfile -t MIGRATED < <(git ls-tree -r origin/main --name-only \
  | grep -E '^package/[^/]+/build\.gradle\.kts$' \
  | cut -d/ -f2 \
  | sort)

# All vendor directories
mapfile -t ALL < <(find package -maxdepth 1 -mindepth 1 -type d \
  | sed 's|^package/||' | sort)

total=${#ALL[@]}
migrated=${#MIGRATED[@]}
pending=$((total - migrated))
pct=$(awk -v m="$migrated" -v t="$total" 'BEGIN{printf "%.1f", (m/t)*100}')

CODE_REGEX='^[a-z0-9_]+$'

is_migrated() {
  local v="$1"
  printf '%s\n' "${MIGRATED[@]}" | grep -qx "$v"
}

flag_for() {
  local v="$1"
  local indexyml="package/$v/index.yml"
  local pkgjson="package/$v/package.json"
  local flags=""

  [ -f "$indexyml" ] || flags="$flags missing-index.yml"
  [ -f "$pkgjson" ]  || flags="$flags missing-package.json"

  if [ -f "$indexyml" ]; then
    local code url
    code=$(awk -F': ' '/^code:/{print $2; exit}' "$indexyml" | tr -d '"' | tr -d "'" | xargs)
    url=$(awk -F': ' '/^url:/{$1=""; sub(/^[ \t:]+/,""); print; exit}' "$indexyml" | tr -d '"' | tr -d "'" | xargs)
    if [ -n "$code" ] && ! [[ "$code" =~ $CODE_REGEX ]]; then
      flags="$flags bad-code"
    fi
    if [ -n "$url" ] && ! [[ "$url" =~ ^https?:// ]]; then
      flags="$flags bad-url"
    fi
  fi

  echo "$flags" | xargs
}

{
  echo "# Migration Status — \`zerobias-org/vendor\`"
  echo
  echo "Tracker for the gradle (zb.content) migration of vendor packages."
  echo "Regenerate with \`./scripts/migration-status.sh\`."
  echo
  echo "**Last updated:** $(date -u +%Y-%m-%dT%H:%M:%SZ)  "
  echo "**Migrated:** $migrated / $total ($pct%)  "
  echo "**Pending:** $pending"
  echo
  echo "## Legend"
  echo
  echo "- ✅ migrated — has \`build.gradle.kts\` on \`origin/main\`"
  echo "- ⬜ pending — still on the lerna-era flow"
  echo "- ⚠ flagged — pre-flight schema issue surfaced (will fail gate as-is). See the Flagged section."
  echo
  echo "## Flagged (fix before migrating)"
  echo
  echo "These vendors will fail \`./gradlew :<vendor>:gate\` as-is — fix the index.yml / package.json drift before adding the gradle marker."
  echo
  echo "| vendor | current version | flags |"
  echo "|---|---|---|"
  flagged_count=0
  for v in "${ALL[@]}"; do
    if is_migrated "$v"; then continue; fi
    flags=$(flag_for "$v")
    if [ -n "$flags" ]; then
      ver=$(jq -r '.version // "?"' "package/$v/package.json" 2>/dev/null || echo "?")
      printf '| %s | %s | %s |\n' "$v" "$ver" "$flags"
      flagged_count=$((flagged_count + 1))
    fi
  done
  if [ "$flagged_count" -eq 0 ]; then
    echo "| _(none)_ | | |"
  fi
  echo
  echo "_$flagged_count flagged_"
  echo
  echo "## All vendors"
  echo
  echo "| vendor | status | current version |"
  echo "|---|---|---|"
  for v in "${ALL[@]}"; do
    ver=$(jq -r '.version // "?"' "package/$v/package.json" 2>/dev/null || echo "?")
    if is_migrated "$v"; then
      status="✅ migrated"
    else
      flags=$(flag_for "$v")
      if [ -n "$flags" ]; then
        status="⚠ flagged ($flags)"
      else
        status="⬜ pending"
      fi
    fi
    printf '| %s | %s | %s |\n' "$v" "$status" "$ver"
  done
} > "$TMP"

if [ "${1:-}" = "--check" ]; then
  if ! diff -q "$OUT" "$TMP" >/dev/null 2>&1; then
    echo "MIGRATION_STATUS.md is out of date — run scripts/migration-status.sh and commit." >&2
    exit 1
  fi
  echo "MIGRATION_STATUS.md is current."
  exit 0
fi

mv "$TMP" "$OUT"
trap - EXIT
echo "Wrote $OUT"
echo "  migrated: $migrated / $total ($pct%)"
