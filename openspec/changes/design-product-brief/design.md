# Design

## Context

See proposal.md. `.agents/design/product-brief.md` is a stub. The template `.agents/design/product-brief-template.md` lists the sections, says the brief holds durable product truth (confirmed facts and explicitly marked open decisions), and forbids visual worlds, CSS values and invented claims. `project-rules.md` wins over the template. Every fact below comes from the constitution, the main specs, the README, issues or the engineer's answers (proposal, decision 6). The sources are cited in parentheses so the spec gate can check them; the written file keeps them, as short pointers.

No frontend/src change: UI design: none, only a document under `.agents/design/` changes. No new dependency. No Flyway migration.

## Goals / Non-Goals

- Goal: one short brief, in the template's sections, that the frontend roles read first and later design decisions cite.
- Non-goal: any visual decision (palette, type, spacing, shape, density, layout, components), CSS values, page concepts, or invented customers, testimonials, benchmarks, pricing or deployment claims.

## Decisions

### D1. Content of `product-brief.md`

The implementer writes the file from the text between the markers below, as it stands after the spec gate. Section headings follow the template (`# Product`, then `##` sections; the template's "Evidence on Hand" is kept). Product principles are proposed, not decided; the engineer agrees them at the spec gate, and the file is written with the agreed wording.

<!-- BRIEF START -->
# Product

## Users

Three roles: admin, teacher, student (constitution §3).

- Teachers: tutors teaching any subject (code owner, 2026-11-22), often independent private tutors. Each has a native language, set by the admin at creation and changeable by the teacher (§9, `specs/web-profile`). A course has an instruction language (initially `en` and `ru`) and may have a target language (§9).
- Students: registered by their teacher or joining through the teacher's join link; at most one teacher; native language set by the teacher or chosen at joining (§8, §9, `specs/student-enrolment`). Mostly adults and teens in one-to-one classes, not children (engineer, 2026-11-22).
- Admin: creates and manages teacher accounts; reads all data except student answers and results (§8).
- Scale: about 1000 teachers (§8); tens of students per teacher; 1-3 admins (engineer, 2026-11-22).

## Product Purpose

A lightweight teaching platform: lessons, exercises, a shared whiteboard and video calls (README).

A teacher builds courses (course > lessons > parts with teacher-chosen names > ordered items), assigns a course to students, and opens or closes each lesson per student. A student reads an open lesson, answers exercises and sees results: correct, wrong or unchecked (#13, #135, #138).

A teacher can share a course or a lesson with other teachers by link: the other teacher gets a read-only preview and may copy it (§8; code owner, 2026-11-22).

Planned (#14): a teacher manages their schedule in their personal cabinet. They see the schedule and each class with its details: the student, a comment, whether it was rescheduled or cancelled, the meeting link and the whiteboard link (code owner, 2026-11-22).

Success: a teacher prepares a lesson once and runs it for many students without leaving the platform; a student always knows what is open and what to do next (engineer, 2026-11-22).

## Positioning

- One light place for content, classes and the whiteboard, for independent teachers rather than schools (engineer, 2026-11-22).
- Several teachers from day one, each with private data; sharing only by link as a read-only preview plus copy (§8).
- Subject-independent core (§9).

## Operating Context

- UI in English and Russian, separate from course languages; Cyrillic must work everywhere; Russian strings are longer. Users on fixed-line internet in Russia and the EU; no third-party CDNs or web fonts (§10, `project-rules.md`).
- Devices: teachers prepare content at a desk on a laptop; teacher editors are designed for desktop (#115–#120, #132, #133, #138). Students are desktop-first too; by default every page, editors included, must also look good on a phone (360 CSS px) (code owner, 2026-11-22). `specs/web-client` R7 (fits 360 CSS px, no horizontal scroll) is the minimum.
- A student may have a lesson open next to a live video call, in a narrow window with split attention.
- Frequency: teachers daily; students a few times a week; admins rarely (creating a teacher, issuing a reset link). Admin screens can be plain; teacher screens must be fast (engineer, 2026-11-22).
- Time zones matter for schedules: teachers and students both have a time zone (`specs/web-profile`, `specs/student-enrolment`).
- First release focus: content and enrolment. Schedule (#14), whiteboard and video (LiveKit Cloud later, §3; a meeting link until then) come later.

## Capabilities and Constraints

- Built now: accounts, login and set password, admin teachers, profile, enrolment, courses, lessons, parts, items core model. Current screens: login, set password, cabinet, profile, admin teachers, not found.
- Planned (backlog, not order): course editors (#115-#120), student course and lesson views (#134, #135), assign and lesson access (#132, #133), results (#138), cabinet tabs (#57), students list (#35), join page (#37), schedule (#14), groups (#54), homework (#56), media (#55), language switch (#102, #103).
- Exercise types, planned and not built (only gap fill is scheduled so far, #120, #135): gap fill, dropdown, free text, matching; images; theory in Markdown with KaTeX maths (#13, #119). Limits: at most 20 courses per teacher and course titles 1-200 characters (`specs/courses` R1); 100 lessons per course (`specs/lessons` R1), 20 parts per lesson (`specs/parts` R1), 50 items per part (`specs/items` R1). YouTube embeds in lessons; other video URLs show as links (§10).
- Terminology: lesson = content, class = scheduled call; never mixed (§3).
- Voice: copy is informal, friendly, sentence case, second person, no apologies (`project-rules.md`). `project-rules.md` wins over the "neutral tone" lines in #102, #115–#120, #132, #133 and #138.
- Personality: calm, not playful. Every concrete visual choice (palette, type, layout, chrome) belongs in `design-code.md`, not here.
- Name and brand: no logo and no brand colour yet; the working name "teachdash" may change.

## Evidence on Hand

- No real lesson content, user research or brand assets in the repository; none may be invented.
- Realistic samples from the specs: course "English B1", lesson "Present Simple", part "Warm-up".

## Product Principles

1. Domain first: the platform stores the data and guards how it can be accessed and exposed; the frontend shows that data.
2. On a phone, no one loses the next action.
3. Every language is first-class: Russian text is as long as it gets, and nothing breaks in either language.
4. Teacher data is private and its owner is always clear.
5. Friendly words, calm personality.

## Accessibility & Inclusion

WCAG 2.2 AA, rules in `project-rules.md`.
<!-- BRIEF END -->

The principles in the block above are proposed; the engineer agrees them at the spec gate (the "proposed" note is not carried into `product-brief.md`). Items marked "(engineer, 2026-11-22)" were shown to the engineer as likely readings and accepted. All other facts are as confirmed in proposal decision 6 or taken from the cited sources. The "Voice and assets" material of the draft sits under "Capabilities and Constraints" (voice, name) and "Evidence on Hand" (assets), since the template has no section of its own for them.

### D2. Where the file is checked

Only `product-brief.md` changes outside the change folder. The README table already lists `product-brief.md` as "ours"; the README is not edited. The README's mention that a stub saying "filled by #171" is skipped stays true for `design-code.md` only after this change; that text is not changed (decision 7) and is left for the second change.

## Risks / Trade-offs

- A fact in the brief goes stale as the backlog moves (the planned list, the built list) -> the brief names the issues and sources, and the later changes that alter a stated fact update the file in the same PR (README rule).
- The brief repeats facts held in specs -> it points at the source and keeps one line per fact; the spec wins on conflict.
- The principles are not yet agreed -> they are marked proposed here and the spec gate decides them.

## Migration Plan

None.

## Open Questions

None for this change. The visual direction, `design-code.md` and the redesign issues belong to the second change.
