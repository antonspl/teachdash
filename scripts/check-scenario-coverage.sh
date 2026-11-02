#!/usr/bin/env bash
# Fails if a spec scenario has no test. A plain text match, nothing is parsed: skipped or
# commented-out tests still count. CI runs this script (constitution §2); under #7 only the
# frontend text match is planned to be replaced by `vitest list --json` and
# `playwright test --list`. A scenario is covered by:
#   - @DisplayName("[<id>] in backend/src/test, or
#   - a string starting with [<id>] (quote ', " or ` right before it) in
#     frontend/src/**/*.test.ts(x) or frontend/e2e/**/*.spec.ts.
# Scenario IDs come from "#### Scenario: <capability>/R<n>-S<m> ..." headings in
# openspec/specs/**/spec.md and openspec/changes/*/specs/**/spec.md (archive excluded).
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"

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

has_backend_test() {
  grep -r -F -q "@DisplayName(\"[$1]" backend/src/test 2>/dev/null
}

# Missing frontend directories are fine: grep fails, so the ID counts as missing.
has_frontend_test() {
  grep -r -F -q -e "'[$1]" -e "\"[$1]" -e "\`[$1]" \
    --include='*.test.ts' --include='*.test.tsx' frontend/src 2>/dev/null ||
  grep -r -F -q -e "'[$1]" -e "\"[$1]" -e "\`[$1]" \
    --include='*.spec.ts' frontend/e2e 2>/dev/null
}

missing=0
total=0
for id in $ids; do
  total=$((total + 1))
  if ! has_backend_test "$id" && ! has_frontend_test "$id"; then
    echo "MISSING test for scenario [$id]" >&2
    missing=$((missing + 1))
  fi
done

if [ "$missing" -gt 0 ]; then
  echo "$missing of $total scenarios have no test" >&2
  exit 1
fi
echo "OK: all $total scenarios have a test"
