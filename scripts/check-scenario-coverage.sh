#!/usr/bin/env bash
# Fails if a spec scenario has no test. CI runs this script (constitution §2).
#
# Scenario IDs come from "#### Scenario: <capability>/R<n>-S<m> ..." headings in
# openspec/specs/**/spec.md and openspec/changes/*/specs/**/spec.md (archive excluded).
# Where a scenario's test is looked up depends on its capability:
#   - a capability in FRONTEND_CAPABILITIES: in the tests the runners list, from
#     `npx vitest list --json` and `npx playwright test --list --reporter=json`, run in
#     frontend/ (needs `npm ci` there; no browser or running stack). Covered means a test
#     title starts with [<id>]. Skipped tests are not listed, so they do not count; a
#     scenario ID only in a describe title does not count either. A stray test.only hides
#     other tests from Vitest, so it shows up as MISSING;
#   - any other capability: only as a plain text match of @DisplayName("[<id>] in
#     backend/src/test (a @Disabled test still counts).
# So a backend scenario cannot pass on a frontend test, or the other way round.
#
# A scenario ID is not required when it sits in a main spec under a requirement that an
# active change delta lists under "## REMOVED Requirements" (same capability, same header,
# compared ignoring whitespace differences), unless the same ID also appears in an active
# change delta outside a REMOVED section, e.g. in the ADDED requirement that replaces it.
#
# To add a capability whose tests are frontend tests, put its name in the list below.
# Forgetting it shows up as MISSING, never as a false pass.
FRONTEND_CAPABILITIES="web-client"
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

# Prints "<capability>|<header>" for each requirement that an active change delta removes.
removed_requirements() {
  local delta
  for delta in $(find openspec/changes -path '*/specs/*/spec.md' -not -path '*/archive/*' | sort); do
    awk -v capability="$(basename "$(dirname "$delta")")" '
      /^## / { removed = ($0 ~ /^## REMOVED Requirements[[:space:]]*$/) }
      removed && /^### Requirement:/ {
        header = $0
        sub(/^### Requirement:/, "", header)
        gsub(/[[:space:]]+/, " ", header)
        sub(/^ /, "", header)
        sub(/ $/, "", header)
        print capability "|" header
      }
    ' "$delta"
  done
}

# Prints the IDs of scenarios in active change deltas that are outside a REMOVED section.
delta_scenarios_kept() {
  local delta
  for delta in $(find openspec/changes -path '*/specs/*/spec.md' -not -path '*/archive/*' | sort); do
    awk '
      /^## / { removed = ($0 ~ /^## REMOVED Requirements[[:space:]]*$/) }
      !removed && /^#### Scenario: / {
        id = $0
        sub(/^#### Scenario: /, "", id)
        sub(/ .*/, "", id)
        print id
      }
    ' "$delta"
  done
}

# Prints the IDs of main spec scenarios under a requirement that a delta removes.
main_scenarios_removed() {
  local removed_list="$1"
  local spec
  for spec in $(find openspec/specs -name spec.md | sort); do
    REMOVED_LIST="$removed_list" awk -v capability="$(basename "$(dirname "$spec")")" '
      BEGIN {
        count = split(ENVIRON["REMOVED_LIST"], entries, "\n")
        for (i = 1; i <= count; i++) {
          removed[entries[i]] = 1
        }
      }
      /^### Requirement:/ {
        header = $0
        sub(/^### Requirement:/, "", header)
        gsub(/[[:space:]]+/, " ", header)
        sub(/^ /, "", header)
        sub(/ $/, "", header)
        in_removed = ((capability "|" header) in removed)
      }
      in_removed && /^#### Scenario: / {
        id = $0
        sub(/^#### Scenario: /, "", id)
        sub(/ .*/, "", id)
        print id
      }
    ' "$spec"
  done
}

specs=$(find openspec/specs openspec/changes -name spec.md -not -path '*/archive/*' 2>/dev/null | sort || true)
if [ -z "$specs" ]; then
  echo "No spec files found" >&2
  exit 1
fi

# shellcheck disable=SC2086
ids=$(grep -h -E '^#### Scenario: ' $specs | sed -E 's/^#### Scenario: ([^ ]+).*/\1/' | sort -u)
if [ -z "$ids" ]; then
  echo "No scenarios found in specs" >&2
  exit 1
fi

