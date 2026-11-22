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
