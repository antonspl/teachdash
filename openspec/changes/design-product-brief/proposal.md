# Proposal

## Clarifications

Decisions by the engineer for issue #171 (2026-11-22), first of two changes:

1. Start now on top of unmerged PR #169 (#166). Superseded by decision 11: the draft PR was opened before #169 merged.
2. #171 is split into two PRs: this one (the product brief) first, then the visual direction with `design-code.md` and the redesign issues.
3. The later visual directions are static HTML mockups of 2-3 key screens per direction, kept outside the repository. Recorded here for the second change; nothing to do in this one.
4. Research angles A (brief draft), B (current UI inventory and design questions) and C (baseline screenshots) were run.
5. The baseline screenshots (70 PNGs: 35 states, desktop and 360 px, `en`) are attached to this change's PR by the main agent, not to the issue. They are local in `frontend/screenshots/design-audit/` (gitignored) and are never committed. This differs from the issue's "attached to this issue" wording: no comment is posted on #171. The engineer accepts the difference; the code owner reviews it at the final review.
6. Brief facts: the likely readings drafted by research angle A are accepted as written, with these answers: a student's lesson may be open next to a live video call (narrow window, split attention); UI copy stays informal and friendly per `project-rules.md` (it wins over the older "neutral tone in both UI languages" lines in #102, #115–#120, #132, #133 and #138), while the look should be calm rather than playful; there is no logo and no brand colour yet; the working product name is "teachdash" and may change.
7. This change touches no workflow files: no roles, `project-rules.md`, `README.md`, vendored sources or permission files. Only `.agents/design/product-brief.md` changes, plus the change's own artifacts.
8. The brief keeps one line of product personality next to voice: "calm, not playful". Every concrete visual choice (palette, type, layout, chrome) belongs in `design-code.md`, not the brief. Principles 1 and 5 state product priorities, not a visual style.
9. The brief's voice item gets one line: `project-rules.md` (informal, friendly copy) wins over the "neutral tone" lines in #102, #115–#120, #132, #133 and #138.
10. Baseline screenshots are attached to this change's PR only, with no comment on #171 (see decision 5).

