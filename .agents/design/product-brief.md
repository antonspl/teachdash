# Product

## Users

The three roles are admin, teacher and student (constitution §3).

- Teachers include tutors of any subject, often independent private tutors. Each has a native language, set by the admin at creation and changeable by the teacher (§9, `specs/web-profile`).
- Students are mostly adults and teens taking private lessons (engineer, 2026-11-22). Each has at most one teacher, who can enroll them or send them a join link. The teacher sets the student's native language or the student chooses it when joining (§8, §9, `specs/student-enrolment`).
- Admins create and manage teacher accounts. They can read data across the platform except student answers and results (§8).
- The platform is designed for about 1,000 teachers (§8), with tens of students per teacher and 1–3 admins (engineer, 2026-11-22). For roster design, plan for around 10 active students per tutor on average, often more.

## Product Purpose

TeachDash is a lightweight tutoring platform for lessons, exercises, a shared whiteboard and video calls (README).

In the intended teaching workflow, a teacher builds courses (course > lessons > parts with teacher-chosen names > ordered items), assigns a course to students, and opens or closes each lesson per student. A student reads an open lesson, answers exercises and sees whether each answer is correct, wrong or unchecked (#13, #135, #138). Between classes, the intended workflow lets students find materials and stay in touch with their tutor.

The goal is for a teacher to prepare a lesson once and run it for many students within the platform. A student should always know what is open and what to do next (engineer, 2026-11-22).

## Positioning

- One light place for content, classes and the whiteboard, for independent tutors (engineer, 2026-11-22).
- Each teacher's data is private from day one. The planned sharing workflow lets teachers share their own courses and lessons with other teachers by link. The recipient can open a read-only preview and make an independent copy (§8).
- The core supports any subject. Each course has an instruction language (`instructionLanguage`) and may have a target language when it teaches a language. The configured course languages initially support English (`en`) and Russian (`ru`) (§9).

## Operating Context

- The UI supports English and Russian independently of course languages. Support Cyrillic throughout and make room for longer Russian strings. Users are on fixed-line internet in Russia and the EU. The platform uses no third-party CDNs or web fonts (§10, `project-rules.md`).
- Teachers and students are desktop-first. Teachers prepare content and teach on a computer, and teacher editors are designed for desktop (#115–#120, #132, #133, #138). Both roles can use a phone between classes. Every page, including the editors, remains usable at 360 CSS px (`specs/web-client` R7).
- A student may have a lesson open next to a live video call, in a narrow window with split attention.
- Teachers use the platform daily and students a few times a week. Admins use it rarely, for tasks such as creating teacher accounts or issuing reset links. Keep teacher screens fast and admin screens simple (engineer, 2026-11-22).
- Teachers and students both have a time zone for scheduling (`specs/web-profile`, `specs/student-enrolment`).
- The first release focuses on content and enrolment. Schedule (#14), whiteboard and video come later. The planned schedule gives teachers a separate place in their personal cabinet to manage classes. The proposed class details include the student, a comment, whether the class was rescheduled or cancelled, and links to the meeting and whiteboard. A meeting link serves the class until LiveKit Cloud is available (§3).

## Capabilities and Constraints

- The November 2026 audit baseline includes accounts, login and password setup, teacher administration, profiles, enrolment, and the core model for courses, lessons, parts and items. The screens are login, set password, cabinet, profile, admin teachers and not found.
- The November 2026 backlog includes course editors (#115-#120), student course and lesson views (#134, #135), assign and lesson access (#132, #133), results (#138), cabinet tabs (#57), students list (#35), join page (#37), schedule (#14), groups (#54), homework (#56), media (#55), language switch (#102, #103).
- Planned lesson content includes gap fill, dropdown, free text and matching exercises, images, and theory in Markdown with KaTeX maths (#13, #119). Gap fill was the only exercise type scheduled at the audit baseline (#120, #135). Each teacher can create up to 20 courses with titles of 1–200 characters (`specs/courses` R1). Each course can contain up to 100 lessons (`specs/lessons` R1), each lesson up to 20 parts (`specs/parts` R1), and each part up to 50 items (`specs/items` R1). Lessons embed YouTube videos and display other video URLs as links (§10).
- A lesson is course content. A class is a scheduled call with a student (§3).
- Use informal, friendly copy in sentence case and second person. Follow the guidance on apologies in `project-rules.md`. `project-rules.md` wins over the "neutral tone" lines in #102, #115–#120, #132, #133 and #138.
- The product has a calm personality. Visual choices such as palette, type, layout and chrome belong in `design-code.md`.
- At the audit baseline, "teachdash" is the working name and the logo and brand colour are open decisions.

## Evidence on Hand

- The repository provides no real lesson content, user research or brand assets. Use documented evidence for claims.
- Examples from the specs include the course "English B1", the lesson "Present Simple" and the part "Warm-up".

## Product Principles

1. Start with the domain. The domain defines what data is stored and the backend enforces who can access it and what can be exposed. The frontend presents the data users are allowed to see (§3, §8). Within that model, the lesson is the main learning object.
2. Students on small screens never lose the next action.
3. Give both supported UI languages equal attention. Allow for longer Russian text and keep the interface usable in both languages.
4. Teacher data is private and its owner is always clear.
5. Friendly words, calm personality.

## Accessibility & Inclusion

WCAG 2.2 AA, rules in `project-rules.md`.
