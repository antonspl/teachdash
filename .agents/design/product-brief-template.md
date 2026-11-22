Source: pbakaus/impeccable, `skill/reference/init.md` (Step 4 template) and `skill/reference/shape.md` (interview questions) @b49177d, Apache-2.0, licence in `.agents/design/licenses/pbakaus-impeccable.LICENSE`; and anthropics/skills, `skills/frontend-design/SKILL.md` (subject-matter paragraph) @e4bb5d9, Apache-2.0, licence in `.agents/design/licenses/anthropics-skills.LICENSE`; modified by teachdash.

# Product brief: template and questions

A template for the sections of `product-brief.md` and the questions its author must be able to answer. It records durable product truth: confirmed facts and explicitly marked open decisions. It does not choose a visual world; the direction and the token plan go in `design-code.md`. Omit a section that has nothing confirmed rather than filling it with generic prose. `project-rules.md` wins over this file.

## Ground the brief in the subject matter

If the brief does not identify what the product or subject matter is, identify it before designing, and confirm with the engineer. Propose one concrete subject, the design's audience and the design's primary job. The subject's industry, subject matter, materials and vernacular are where distinctive visual choices come from: a design for a toy for girls aged 8-11 will look very different from a dashboard for financial analysts. Build with the brief's real content and subject matter throughout.

## Questions the brief must answer

Choose the few that most change the result; state the likely reading and let the engineer correct it, instead of presenting a questionnaire. A sparse task needs at least one answered round; a precise one needs only a compact confirmation.

Purpose, people and outcome:

- What is this surface or feature for, and what problem must it solve?
- Who specifically reaches it, in what situation and state of mind?
- What is the primary thing they must understand or do? What would success look like?
- What is uniquely true here that a neighbouring product or a generic template could not claim?

Material, behaviour and boundaries, only where a decision is still open:

- What real content, evidence, data and assets must the experience carry? What are realistic minimum, typical and maximum ranges?
- Which states and transitions matter: first run, empty, loading, error, success, permissions, overflow, expert use?
- What is the intended fidelity, breadth and interactivity: exploration, production-ready screen, full flow, broader surface?
- What must remain untouched? What would make the result feel wrong even if it looked polished?
- Which performance, accessibility, localisation or delivery constraints are binding?

Do not put CSS values or canned aesthetic lanes in the brief.

## What belongs in the brief

- users, jobs, workflows, purpose, success, positioning and operating context;
- capabilities, constraints, terminology, evidence and accessibility;
- confirmed voice and assets.

## What does not belong in the brief

- visual worlds, palettes, typography, components or page concepts (these go in `design-code.md`);
- narrative, call-to-action or proof sequences, or other surface strategy;
- invented testimonials, customers, benchmarks, pricing, licensing or deployment claims;
- a requirement to decide every optional field.

## Template

Write only confirmed facts and explicitly marked open decisions.

```markdown
# Product

## Users
[Primary users, their situation, and job. Add other audiences only when confirmed.]

## Product Purpose
[What the product does, why it exists, and what success means.]

## Positioning
[The product mechanism or claim a neighbouring product could not truthfully copy.]

## Operating Context
[Workflows, environments, tools, documents, materials, and rituals that are factual parts of using or evaluating the product.]

## Capabilities and Constraints
[Confirmed functionality, technical constraints, terminology, and explicitly undecided product facts.]

## Evidence on Hand
[Real content, data, demonstrations, case studies or assets, with paths where applicable. State absences that future work must not fabricate.]

## Product Principles
[Three to five durable strategic principles derived from confirmed answers; no visual recipes.]

## Accessibility & Inclusion
[Known user needs or required standard. Omit when no product-specific requirement was established.]
```