**Code gate review** (engineer's decisions 11-13, from the code-gate review; the spec was already approved):

11. Decision 1 changed: the draft PR #189 was opened before #169 merged, stacked on `workflow-frontend-implementer` (engineer, 2026-11-22). The branch is rebased onto `main` after #169 merges.
12. The issue lists in the brief are completed: the "neutral tone" lines are in #102, #115–#120, #132, #133 and #138 (decisions 6 and 9, and the brief's voice line); the desktop-only editor lines are in #115–#120, #132, #133 and #138 (the brief's Devices line).
13. The brief's Devices line says that teacher editors are designed for desktop, and that every page, editors included, still fits 360 CSS px (`specs/web-client` R7).

**Final-review fix round** (engineer's decisions 14-19, 2026-11-24, from the code owner's final-review comments on `.agents/design/product-brief.md`; the change was archived and restored in bbbf9cf to fold them in):

14. (Anton, comment 4659260401, Users > Teachers line) Teachers are any tutors teaching any subject, not language teachers. A course has an instruction language (initially `en` and `ru`) and may have a target language (constitution §9). The Teachers line is rewritten accordingly and keeps the native-language part. "Language teaching (en and ru) is the first use" is removed from Positioning; the subject-independent core stays. The spec samples ("English B1", "Present Simple", "Warm-up") stay.
15. (Anton, comment 4659265790, Product Purpose) Teachers can share their courses and lessons with other teachers by link. Product Purpose says so in the words of constitution §8: a link to a course or a lesson, the other teacher gets a read-only preview and may copy it.
16. (Anton, comment 4659270344, Product Purpose / Success) Teachers manage their schedule in their personal cabinet, a separate planned task (#14): they see their schedule and each class with its details (the student, a comment, whether it was rescheduled or cancelled, the meeting link, the whiteboard link). The brief says "class" (scheduled call), not "lesson" (§3), and marks it planned (#14). Added to Product Purpose.
17. (Anton, comment 4659274680, Operating Context > Devices) Students are desktop-first too; by default every page must also look good on a phone (360 CSS px). This supersedes the "Students are phone-first (360 CSS px) or laptop" reading in the Devices line, drafted by research angle A and accepted in decision 6, and the Devices wording of decision 13 ("still fits 360 CSS px"). Product Principle 2 is reworded to "On a phone, no one loses the next action."
18. (Anton, comment 4659295751, Product Principles) Principle 1 "Content first: the lesson is the main object; everything else serves it." is replaced by "Domain first: the platform stores the data and guards how it can be accessed and exposed; the frontend shows that data." Principles 4 and 5 are unchanged. Decision 8 called principles 1 and 5 product priorities: principle 1 is now a data and architecture rule, not a product priority or a visual style; principle 5 stays a product priority.
19. Process: the change was restored after the final review (bbbf9cf) and goes through the spec gate and the code gate again. PR #189 is based on `main` now (#169 merged), which supersedes decision 11's stacked-PR note (its rebase after #169 is done). Facts taken from Anton's comments are marked "(code owner, 2026-11-22)" in the brief.

**Spec-critique fixes** (engineer's decisions 20-23, 2026-11-24):

20. The Devices line keeps Anton's "look good on a phone" wording, attributed "(code owner, 2026-11-22)", and says `specs/web-client` R7 (fits 360 CSS px, no horizontal scroll) is the minimum. The "(code owner, 2026-11-22)" tag on the Teachers line covers only the any-tutor, any-subject clause, not "often independent private tutors".
21. The Students line says "one-to-one classes" instead of "private lessons" (§3: lesson = content, class = scheduled call). The other uses of "lesson" in the brief mean content and stay.
22. Task 3.5 allows `openspec/changes/**` (the change folder moves to `openspec/changes/archive/` at 4.2), and the `<restore>` placeholder is replaced by `bbbf9cf`.
23. `README.md` line 3 still says "for language teachers". It is out of scope for this change and is filed as issue #192; README is not edited here.

The five product principles in `design.md` are proposed, not decided: the engineer agrees them (or changes them) at the spec gate.

## Why

Issue #171 asks for a design audit from scratch, and says the product brief comes first: every later design decision refers back to it. `.agents/design/product-brief.md` is a stub ("This file is filled by #171"), so the frontend roles that read it today find nothing about who uses teachdash, on which devices, in which languages and for which tasks. Part of the brief's facts (users, scale, devices, languages, terminology, limits) already sit in the constitution, the specs and issues; this change collects them in one short file that follows `.agents/design/product-brief-template.md`.

## What changes

- `.agents/design/product-brief.md` is written from the text in `design.md`, replacing the stub entirely.
- Nothing else outside this change's folder.

`design-code.md` stays a stub ("filled by #171") until the second change.

## Research angles

Agreed with the engineer (decision 4):

- A, brief draft: a draft of the brief from the constitution, specs, README and issues, with the likely readings of unconfirmed facts. Conclusion: the draft in `design.md`, with the engineer's answers in decision 6 applied.
- B, current UI inventory and design questions: the screens that exist (login, set password, cabinet, profile, admin teachers, not found) and the design questions for the second change. Its conclusions feed the second change; nothing from it goes into the brief beyond the list of current screens.
- C, baseline screenshots: 70 PNGs, local and gitignored (decision 5).

## Out of scope

- The visual direction, mockups and `design-code.md` (second change, decisions 2 and 3).
- Redesign issues, and folding in #168, #165 and the `lang` attribute issue (second change).
- Any edit to roles, `project-rules.md`, `README.md`, vendored sources, `scripts/` or permission files (decision 7).
- Frontend code, backend code, specs and migrations. This is a tooling change with `skip_specs: true`: no spec deltas, no scenarios, no Red step.
- Committing the baseline screenshots (decision 5).
- Closing #171: the PR must not use a closing keyword ("Closes #171"); it says "Part of #171", because the second change completes the issue.
