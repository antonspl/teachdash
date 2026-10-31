#!/usr/bin/env bash
# Fails if a spec scenario has no test with @DisplayName("[<id>] ...") in backend/src/test.
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

missing=0
total=0
for id in $ids; do
  total=$((total + 1))
  if ! grep -r -F -q "@DisplayName(\"[$id]" backend/src/test 2>/dev/null; then
    echo "MISSING test for scenario [$id]" >&2
    missing=$((missing + 1))
  fi
done

if [ "$missing" -gt 0 ]; then
  echo "$missing of $total scenarios have no test" >&2
  exit 1
fi
echo "OK: all $total scenarios have a test"
