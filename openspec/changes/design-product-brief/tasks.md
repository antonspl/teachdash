# Tasks

This is a tooling change with `skip_specs: true`: no "Failing tests" section and no test review stop. Each task says how it is proven; the proof is recorded in the PR for the code gate. Every `openspec` call uses `npx -y @fission-ai/openspec@1.15.0`. Base for the checks below: the commit that adds this change's artifacts (`<base>`), or `main` once the branch is rebased.

## 1. Write the product brief

- [x] 1.1 Write `.agents/design/product-brief.md` from the text between the BRIEF START and BRIEF END markers of `design.md` (decision D1, with the principles as the engineer agreed them at the spec gate; the "proposed" note on the principles is not carried into `product-brief.md`), replacing the stub entirely. Proof: the file no longer contains "This file is filled by #171"; `diff` of the file against the marked block of `design.md` shows no difference other than the markers.
- [x] 1.2 Check the file follows the sections of `.agents/design/product-brief-template.md`: `# Product`, then Users, Product Purpose, Positioning, Operating Context, Capabilities and Constraints, Evidence on Hand, Product Principles, Accessibility & Inclusion, in that order. Proof: `grep -n '^#' .agents/design/product-brief.md` is pasted in the PR.
- [x] 1.3 Check the file has no visual decisions or CSS values and no invented facts: no colours, font names, sizes, radii, shadows or layout concepts; no customers, testimonials, benchmarks, pricing or deployment claims; every number and limit traces to a cited source or to "(engineer, 2026-11-22)". Proof: `grep -oiE '#[0-9a-fA-F]{3,8}\b' .agents/design/product-brief.md | grep -vE '^#[0-9]+$'` prints nothing, and `grep -niE '[0-9]+ ?(CSS )?(px|rem|em)\b|palette|shadow|radius|typeface'` prints only two lines: the Operating Context line with "360 CSS px", and the personality line "calm, not playful", which matches on "palette" because it defers every visual choice to `design-code.md`. The reviewer also checks by hand that nothing states a visual decision (palette, type, layout concepts, chrome) beyond the one allowed personality line "calm, not playful", and checks each fact against its source.
- [x] 1.4 Check only `product-brief.md` changed outside the change folder, and the README still lists `product-brief.md` correctly. Proof: `git diff --name-only <base>..HEAD` lists only `.agents/design/product-brief.md` and `openspec/changes/design-product-brief/**` (and the task ticks), and, after task 2.2, `openspec/changes/archive/**`; `.agents/design/README.md` is unchanged and its sources table row for `product-brief.md` is intact.
- [x] 1.5 Check `design-code.md` is untouched and still a stub. Proof: `git diff <base>..HEAD -- .agents/design/design-code.md` is empty.

## 2. Workflow follow-up

- [x] 2.1 Code review gate: run the `code-gate-reviewer` on the PR; the engineer approves the code gate.
- [x] 2.2 Archive the change in the same PR (`openspec archive`; nothing merges into `openspec/specs/`).
- [x] 2.3 Mark the PR ready for the final code owner review.

## 3. Fold in the code owner's final-review comments

Added after the restore commit (bbbf9cf); proposal decisions 14-19. Groups 1 and 2 above stay as done history. Base for the checks: `bbbf9cf`.

- [x] 3.1 Rewrite `.agents/design/product-brief.md` from the text between the BRIEF START and BRIEF END markers of the updated `design.md`. Proof: `diff` of the file against the marked block (markers removed) shows no difference.
- [x] 3.2 Check the headings are unchanged. Proof: `grep -n '^#' .agents/design/product-brief.md` prints the same nine headings in the same order as before (task 1.2): `# Product`, Users, Product Purpose, Positioning, Operating Context, Capabilities and Constraints, Evidence on Hand, Product Principles, Accessibility & Inclusion.
- [x] 3.3 Re-run the 1.3 checks. Proof: `grep -oiE '#[0-9a-fA-F]{3,8}\b' .agents/design/product-brief.md | grep -vE '^#[0-9]+$'` prints nothing; `grep -niE '[0-9]+ ?(CSS )?(px|rem|em)\b|palette|shadow|radius|typeface' .agents/design/product-brief.md` prints exactly two lines: the Operating Context Devices line (matching "360 CSS px") and the personality line ("calm, not playful", matching "palette"). The reviewer checks by hand that the new Product Purpose lines (sharing, schedule) and the Teachers line trace to §8, §9, §3, #14 or "(code owner, 2026-11-22)".
- [x] 3.4 Check the old wording is gone and the new is in. Proof: `grep -niE 'language teachers|phone-first|content first' .agents/design/product-brief.md` prints nothing; `grep -n 'Domain first' .agents/design/product-brief.md` prints exactly one line (principle 1).
- [x] 3.5 Check only `product-brief.md` and the change folder changed since the restore commit, and `design-code.md` is untouched. Proof: `git diff --name-only bbbf9cf..HEAD` lists only `.agents/design/product-brief.md` and files under `openspec/changes/**` (the change folder, or `openspec/changes/archive/` after task 4.2); `git diff bbbf9cf..HEAD -- .agents/design/design-code.md` is empty.
- [x] 3.6 Code-gate fixes (decisions 24-25): re-sync `.agents/design/product-brief.md` from the updated block (the "Built now", "Planned" and exercise-types lines of Capabilities and Constraints), then re-run proofs 3.1-3.5 with the same expected output (the headings and the "360 CSS px" and "calm, not playful" lines are unchanged; no new match for the 3.3 grep). The reviewer checks each "Built now" item against `openspec/specs/` and the screens against `frontend/src/App.tsx`.

## 4. Workflow follow-up (second round)

- [ ] 4.1 Code review gate: run the `code-gate-reviewer` on the PR; the engineer approves the code gate (the spec gate for this round's artifacts is approved first).
- [ ] 4.2 Archive the change again in the same PR (`openspec archive`; nothing merges into `openspec/specs/`).
- [ ] 4.3 Mark the PR ready for the final code owner review.
- [ ] 4.4 Reply to each of Anton's comments (4659260401, 4659265790, 4659270344, 4659274680, 4659295751), mapping each to the commit that fixes it.
