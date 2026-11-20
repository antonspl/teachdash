# Proposal

## Clarifications

Decisions by the engineer for issue #166 (2026-11-20). After the first spec critique the engineer revised decisions 3, 4, 7, 8 and 10 and added decision 12; decisions 2, 6 and 9 took in that critique's clear fixes. After the second spec critique the engineer revised decisions 3 (A2, which replaces the first critique's decision A on pass 1), 5, 6, 7 and 11 (B2, own UI fixed in the PR) and 12 (C2, mixed-change routing unproven); decisions 2, 8 and 10 take in that critique's clear fixes. The revised wording replaces the earlier wording.

1. **Where the design sources live.** They are vendored as plain Markdown into `.agents/design/` (not `.agents/skills/`) and read by roles by path. No tool lists them, so the main agent, the backend `implementer` and other roles do not see them. `scripts/setup-agents` is not changed.
2. **The new role.** `.agents/roles/frontend-implementer.md`, class `medium`, access `write`, web `true`. Short body: read `.agents/roles/implementer.md` and follow it; then read `.agents/design/` in this order: `README.md`, `project-rules.md`, `checklist.md`; `accessibility.md` when the slice adds or changes interactive elements, forms or dialogs; the rest on demand; then its additions. One line says: where `implementer.md` and this file differ, this file wins. Additions:
   - build new UI to the design sources; new UI meets the project rules and WCAG 2.2 AA;
   - it may only add new role-named tokens to `tokens.css` inside its slice; changing an existing token's value, renaming it or deleting it is an engineer decision (revised after the second spec critique);
   - when its slice changes pages (`frontend/src/pages/`) or `src/ui/` components, take Playwright screenshots of each page it touches and each state the slice changes, and check them before reporting (decision 8);
   - list the screenshot paths in its report;
   - no new dependencies.

   The backend `implementer.md` is unchanged.
