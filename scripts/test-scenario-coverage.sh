#!/usr/bin/env bash
# Tests the spec phase rule of check-scenario-coverage.sh on fixture trees (backend
# capabilities only, so no npm is needed). Each case copies the script into a temp tree,
# because the script reads the tree above its own location.
set -uo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

failures=0
cases=0

# new_tree: creates an empty fixture tree and sets $tree.
new_tree() {
  tree=$(mktemp -d "$work/tree.XXXXXX")
  mkdir -p "$tree/scripts" "$tree/openspec/specs" "$tree/openspec/changes" "$tree/backend/src/test"
  cp "$here/check-scenario-coverage.sh" "$tree/scripts/"
}

# write_spec <file> <section> <requirement> <id>...
write_spec() {
  local file="$1" section="$2" requirement="$3" id
  shift 3
  mkdir -p "$(dirname "$file")"
  {
    echo "## $section"
    echo "### Requirement: $requirement"
    for id in "$@"; do
      echo "#### Scenario: $id something"
    done
  } >>"$file"
}

# write_test <id-prefix text>: a backend test whose @DisplayName starts with the given text.
write_test() {
  printf '@DisplayName("%s something")\n' "$1" >>"$tree/backend/src/test/T.java"
}

# write_tasks <change> <line>
write_tasks() {
  mkdir -p "$tree/openspec/changes/$1"
  echo "$2" >"$tree/openspec/changes/$1/tasks.md"
}

# expect <name> <expected exit code> <expected output lines...>
expect() {
  local name="$1" want_rc="$2" out rc want
  shift 2
  want=$(printf '%s\n' "$@")
  out=$(COVERAGE_STRICT=$STRICT bash "$tree/scripts/check-scenario-coverage.sh" 2>&1)
  rc=$?
  cases=$((cases + 1))
  if [ "$out" = "$want" ] && [ "$rc" -eq "$want_rc" ]; then
    echo "ok   $name"
  else
    failures=$((failures + 1))
    echo "FAIL $name (exit $rc, want $want_rc)"
    echo "--- want"
    echo "$want"
    echo "--- got"
    echo "$out"
  fi
}

c=openspec/changes
STRICT=

new_tree
write_spec "$tree/openspec/specs/cap/spec.md" "Requirements" "Old" "cap/R1-S1"
write_test "[cap/R1-S1]"
write_spec "$tree/$c/c1/specs/cap/spec.md" "ADDED Requirements" "New" "cap/R2-S1"
write_tasks c1 "- [ ] 1.1 Write tests"
expect "spec phase: new scenario without test is pending" 0 \
  "PENDING cap/R2-S1 (spec phase: c1)" \
  "OK: 1 of 2 scenarios have a test, 1 pending (spec phase)"

new_tree
write_spec "$tree/openspec/specs/cap/spec.md" "Requirements" "Old" "cap/R1-S1"
write_test "[cap/R1-S1]"
write_spec "$tree/$c/c1/specs/cap/spec.md" "ADDED Requirements" "New" "cap/R2-S1"
write_tasks c1 "$(printf -- '- [ ] 1.1 Write tests\n- [x] 1.2 Done')"
expect "a ticked task makes the change strict" 1 \
  "MISSING test for scenario [cap/R2-S1]" \
  "1 of 2 scenarios have no test"

new_tree
write_spec "$tree/openspec/specs/cap/spec.md" "Requirements" "Old" "cap/R1-S1"
write_test "[cap/R1-S1]"
write_spec "$tree/$c/c1/specs/cap/spec.md" "ADDED Requirements" "New" "cap/R2-S1" "cap/R2-S2"
write_test "[cap/R2-S1]"
write_tasks c1 "- [ ] 1.1 Write tests"
expect "a test for one new scenario makes the change strict" 1 \
  "MISSING test for scenario [cap/R2-S2]" \
  "1 of 3 scenarios have no test"

new_tree
write_spec "$tree/openspec/specs/cap/spec.md" "Requirements" "Old" "cap/R1-S1"
write_test "[cap/R1-S1]"
write_spec "$tree/$c/c1/specs/cap/spec.md" "ADDED Requirements" "New" "cap/R2-S1"
expect "no tasks.md makes the change strict" 1 \
  "MISSING test for scenario [cap/R2-S1]" \
  "1 of 2 scenarios have no test"

new_tree
write_spec "$tree/openspec/specs/cap/spec.md" "Requirements" "Old" "cap/R1-S1"
write_spec "$tree/$c/c1/specs/cap/spec.md" "MODIFIED Requirements" "Old" "cap/R1-S1" "cap/R1-S2"
write_tasks c1 "- [ ] 1.1 Write tests"
expect "MODIFIED delta: the old ID stays strict, the new one is pending" 1 \
  "MISSING test for scenario [cap/R1-S1]" \
  "PENDING cap/R1-S2 (spec phase: c1)" \
  "1 of 2 scenarios have no test"