# Drop the IDs of removed requirements that no delta brings back.
removed_list=$(removed_requirements)
if [ -n "$removed_list" ]; then
  not_required=$(comm -23 \
    <(main_scenarios_removed "$removed_list" | sort -u) \
    <(delta_scenarios_kept | sort -u))
  if [ -n "$not_required" ]; then
    ids=$(comm -23 <(echo "$ids") <(echo "$not_required"))
  fi
fi

is_frontend_capability() {
  local capability
  for capability in $FRONTEND_CAPABILITIES; do
    if [ "$capability" = "${1%%/*}" ]; then
      return 0
    fi
  done
  return 1
}

has_backend_test() {
  grep -r -F -q "@DisplayName(\"[$1]" backend/src/test 2>/dev/null
}

has_test() {
  if is_frontend_capability "$1"; then
    grep -q -F -x -e "[$1]" "$frontend_ids"
  else
    has_backend_test "$1"
  fi
}

# Writes the IDs ([<id>]) of the frontend tests that are not skipped, one per line.
collect_frontend_ids() {
  local tmp="$1"
  if [ ! -d frontend/node_modules ]; then
    echo "frontend/node_modules is missing: run 'npm ci' in frontend/ first" >&2
    return 1
  fi
  if ! (cd frontend && npx vitest list --json) >"$tmp/vitest.json" 2>"$tmp/vitest.err"; then
    echo "'npx vitest list --json' failed (a stray test.only fails it in CI):" >&2
    if [ -s "$tmp/vitest.err" ]; then cat "$tmp/vitest.err" >&2; else cat "$tmp/vitest.json" >&2; fi
    return 1
  fi
  if ! (cd frontend && npx playwright test --list --reporter=json) >"$tmp/playwright.json" 2>"$tmp/playwright.err"; then
    echo "'npx playwright test --list --reporter=json' failed:" >&2
    cat "$tmp/playwright.err" >&2
    node -e 'try { for (const e of JSON.parse(require("fs").readFileSync(process.argv[1], "utf8")).errors) console.error(e.message); } catch {}' "$tmp/playwright.json" >&2
    return 1
  fi
  if ! node - "$tmp/vitest.json" "$tmp/playwright.json" >"$tmp/ids.txt" 2>"$tmp/parse.err" <<'NODE'
const fs = require('fs');
const [vitestFile, playwrightFile] = process.argv.slice(2);
const titles = [];

const vitest = JSON.parse(fs.readFileSync(vitestFile, 'utf8'));
for (const test of vitest) {
  titles.push(test.name.split(' > ').pop());
}

const playwright = JSON.parse(fs.readFileSync(playwrightFile, 'utf8'));
if (playwright.errors && playwright.errors.length > 0) {
  throw new Error(playwright.errors.map((e) => e.message).join('\n'));
}
function walk(suite) {
  for (const spec of suite.specs || []) {
    if (spec.tests.some((t) => t.expectedStatus !== 'skipped')) {
      titles.push(spec.title);
    }
  }
  for (const child of suite.suites || []) {
    walk(child);
  }
}
for (const suite of playwright.suites) {
  walk(suite);
}

for (const title of titles) {
  const match = /^\[[^\]]+\]/.exec(title);
  if (match) {
    console.log(match[0]);
  }
}
NODE
  then
    echo "Could not read the test lists from Vitest and Playwright:" >&2
    cat "$tmp/parse.err" >&2
    return 1
  fi
}

frontend_ids=""
for id in $ids; do
  if is_frontend_capability "$id"; then
    tmp=$(mktemp -d)
    trap 'rm -rf "$tmp"' EXIT
    collect_frontend_ids "$tmp" || exit 1
    frontend_ids="$tmp/ids.txt"
    break
  fi
done

missing=0
frontend_missing=0
total=0
for id in $ids; do
  total=$((total + 1))
  if ! has_test "$id"; then
    echo "MISSING test for scenario [$id]" >&2
    missing=$((missing + 1))
    if is_frontend_capability "$id"; then
      frontend_missing=$((frontend_missing + 1))
    fi
  fi
done

if [ "$missing" -gt 0 ]; then
  echo "$missing of $total scenarios have no test" >&2
  if [ "$frontend_missing" -gt 0 ]; then
    echo "For frontend scenarios, also look for a stray test.only: it hides other tests." >&2
  fi
  exit 1
fi
echo "OK: all $total scenarios have a test"