3. **Routing** (revised after the second spec critique, decision A2; it replaces the first critique's decision A). The main agent sends every implementation slice that touches `frontend/` to `frontend-implementer`, other slices to `implementer`. A rule in `openspec/config.yaml` `rules.tasks` splits implementation task groups by layer: an implementation group (after the test review task, or any group in a frontend-only change) touches either code under `frontend/` or code under `backend/`, never both. Repository tooling (`scripts/`, for example adding a capability to `FRONTEND_CAPABILITIES` in `scripts/check-scenario-coverage.sh`, and config files) may go with either group. This change adds no test rules for a change with both layers: its "Failing tests" section stays as today, one section written by the backend `implementer` (archived `2026-11-02-login-invalid-credentials`, task 1.5). `openspec/config.yaml` lines 27 and 38 are not changed. So the frontend-implementer works only in pass 2 of a change with both layers, or in the single pass of a frontend-only change, where its group writes its own "[<scenario ID>]" tests (`openspec/config.yaml` line 31). The frontend-only exception (one pass) stays. No change to constitution step 7 or `scripts/check-scenario-coverage.sh`. How frontend tests fit pass 1 (constitution §2 against practice, no frontend stubs, coverage by `FRONTEND_CAPABILITIES`) is #170 and out of scope.
4. **Design angle** (revised after the spec critique). For a change to UI under `frontend/src`, the main agent always proposes a design angle at Clarify (constitution §1 step 2); the engineer may drop it for a small task, as step 2 allows. At Explore only if Explore is run; Explore stays optional. A `researcher` reads `.agents/design/` and the screens involved and returns design questions (layout, states, copy, a11y, tokens and components), and existing-UI problems outside the change separately. The main agent asks the engineer. The answers go into the proposal's Clarifications and `design.md`. The angle always covers the existing screens the change touches; problems outside the change's scope go through decision 5. Routing to `frontend-implementer` stays "any slice touching `frontend/`"; screenshots are taken only when a slice changes pages or `src/ui/` components.
5. **Design and UI problems in existing UI outside the change's scope** (revised after the second spec critique). The main agent lists them to the engineer and files them as separate `area:frontend` GitHub issues only after the engineer agrees. They are never added to the current change. This applies only to design and UI findings in existing UI outside the change; every other finding follows constitution §1 step 10 unchanged.
6. **Review.**
   - The `reviewer` checks frontend slices against `.agents/design/` (project rules win) and the listed screenshots.
   - The `code-gate-reviewer` gets one bullet: check frontend diffs against `.agents/design/project-rules.md`.
   - `.agents/design/` is added to both reviewers' "no unrequested changes" list, so a frontend-implementer cannot loosen the rules to pass its own UI.
   - Findings are triaged as constitution §1 step 10 says, with the sentence of decision 7 (revised after the second spec critique, decision B2): security problems, data loss, spec violations, and design-rule and WCAG 2.2 AA violations in UI the change itself adds or changes are fixed in the PR. "UI the change adds or changes" means elements the change adds, or whose markup or CSS the change edits; touching an existing component for an unrelated reason does not pull in its existing problems (for example #168, field border contrast, stays out unless the change edits that border). Design and UI problems in existing UI outside the change go through decision 5; every other finding becomes an issue as step 10 says.
   - No permission-file change.
7. **Where the rules live** (revised after the first and the second spec critique). `AGENTS.md`, plus two constitution edits:
   - one sentence in §1 step 2: "For a change to UI under `frontend/src`, the proposed angles always include a design angle (AGENTS.md).";
   - one sentence in §1 step 10, next to "Security problems, data loss and spec violations are fixed in the PR" (decision B2): design-rule (`.agents/design/project-rules.md`) and WCAG 2.2 AA violations in UI the change adds, or whose markup or CSS it edits, are fixed in the PR too.

   The constitution edits are only steps 2 and 10; step 7 is not changed.
8. **Screenshots** (revised after the first and the second spec critique). Saved to `frontend/screenshots/<change-name>/`, which is cleared before each run, so a stale PNG cannot survive a failed run. `frontend/screenshots/` is gitignored (one `.gitignore` line); Playwright empties `test-results/` on each run, so that folder is not used. Each page the slice touches and each state the slice changes (for example the error shown after an empty submit, or a dialog open) is captured at both widths: desktop and a 360 px viewport (clear fix after the second spec critique). Method (today's practice):
   - rebuild the stack with `docker compose up --build --wait` (port 8081 serves the built app, and the Playwright `baseURL` is hardcoded to it);
   - write a temporary `frontend/e2e/zshots.spec.ts` using the `e2e/accounts.ts` helpers;
   - run `npx playwright test zshots --project=desktop-chromium`;
   - delete the spec before committing.

   The frontend-implementer's report lists the screenshot paths; the engineer attaches images to the PR by hand if wanted (`gh` cannot upload images). `docker` and `npx` are in no allow list, so each call prompts. Tool support is in decision 12.
9. **Sources and licences.** One file per source. Each has its upstream licence in `.agents/design/licenses/<owner>-<repo>.LICENSE`; anthropics/skills has no root LICENSE, so `skills/frontend-design/LICENSE.txt` @e4bb5d9 is taken. Each vendored file has a header line naming the source, path, pinned commit, licence, the licence file path in `.agents/design/licenses/` and "modified by teachdash" (Apache-2.0 §4(b)). impeccable has a `NOTICE.md`, but its notices cover none of what goes into `critique-rubric.md` (they are about iOS and Android references, fonts and consent rules), so under Apache-2.0 §4(d) no copy is included. The WCAG target is 2.2 AA; the AAA tables are dropped. The impeccable `critique-rubric.md` is taken, trimmed to about 200 lines, and read on demand by the reviewer and the design-angle researcher. The impeccable `audit.md` is skipped, but its three unique points go into the checklist in our own words: reduced motion keeps state feedback; interrupted touch gestures; `will-change`.
10. **Proof, recorded in the PR** (revised after the first and the second spec critique).
    - A dry run on #165 (ui-required-fields): Clarify and Research up to the design questions, with no spec deltas for #165; then one slice on a throwaway branch off this one. On that branch a scratch change `ui-required-fields` is created by `scripts/flow/change-start` or `openspec new change`, never by hand, so the slice has proposal Clarifications for the design answers, a minimal `tasks.md` for the slice, and a name for `frontend/screenshots/ui-required-fields/`. The run shows the design questions asked before code, the frontend-implementer's screenshot paths and the reviewer's design check, and proves the `reviewer.md` and `researcher.md` edits. The throwaway branch's last SHA is recorded in PR #169; then the branch, the scratch change and the code are deleted; no #165 code survives.
    - After all role edits, `scripts/setup-agents` generates the role for all three tools and `--check` prints `ok`; `implementer.md` is unchanged.
11. **Approval.** The role files, `openspec/config.yaml` and the constitution (§1 steps 2 and 10) need the owner's approval (CODEOWNERS). `.agents/design/`, `AGENTS.md` and `.gitignore` do not.
12. **Three tools and mixed changes** (added after the first spec critique, extended after the second, decision C2). The design is meant for Claude Code, opencode and Codex, but only proven in Claude Code by the dry run. opencode and Codex are unverified: the Codex sandbox may block `docker` or Chromium, and whether opencode and Codex can show a PNG to the reviewer is unchecked. Routing in a change with both layers (implementation groups split by layer, both roles in one change) is also unproven: #165 is frontend-only, so the dry run does not exercise it.

Already filed outside this change: #167 (a stray block in `scripts/flow/test`), #168 (field border contrast 2.5:1 against WCAG 1.4.11; the first design check will hit it, and it stays out of this change unless a change edits that border) and #170 (frontend tests in a change with both layers).

## Research

Three angles. The details are in `design.md`.

- **Which sources to vendor, at which commit, and what to cut.** Five sources pinned by commit: addyosmani/web-quality-skills (MIT), wondelai/skills (MIT), vercel-labs/web-interface-guidelines (MIT), anthropics/skills (Apache-2.0) and pbakaus/impeccable (Apache-2.0). Each is trimmed to what fits a Vite SPA with our tokens and `src/ui/` components: no Tailwind, no network tools, no AAA, no new dependencies. Total about 1,350 lines (about 14k tokens); a slice always reads the index, the project rules and the checklist (about 3k tokens). Design D1.
- **Where the sources contradict our rules.** Fifteen conflicts (C0-C14), for example Title Case against our sentence case, uppercase-small labels, web fonts, raw px and hex values, and `autocomplete="off"`. All are resolved in `project-rules.md`, which comes first and wins. Design D2.
- **How the role is generated and allowed.** `scripts/setup-agents` copies a role body verbatim into all three tools and gives write roles no tool limits. The body must not contain three single quotes. `--check` hashes only `.agents/roles` and `.agents/skills`, so edits to `.agents/design/` need no regeneration. Every `*.md` in `.agents/roles/` is treated as a role, so a shared include cannot live there. The permission files do not list `docker` or `npx`, so each call prompts; no permission change is made. Frontend-only changes have one pass today (`openspec/config.yaml`; constitution step 7 says backend tests). In a change with both layers, a frontend test may already sit in the one "Failing tests" section (login-invalid-credentials, task 1.5); how that fits the rules is #170. Design D3, D9.

## Why

Frontend work should come out well designed, not just working. Today no role carries design guidance, design questions are not asked before frontend code is written, and the house rules for UI copy and spacing are recorded only in archived changes and memory.

## What Changes

- **`.agents/design/`:** five vendored and adapted design sources with their licences, an index, and `project-rules.md`, which comes first and wins over every source.
- **New role `frontend-implementer`:** follows `implementer.md`, reads the design sources, builds UI to them, takes and checks screenshots when a slice changes pages or `src/ui/` components, lists them in its report.
- **Routing:** implementation slices that touch `frontend/` go to `frontend-implementer`, the rest to `implementer`. `openspec/config.yaml` gets one tasks rule: an implementation group touches either `frontend/` or `backend/` code, never both. No test rules change.
- **Design angle:** for a change to UI under `frontend/src`, the main agent proposes a design angle at Clarify; the engineer may drop it for a small task. Out-of-scope design and UI problems in existing UI become separate issues after the engineer agrees. `researcher.md` gets the design-angle rule.
- **Review:** `reviewer.md` checks frontend slices against the design sources and the screenshots. `code-gate-reviewer.md` checks frontend diffs against the project rules. Both guard `.agents/design/` against unrequested changes.
- **Constitution §1:** one sentence in step 2 (design angle) and one in step 10 (design-rule and WCAG 2.2 AA violations in UI the change adds or changes are fixed in the PR).
- **`.gitignore`:** `frontend/screenshots/`.

## Out of Scope

- Changes to `scripts/setup-agents`, `scripts/flow/`, `scripts/check-scenario-coverage.sh`, the permission files and `.agents/roles/implementer.md`.
- Constitution step 7, and test rules for a change with both layers (`openspec/config.yaml` lines 27 and 38): frontend tests in pass 1 are #170.
- New dependencies, a screenshot script, or a Playwright project for screenshots.
- The impeccable `audit.md` and the sources' other references (beyond the three audit points in the checklist).
- Fixing existing UI that the change does not add or change, including #168. Design and UI problems in existing UI found by the dry run become issues after the engineer agrees.
- Any #165 code or spec; the dry run's branch is deleted.
- Proving the role in opencode and Codex, and proving routing in a change with both layers.
- #167.
- Checking the design sources or the routing by a script or in CI.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is tooling with no product behaviour change, so `.openspec.yaml` sets `skip_specs: true`.

## Impact

- **New:** `.agents/design/` (index, project rules, eight adapted source files, five licences), `.agents/roles/frontend-implementer.md`.
- **Changed:** `.agents/roles/reviewer.md`, `.agents/roles/researcher.md`, `.agents/roles/code-gate-reviewer.md`, `AGENTS.md`, `openspec/constitution.md` §1 steps 2 and 10, `openspec/config.yaml` (`rules.tasks`, one rule), `.gitignore`.
- **Owner approval:** the role files, `openspec/config.yaml` and the constitution §1 steps 2 and 10 (constitution §1, CODEOWNERS).
- **Process:** after the merge, each developer runs `scripts/setup-agents` and restarts the session so the new role appears.
