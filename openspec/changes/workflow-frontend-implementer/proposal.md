# Proposal

## Clarifications

Decisions by the engineer for issue #166 (2026-11-20):

1. **Where the design sources live.** They are vendored as plain Markdown into `.agents/design/` (not `.agents/skills/`) and read by roles by path. No tool lists them, so the main agent, the backend `implementer` and other roles do not see them. `scripts/setup-agents` is not changed.
2. **The new role.** `.agents/roles/frontend-implementer.md`, class `medium`, access `write`, web `true`. Short body: read `.agents/roles/implementer.md` and follow it; then read `.agents/design/` (the `README.md` index, `project-rules.md` first, then `checklist.md`; `accessibility.md` for new UI; the rest on demand); then its additions. One line says: where `implementer.md` and this file differ, this file wins. Additions:
   - build new UI to the design sources;
   - it may edit `tokens.css` inside its slice; new values become role-named tokens;
   - take Playwright screenshots of the pages its slice touches and check them before reporting;
   - list the screenshot paths in its report;
   - no new dependencies.

   The backend `implementer.md` is unchanged.
3. **Routing.** The main agent sends every slice that touches `frontend/` to `frontend-implementer`, other slices to `implementer`. A rule in `openspec/config.yaml` `rules.tasks`: task groups are split by layer; a group touches either `frontend/` or the rest, never both.
4. **Design angle.** For every change that touches the frontend, explore, research and propose include a design angle. A `researcher` reads `.agents/design/` and the screens involved and returns design questions (layout, states, copy, a11y, tokens and components), and existing-UI problems outside the change separately. The main agent asks the engineer. The answers go into the proposal's Clarifications and `design.md`. From the next frontend change on, the angle also covers the existing screens the change touches.
5. **Design problems in existing UI outside the change's scope.** The main agent lists them to the engineer and files them as separate `area:frontend` GitHub issues only after the engineer agrees. They are never added to the current change.
6. **Review.**
   - The `reviewer` checks frontend slices against `.agents/design/` (project rules win) and the listed screenshots.
   - The `code-gate-reviewer` gets one bullet: check frontend diffs against `.agents/design/project-rules.md`.
   - `.agents/design/` is added to both reviewers' "no unrequested changes" list, so a frontend-implementer cannot loosen the rules to pass its own UI.
   - No permission-file change.
7. **Where the rules live.** `AGENTS.md`, plus a minimal edit to constitution §1:
   - step 2 gets one sentence: "A change that touches the frontend always has a design angle (AGENTS.md).";
   - step 7 gets a note that a change's frontend tests land with the code in pass 2 (as for frontend-only changes today, §2), including changes that have both layers.
