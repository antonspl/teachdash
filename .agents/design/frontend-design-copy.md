Source: anthropics/skills, `skills/frontend-design/SKILL.md` @e4bb5d9, Apache-2.0, licence in `.agents/design/licenses/anthropics-skills.LICENSE`; modified by teachdash.

# Frontend design: restraint and copy

Only the line length, restraint, quality floor, screenshots and copy parts of the source are kept. `project-rules.md` wins over this file.

## Line length

Text and forms sit inside `--content-width`.

## Restraint and self-critique

Spend your boldness in one place. Let one element be the most important thing on the screen, keep everything around it quiet and disciplined, and cut any decoration that does not serve the task. Before you finish, look again and remove one thing.

Build to a quality floor without announcing it:
- responsive down to 360 px;
- visible keyboard focus;
- reduced motion respected, with state feedback kept (C6);
- WCAG 2.2 AA, including contrast;
- colours from the role tokens only (C3).

Critique your own work as you build. Take screenshots and look at them: a picture shows what code review misses. The frontend-implementer role says how (each page the slice touches and each state it changes, at desktop and 360 px).

## Writing in the interface

Words appear in a design for one reason: to make it easier to understand and use. They are design content, not decoration. Bring the same intention and minimalism to copy that you bring to spacing and colour. Before writing anything, ask what the screen needs to say and how it can best help the person find their way.

Write from the user's side. Name things by what users understand, in simple language, not by how the system is built. A teacher manages lessons, not records or entities. Describe what something is or does in plain terms rather than selling it. Being specific and clear to a new user always beats being clever.

Use active voice by default. A button says exactly what happens when it is used: "Save changes", not "Submit". An action keeps the same name through the whole flow, so a button that says "Publish" leads to a message that says "Published". The words of an interface are its signposts; consistency is how people learn their way around.

Treat failure and emptiness as moments for direction, not mood. Say what went wrong and how to fix it (C10). Errors don't apologise and are never vague about what happened. An empty screen is an invitation to act: text plus a `src/ui/` action (C14).

Keep the tone friendly and informal, like talking to a friend, in the second person (C10): plain verbs, sentence case (C1), no filler. Let each piece of text do exactly one job. All copy goes through i18n, with each language's own typography (C11).
