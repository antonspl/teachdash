# shellcheck shell=bash
# Shared helpers for the flow scripts. Sourced, not executed.

# shellcheck disable=SC2034
MARKER='<!-- flow -->'

die() {
  printf '%s: %s\n' "$(basename "$0")" "$*" >&2
  exit 1
}

# Refuses to run on a detached HEAD or on main.
guard_branch() {
  local branch
  branch=$(git symbolic-ref --short HEAD 2>/dev/null) || die "detached HEAD, check out the change branch first"
  [ "$branch" != main ] || die "on main, switch to the change branch first"
  printf '%s\n' "$branch"
}

# Maps a gate name to its label in the PR description.
gate_label() {
  case "$1" in
    spec) echo Spec ;;
    tests) echo Tests ;;
    code) echo Code ;;
    *) die "unknown gate '$1' (spec, tests or code)" ;;
  esac
}

# Reads a PR description on stdin and prints it with the "- <label>: <range>"
# line under "## Review ranges" replaced, or added after the section's last
# non-blank line. Fails if the heading is missing.
set_range_line() {
  awk -v label="$1" -v range="$2" '
    { sub(/\r$/, "") }
    function flush_pending() {
      for (i = 1; i <= nblank; i++) print ""
      nblank = 0
    }
    /^## / {
      if (insec && !done) { print line; done = 1; flush_pending() }
      insec = ($0 == "## Review ranges")
      if (insec) found = 1
      flush_pending()
      print
      next
    }
    insec {
      if ($0 ~ "^- " label ":") { flush_pending(); print "- " label ": " range; done = 1; next }
      if ($0 == "") { nblank++; next }
      flush_pending()
      print
      next
    }
    { print }
    BEGIN { line = "- " label ": " range }
    END {
      if (!found) exit 3
      if (insec && !done) print line
    }
  ' || die "no '## Review ranges' heading in the PR description"
}