8. **Screenshots.** Saved to a new gitignored `frontend/screenshots/` (one `.gitignore` line). This works in Claude Code, opencode and Codex, and Playwright empties `test-results/` on each run. Widths: desktop plus a 360 px viewport. Method (today's practice):
   - rebuild the stack with `docker compose up --build --wait` (port 8081 serves the built app, and the Playwright `baseURL` is hardcoded to it);
   - write a temporary `frontend/e2e/zshots.spec.ts` using the `e2e/accounts.ts` helpers;
   - run `npx playwright test zshots --project=desktop-chromium`;
   - delete the spec before committing.
9. **Sources and licences.** One file per source. Each has its upstream licence in `.agents/design/licenses/<owner>-<repo>.LICENSE` and a header line naming the source, path, pinned commit, licence and "modified by teachdash" (Apache-2.0 §4(b)). No NOTICE files are needed. The WCAG target is 2.2 AA; the AAA tables are dropped. The impeccable `critique-rubric.md` is taken, trimmed to about 200 lines, and read on demand by the reviewer and the design-angle researcher. The impeccable `audit.md` is skipped, but its three unique points go into the checklist in our own words: reduced motion keeps state feedback; interrupted touch gestures; `will-change`.
10. **Proof, recorded in the PR.**
    - A dry run on a real small frontend issue (for example #165 ui-required-fields) up to the design questions and one slice. It shows the design questions asked before code, the frontend-implementer's screenshots and the reviewer's design check.
    - `scripts/setup-agents` generates the role for all three tools and `--check` prints `ok`; `implementer.md` is unchanged.
11. **Approval.** The role files, `openspec/config.yaml` and the constitution need the owner's approval (CODEOWNERS). `.agents/design/`, `AGENTS.md` and `.gitignore` do not.

Already filed outside this change: #167 (a stray block in `scripts/flow/test`) and #168 (field border contrast 2.5:1 against WCAG 1.4.11; the first design check will hit it, and it stays out of this change).

## Research

Three angles. The details are in `design.md`.

- **Which sources to vendor, at which commit, and what to cut.** Five sources pinned by commit: addyosmani/web-quality-skills (MIT), wondelai/skills (MIT), vercel-labs/web-interface-guidelines (MIT), anthropics/skills (Apache-2.0) and pbakaus/impeccable (Apache-2.0). Each is trimmed to what fits a Vite SPA with our tokens and `src/ui/` components: no Tailwind, no network tools, no AAA, no new dependencies. Total about 1,350 lines (about 14k tokens); a slice always reads the index, the project rules and the checklist (about 3k tokens). Design D1.
- **Where the sources contradict our rules.** Fifteen conflicts (C0-C14), for example Title Case against our sentence case, uppercase-small labels, web fonts, raw px and hex values, and `autocomplete="off"`. All are resolved in `project-rules.md`, which comes first and wins. Design D2.
- **How the role is generated and allowed.** `scripts/setup-agents` copies a role body verbatim into all three tools and gives write roles no tool limits. The body must not contain three single quotes. `--check` hashes only `.agents/roles` and `.agents/skills`, so edits to `.agents/design/` need no regeneration. Every `*.md` in `.agents/roles/` is treated as a role, so a shared include cannot live there. The permission files do not list `docker` or `npx`, so they need no change. Frontend-only changes have one pass today (`openspec/config.yaml`, constitution step 7 says backend tests). Design D3.

## Why

Frontend work should come out well designed, not just working. Today no role carries design guidance, design questions are not asked before frontend code is written, and the house rules for UI copy and spacing are recorded only in archived changes and memory.

## What Changes

- **`.agents/design/`:** five vendored and adapted design sources with their licences, an index, and `project-rules.md`, which comes first and wins over every source.
- **New role `frontend-implementer`:** follows `implementer.md`, reads the design sources, builds UI to them, takes and checks screenshots, lists them in its report.
- **Routing:** frontend slices go to `frontend-implementer`, the rest to `implementer`. `openspec/config.yaml` gets a tasks rule that splits task groups by layer.
- **Design angle:** frontend changes get a design angle at explore, research and propose. Out-of-scope problems in existing UI become separate issues after the engineer agrees. `researcher.md` gets the design-angle rule.
- **Review:** `reviewer.md` checks frontend slices against the design sources and the screenshots. `code-gate-reviewer.md` checks frontend diffs against the project rules. Both guard `.agents/design/` against unrequested changes.
- **Constitution §1:** one sentence in step 2 and a note in step 7.
- **`.gitignore`:** `frontend/screenshots/`.

## Out of Scope

- Changes to `scripts/setup-agents`, `scripts/flow/`, the permission files and `.agents/roles/implementer.md`.
- New dependencies, a screenshot script, or a Playwright project for screenshots.
- The impeccable `audit.md` and the sources' other references (beyond the three audit points in the checklist).
- Fixing existing UI, including #168. Problems found by the dry run become issues.
- #167.
- Checking the design sources or the routing by a script or in CI.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is tooling with no product behaviour change, so `.openspec.yaml` sets `skip_specs: true`.

## Impact

- **New:** `.agents/design/` (index, project rules, eight adapted source files, five licences), `.agents/roles/frontend-implementer.md`.
- **Changed:** `.agents/roles/reviewer.md`, `.agents/roles/researcher.md`, `.agents/roles/code-gate-reviewer.md`, `AGENTS.md`, `openspec/constitution.md` §1, `openspec/config.yaml`, `.gitignore`.
- **Owner approval:** the role files, `openspec/config.yaml` and the constitution (constitution §1, CODEOWNERS).
- **Process:** after the merge, each developer runs `scripts/setup-agents` and restarts the session so the new role appears.