new_tree
write_spec "$tree/openspec/specs/cap/spec.md" "Requirements" "Gone" "cap/R1-S1"
write_spec "$tree/openspec/specs/cap/spec.md" "Requirements" "Kept" "cap/R2-S1"
mkdir -p "$tree/$c/c1/specs/cap"
printf '## REMOVED Requirements\n### Requirement: Gone\n' >"$tree/$c/c1/specs/cap/spec.md"
write_tasks c1 "- [ ] 1.1 Remove"
expect "REMOVED-only change: removed scenario needs no test, others stay strict" 1 \
  "MISSING test for scenario [cap/R2-S1]" \
  "1 of 1 scenarios have no test"

new_tree
write_spec "$tree/$c/a/specs/capa/spec.md" "ADDED Requirements" "New" "capa/R1-S1"
write_tasks a "- [ ] 1.1 Write tests"
write_spec "$tree/$c/b/specs/capb/spec.md" "ADDED Requirements" "New" "capb/R1-S1"
write_tasks b "- [x] 1.1 Write tests"
expect "two changes: the phase is decided per change" 1 \
  "PENDING capa/R1-S1 (spec phase: a)" \
  "MISSING test for scenario [capb/R1-S1]" \
  "1 of 2 scenarios have no test"

new_tree
write_spec "$tree/openspec/specs/cap/spec.md" "Requirements" "Old" "cap/R1-S1"
write_test "[cap/R1-S1]"
write_spec "$tree/$c/c1/specs/cap/spec.md" "ADDED Requirements" "New" "cap/R2-S1"
write_test "[cap/R2_S1]"
write_tasks c1 "- [x] 1.1 Tests"
expect "pass 2 with a typo in the @DisplayName prefix" 1 \
  "MISSING test for scenario [cap/R2-S1]" \
  "1 of 2 scenarios have no test"

new_tree
write_spec "$tree/openspec/specs/cap/spec.md" "Requirements" "Old" "cap/R1-S1"
write_spec "$tree/$c/c1/specs/cap/spec.md" "ADDED Requirements" "New" "cap/R2-S1"
write_tasks c1 "- [ ] 1.1 Write tests"
expect "main spec scenario without test stays strict" 1 \
  "MISSING test for scenario [cap/R1-S1]" \
  "PENDING cap/R2-S1 (spec phase: c1)" \
  "1 of 2 scenarios have no test"

new_tree
write_spec "$tree/$c/c1/specs/cap/spec.md" "REMOVED Requirements" "Gone" "cap/R9-S1"
write_tasks c1 "- [ ] 1.1 Remove"
expect "scenarios in a REMOVED section are not new" 1 \
  "MISSING test for scenario [cap/R9-S1]" \
  "1 of 1 scenarios have no test"

new_tree
write_spec "$tree/$c/a/specs/cap/spec.md" "ADDED Requirements" "New" "cap/R1-S1"
write_tasks a "- [ ] 1.1 Write tests"
write_spec "$tree/$c/b/specs/cap/spec.md" "ADDED Requirements" "New" "cap/R1-S1"
write_tasks b "- [x] 1.1 Write tests"
expect "an ID new in a strict change is not pending" 1 \
  "MISSING test for scenario [cap/R1-S1]" \
  "1 of 1 scenarios have no test"

new_tree
write_spec "$tree/$c/c1/specs/cap/spec.md" "ADDED Requirements" "New" "cap/R1-S1"
write_tasks c1 "$(printf -- '- [ ] 1.1 Write tests\n  - [X] 1.2 Done')"
expect "an indented capital X tick makes the change strict" 1 \
  "MISSING test for scenario [cap/R1-S1]" \
  "1 of 1 scenarios have no test"

new_tree
write_spec "$tree/$c/c1/specs/cap/spec.md" "ADDED Requirements" "New" "cap/R1-S1"
write_tasks c1 "$(printf -- '* [ ] 1.1 Write tests\n* [x] 1.2 Done')"
expect "a * bullet tick makes the change strict" 1 \
  "MISSING test for scenario [cap/R1-S1]" \
  "1 of 1 scenarios have no test"

new_tree
write_spec "$tree/openspec/specs/cap/spec.md" "Requirements" "Old" "cap/R1-S1"
write_test "[cap/R1-S1]"
write_spec "$tree/$c/c1/specs/cap/spec.md" "ADDED Requirements" "New" "cap/R2-S1"
write_test "[cap/R2-S1]"
write_tasks c1 "- [x] 1.1 Tests"
expect "all scenarios covered" 0 "OK: all 2 scenarios have a test"

new_tree
write_spec "$tree/openspec/specs/cap/spec.md" "Requirements" "Old" "cap/R1-S1"
write_test "[cap/R1-S1]"
write_spec "$tree/$c/c1/specs/cap/spec.md" "ADDED Requirements" "New" "cap/R2-S1"
write_tasks c1 "- [ ] 1.1 Write tests"
STRICT=1
expect "strict mode: a spec-phase scenario is missing" 1 \
  "MISSING test for scenario [cap/R2-S1] (change c1 is still in the spec phase)" \
  "1 of 2 scenarios have no test"
STRICT=

echo "$cases cases, $failures failed"
[ "$failures" -eq 0 ]
