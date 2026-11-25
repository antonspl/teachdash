# Proposal

## Clarifications

Decisions by the engineer for issue #171, second of two changes (part 1 is the archived `design-product-brief`, PR #189). The visual plan is the "r3" mockup (variant B "Mulberry" layout with variant A "Ink" colours), built in rounds D, R2 and R3 and kept outside the repository. Rule proposals carry the numbers of the mockup's build spec (P1, P2, R2-P3, R2-P4, R2-P5); Q-numbers are the design questions of the current-UI report (angle B).

1. Base = r3: the layout of variant B "Mulberry" with the colours of variant A "Ink". Petrol accent `#155770` on cool paper; the accent is rare; type icons are muted. "Current place" = white tab + 3px bar; "selected" = accent-subtle + 3px bar.
2. Dark mode is out of scope.
3. Shared points: Q5 the green button becomes the accent primary; Q10 control-border vs divider token split (folds #168); Q11 states; Q13 the page action sits on the title row; Q16 empty states; Q17 "Loading…" text; Q18 banners are icon + text, no shadow; Q19 "(optional)" (#165).
4. Rule proposals accepted: P1 token rework (border/divider split, remove `action-success`, role-named spacing); P2 shadow only on overlays; R2-P3 controls 2.5rem on `pointer: fine`, 2.75rem on touch, 2rem row icon buttons; R2-P4 icon-only buttons only for repeated row actions, tooltip and aria-label from one i18n key. Rejected: R2-P5 (item type by icon alone): rows show icon + short name.
5. No dead space: data tables and the builder stretch to the edge, the editor takes the remaining width; lesson and homework = a 40rem text column + a right side panel (lesson progress and what is next; for homework the question list and "When you send"), the panel moves below under 59rem; login is centred.
6. Two list types: the data table (row background and dividers edge to edge, content capped about 72rem, no outer box, full-width dividers, page header capped the same, left-aligned beside the rail at 1920) and the short link list (inset, narrow; student home capped about 44rem). Admin keeps 72rem.
7. White space around the lesson text at 1920 stays.
8. Left pane nesting: "Course" label > lessons (nested list, chevron, `aria-expanded`) > parts (indented, guide line, current part bold + bar, `aria-current`). The lesson name opens its first part; a separate chevron expands the parts. Fixed 14rem tree width.
9. Type scale: "large" = 1.25rem (the course name uses it).
10. Builder: item Up/Down/Delete live in the item header next to "Item n of N"; rows have no buttons. Part actions (Rename/Up/Down/Delete) only on the current part (the one holding the selected item) on desktop, as word buttons (Delete on the name line, Rename/Up/Down on a second line); on phones word actions on every part head. When a selection removes the part action line above the clicked row, keep the row under the pointer by adjusting the outline pane scroll. Badge "Students send it" instead of a second "Homework". "Type the answer" uses a text-cursor icon, not a pencil. Item type short names "Pick"/"Type" (ru Теория/Изображение/Аудио/Выбор/Ввод); the type label width is per language (`max-content`).
11. Button roles (a rule for the design code): create and page-level quiet actions = icon + word in text colour (including repeated "Add item/lesson/option"); edit actions on part and item heads = word only, muted; icon-only = repeated row actions (Remove option ×) and universal symbols (close, chevrons, back, pager, play). Exempt: form and primary buttons. Admin row actions = icon + word at every width, wrapping under the name at fixed per-language main-area widths (56rem en (dropped, see fix to finding 8), 72rem ru; no JS measuring). A muted, non-red Delete is fine because every Delete opens a confirmation dialog (the design code must state this rule; refs #137, #121). "Send to Anna" is text only. At 360 move actions are words ("Up"/"Down") everywhere.
12. The Profile rail item is a menu (a non-clickable "Anna Petrova, teacher" line at the top, then Profile, language, Log out) at every width; Log out moves out of the phone tab bar into it. Russian rail labels are short words chosen for the rail, `hyphens: auto` as the fallback; "Учителя" everywhere in Russian. Admin at 360: the time zone goes on the email line. Phone builder: item reorder and delete move into the item view; the item type name sits on a small muted line above the preview in every language. "Your answers" is always open on wide screens.
13. Admin search and paging are shown in the direction but ship as a separate later issue with the backend (`GET /api/admin/teachers` is unpaged, `AdminController.java:37`), not in the first admin redesign.
14. #102 (locale switch) and #103 (Russian) land before the app frame and the admin redesign.
15. The builder follows r3 on phones too (specs #102 and #115–#120 said editors are desktop-only; the builder feature issues grow accordingly).
16. (amended by 31) The side menu (rail) ships with only the cabinet title and Profile; tabs arrive with their features.
17. #165 "(optional)" is its own change (it touches 4 specs), so it does not block the primitives issue.
18. Token rename: rename across all `src/ui` CSS in the same PR, with before/after screenshots (no alias period).
19. The lesson tree component is built with the first builder issue (#116/#117), not in the components issue.
20. `role="alert"` for error banners is a component rule in the banners issue, not a deferred product question.
21. Deferred to feature issues (not decided here): item type names, including the reviewer's "verbs" objection to Pick/Type ("Answer" would clash with students' answers) -> #118/#120; the right answer after a wrong pick, blank answers on send, the transcript -> #135/#126/#127; the homework concept and due dates -> #56/#135. The roles and reading-list workflow question stays open (needs Anton).
22. Design-code note: an admin table built with grid/subgrid may drop table semantics; keep table semantics.
23. Mockup location: the r3 mockup is NOT added to the repository or to `.agents/design/`. Its screenshots live in PR comments (5 desktop shots, one main screen per role, are in PR #194 comment 7156421470). `design-code.md` stays self-contained (all values and rules written out) and does not link to or depend on the mockup. The PR comment is mentioned in this proposal only.
24. Builder "Hide" / "Show lessons": between 64rem and 80rem the lesson tree is an overlay drawer opened by "Show lessons"; "Hide" exists only at 80rem and wider, where it collapses the tree in place and a "Show lessons" button replaces it. The `sidebar` and `chev-left` icons are added to the icon table. Focus: opening the drawer focuses the current lesson or part in the tree; closing it (Escape, Hide, or picking a part) returns focus to the button that opened it / "Show lessons".
25. Phone builder: to change lesson, the back button in the phone title bar leads to a course screen with the lesson list.
26. Empty part on desktop: a part also counts as "current" (shows Rename/Up/Down/Delete) when it has focus or the last action was in it, so an empty part's actions are reachable.
27. Stale lines are fixed in this PR, as the design README asks: `product-brief.md` Devices line (editors are also designed for phones, per Clarification 15); `project-rules.md` C5 and C21 (`--shadow-card`, "one soft shadow" become shadow only on overlays) and C9/C18 (compact controls 2.5rem `pointer: fine` / 2.75rem touch / 2rem row icon buttons); `visual-criteria.md` accent wording aligned with the design code ("actions and place, never status"). One-line edits each; tasks added; the implementer makes them (outside the change folder); exact before and after lines are in `design.md` D5.
28. Mockup-only behaviours (tooltip flip, half-rem window clamp, 400ms delay and Escape/hover rules; editor two-column layout from 50rem with sticky preview; sticky editor footer with the divider only while pinned; login 12vh offset; language radio items inside the Profile menu; accent type icon on the selected row; item picker placement popover on desktop / inline panel on phones) are each decided in their own feature issue. `design-code.md` records each as "in the chosen mockup: <what it did>; decided in <issue>", not as a rule. Items with no decision (empty lesson, empty course in the tree, picker roles and keyboard, autosave failure state, tree disabled and loading states) are recorded as "open, decided in builder issue #116/#117", with what the mockup did if anything.
29. Positioning of tooltip, menu and picker: a small `src/ui` helper sets the position from code with `style.setProperty`, recorded in "Departures" as an explicit exception to the project-rules no-inline-style rule.
30. The Profile menu (with Log out and language) ships in redesign step 4a together with the desktop rail, so desktop never lacks Log out. 4b is the phone tab bar only.

31. The cabinet title is not a rail item. Admin's single rail item is Teachers (opens `/admin`). For teacher and student, the single rail item carries the cabinet's name until real tabs arrive with their features (it opens the cabinet page). The cabinet title itself is the page heading of the cabinet page. The Profile menu stays as decided (30).
32. Between redesign steps 4a and 4b, the current header stays below 64rem (phones keep their Log out and language select there) until 4b ships the phone tab bar and title bar, which then removes the old header.

Specs affected later (not by this change): step 4a changes `web-auth` R3 (the header shows the name, a Profile button and a Log out button) and the e2e `auth.spec.ts`.

Process notes (not engineer decisions): Codex's "Baseline · …" comments on #171 (2026-11-24) and PR #188 are not used by this change. `design-code.md` is written by the implementer from the text in `design.md` (as in part 1), because the spec-writer may write only under this change's folder.

## Why

Issue #171 asks for a design audit; part 1 wrote the product brief. `.agents/design/design-code.md` is still a stub ("This file is filled by #171"), so the frontend roles and the code-gate review have no chosen direction to build or check against, and constitution §1 step 10 already says violations of "the design code (#171)" are fixed in the PR. This change records the chosen direction (tokens, layout, components, rules) in `design-code.md`, and the ordered plan of redesign issues to file afterwards.

## What changes

- `.agents/design/design-code.md` is written from the text in `design.md` (D1), replacing the stub entirely: direction, tokens (with the rename map from today's tokens), colour, type, space and shape, states, buttons and icons, components, layout, copy, accessibility, deferred points, and the redesign plan.
- Four stale lines are fixed outside the change folder, by the implementer (decision 27; exact lines in `design.md` D5): `product-brief.md` (Devices), `project-rules.md` (C5, C9, C18, C21) and `visual-criteria.md` (accent wording).
- Nothing else outside this change's folder.

Departures from `project-rules.md` (C5 shadow and token renames, C9/C18 control height, C21 one shadow) are stated in the design code and win by C0 (the design code ranks above the project rules). `project-rules.md` has its stale C5, C9, C18 and C21 lines corrected in this PR (decision 27).

## Research angles

Agreed with the engineer:

- Mockup rounds D, R2 and R3: static HTML directions, then three variants, then the chosen r3 base; conclusion: decisions 1-22 above.
- B, current UI inventory and design questions (from part 1): the Q-numbers above.
- F, redesign issue plan in dependency order: conclusion in `design.md` (D3) and in the design code's last section.

## Out of scope

- Any change under `frontend/src`, backend code, specs and migrations. This is a tooling change with `skip_specs: true`: no spec deltas, no scenarios, no Red step. "UI design: none, only a document under `.agents/design/` changes."
- Filing the redesign issues (done after the change, by the engineer or the main agent) and editing the existing issues #102, #115–#120, #132, #133, #138 (the desktop-only editor lines).
- Dark mode (decision 2); admin search and paging (decision 13); the "(optional)" marker (decision 17, its own change); the lesson tree component (decision 19).
- Designs for screens that are not built (builder, student home, lesson, homework): they get rules in the design code, not redesign issues.
- Edits to roles, `README.md`, vendored sources and permission files, and any edit to `project-rules.md`, the product brief or `visual-criteria.md` beyond the lines in decision 27.
- Copying the mockup sources into the repository (decided against, decision 23).
- Closing #171: the PR closes it ("Closes #171"), because this change completes the issue.
