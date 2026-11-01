#!/usr/bin/env bash
# Fails if a spec scenario has no test. A scenario is covered by either:
#   - @DisplayName("[<id>] ...") in backend/src/test, or
#   - a frontend test(...) or it(...) call whose title string starts with "[<id>]", in
#     frontend/src/**/*.test.ts(x) or frontend/e2e/**/*.spec.ts. The title may be on the
#     line after the call and use ', " or `. Comments, test.skip(, test.fixme(, tests inside a
#     describe.skip or describe.fixme block (also test.describe.skip, nested blocks
#     too) and an ID elsewhere in the title do not count. test.describe wrappers and runtime
#     test.skip(condition) inside a test are fine, the test( call itself is what counts.
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

# One perl process reads all frontend test files and prints the IDs of real test calls.
# find fails when a directory is missing; that is not an error, so it is tolerated here.
frontend_files=$( { find frontend/src \( -name '*.test.ts' -o -name '*.test.tsx' \) 2>/dev/null || true
                   find frontend/e2e -name '*.spec.ts' 2>/dev/null || true; } )
frontend_ids=""
if [ -n "$frontend_files" ]; then
  if ! command -v perl >/dev/null 2>&1; then
    echo "perl is required to read the frontend tests but was not found" >&2
    exit 1
  fi
  # shellcheck disable=SC2086
  if ! frontend_ids=$(perl -0777 -ne '
    # Blank out comments; string literals are matched first so "//" inside them is kept.
    s{(\x27(?:\\.|[^\x27\\\n])*\x27|"(?:\\.|[^"\\\n])*"|`(?:\\.|[^`\\])*`)|//[^\n]*|/\*.*?\*/}{defined $1 ? $1 : " "}gse;
    # Blank out whole describe.skip / describe.fixme blocks: a balanced (...) after the
    # marker, skipping string literals. If a block cannot be balanced, stop instead of
    # counting tests that may be skipped.
    my $balanced = qr{(\((?:[^()\x27"`]++|\x27(?:\\.|[^\x27\\\n])*+\x27|"(?:\\.|[^"\\\n])*+"|`(?:\\.|[^`\\])*+`|(?-1))*+\))}s;
    my $marker = qr{(?<![\w.\$])(?:test\.)?describe(?:\.\w+)*\.(?:skip|fixme)\s*\(};
    my $found = () = /$marker/g;
    my $blanked = s{(?<![\w.\$])((?:test\.)?describe(?:\.\w+)*\.(?:skip|fixme))\s*$balanced}{my $t = $&; $t =~ s/[^\n]/ /g; $t}ge;
    die "$ARGV: cannot find the end of a describe.skip block\n" if $blanked < $found;
    print "$2\n" while /(?<![\w.\$])(?:test|it)\s*\(\s*([\x27"`])\[([^\]\s]+)\]/g;
  ' $frontend_files | sort -u); then
    echo "Reading the frontend tests with perl failed (see the message above)" >&2
    exit 1
  fi
fi

has_frontend_test() {
  grep -F -x -q -- "$1" <<<"$frontend_ids"
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
