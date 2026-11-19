# shellcheck shell=bash
# Shared helpers for the flow scripts. Sourced, not executed.

# shellcheck disable=SC2034
MARKER='<!-- flow -->'

die() {
  printf '%s: %s\n' "$(basename "$0")" "$*" >&2
  exit 1
}

# Validates a change name (kebab-case).
require_name() {
  [[ $1 =~ ^[a-z0-9][a-z0-9-]*$ ]] || die "name must be kebab-case"
}

# Validates a number argument; $2 names it in the error.
require_number() {
  [[ $1 =~ ^[0-9]+$ ]] || die "$2 must be a number"
}

# Refuses to run on a detached HEAD or on main.
guard_branch() {
  local branch
  branch=$(git symbolic-ref --short HEAD 2>/dev/null) || die "detached HEAD, check out the change branch first"
  [ "$branch" != main ] || die "on main, switch to the change branch first"
  printf '%s\n' "$branch"
}

# Prints the upstream ref (for example origin/my-change) of the current branch
# after fetching that one branch. Returns 1 without upstream, 2 if the fetch fails.
fetch_upstream() {
  local branch up remote merge
  branch=$(git symbolic-ref --short HEAD 2>/dev/null) || return 1
  up=$(git rev-parse --abbrev-ref --symbolic-full-name '@{upstream}' 2>/dev/null) || return 1
  remote=$(git config "branch.$branch.remote") || return 1
  merge=$(git config "branch.$branch.merge") || return 1
  git fetch -q "$remote" "+$merge:refs/remotes/$up" >/dev/null 2>&1 || return 2
  printf '%s\n' "$up"
}

# Requires an upstream, HEAD equal to it and no uncommitted changes to tracked
# files. Prints the upstream ref. Call after guard_branch.
guard_synced() {
  local up counts ahead behind rc=0
  up=$(fetch_upstream) || rc=$?
  [ "$rc" -ne 1 ] || die "the branch has no upstream, push it first (git push -u)"
  [ "$rc" -eq 0 ] || die "could not fetch the upstream branch"
  counts=$(git rev-list --left-right --count "HEAD...$up")
  read -r ahead behind <<<"$counts"
  [ "$ahead" = 0 ] && [ "$behind" = 0 ] || die "the branch differs from $up: $ahead unpushed, $behind behind"
  git diff --quiet HEAD -- || die "uncommitted changes to tracked files, commit or stash them first"
  printf '%s\n' "$up"
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
    BEGIN { line = "- " label ": " range }
    function flush_pending() {
      for (i = 1; i <= nblank; i++) print ""
      nblank = 0
    }
    { sub(/\r$/, "") }
    /^## / {
      if (insec && !done) { print line; done = 1 }
      flush_pending()
      insec = ($0 == "## Review ranges")
      if (insec) found = 1
      print
      next
    }
    insec {
      if ($0 ~ "^- " label ":") { flush_pending(); print line; done = 1; next }
      if ($0 == "") { nblank++; next }
      flush_pending()
      print
      next
    }
    { print }
    END {
      if (!found) exit 3
      if (insec && !done) print line
      flush_pending()
    }
  ' || die "no '## Review ranges' heading in the PR description"
}
