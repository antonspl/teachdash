# Design sources

Frontend design guidance. Its readers are the `frontend-researcher`, the `frontend-spec-critic`, the `frontend-implementer`, the `visual-reviewer` and the `code-gate-reviewer` for a PR touching `frontend/src`; each reads this file first. Roles read these files by path; no tool lists them.

## Reading order

Every frontend role reads `README.md` first, then `product-brief.md` and `design-code.md` (stubs that say "filled by #171" are noted and skipped). Then, per role:

- `frontend-researcher`: all the visual sources in full (`refactoring-ui.md`, `refactoring-ui-patterns.md`, `critique-rubric.md`, `visual-criteria.md`, `frontend-design-principles.md`, `frontend-design-copy.md`, `visual-colour.md`, `visual-type.md`, `visual-layout.md`, `visual-passes.md`), then `project-rules.md` and `checklist.md`; the accessibility files as its role file says.
- `frontend-spec-critic`: `project-rules.md`, `visual-criteria.md`, `frontend-design-principles.md`, `accessibility.md`.
- `frontend-implementer` (per slice): the change's "UI design", `project-rules.md`, `visual-criteria.md`, `critique-rubric.md`, `checklist.md` (every slice), `accessibility.md` (forms, dialogs, interactive elements); one pass of `visual-passes.md` only when its self-critique or a `visual-reviewer` finding names it.
- `visual-reviewer`: the change's "UI design", `project-rules.md`, `visual-criteria.md`, `critique-rubric.md`, `refactoring-ui.md`, `refactoring-ui-patterns.md`, `frontend-design-principles.md`, `visual-colour.md`, `visual-type.md`, `visual-layout.md`, `visual-passes.md`. It may also run read-only `ui-ux-pro-max/scripts/search.py` queries.
- `code-gate-reviewer`: `project-rules.md`, `checklist.md`, `accessibility.md` for forms, dialogs and interactive elements; no screenshots.

`ui-ux-pro-max/` is query-only for every role: `search.py` queries or grep, never the data files read whole. The rest of the files are read on demand.

## Precedence

the change's spec > design code > `project-rules.md` > `visual-criteria.md` > vendored sources

The design code is `design-code.md`; #171 records the chosen direction and the token plan there. Where a source says something else, follow the one higher in the line (C0). The sources are adapted copies: their own Title Case, uppercase labels, web fonts, raw values and scores are already removed.

A change which alters a behaviour stated in a `.agents/design/` file updates that file in the same PR. That sentence counts as a requested change: the
edit may be made without asking again (the permission files ask first for edits under `.agents/design/**`).

## Sources

Each vendored file starts with a header line naming its source, path, commit, licence and licence file, and says it was modified by teachdash. To re-sync, fetch the path at a newer commit and adapt it again; nothing syncs automatically.

| File | Source | Path | Commit | Licence | Licence file |
|---|---|---|---|---|---|
| `project-rules.md` | ours | - | - | - | - |
| `visual-criteria.md` | ours | - | - | - | - |
| `product-brief.md` | ours | - | - | - | - |
| `design-code.md` | ours | - | - | - | - |
| `checklist.md` | vercel-labs/web-interface-guidelines | `command.md` | 434b7f9 | MIT | `licenses/vercel-labs-web-interface-guidelines.LICENSE` |
| `accessibility.md` | addyosmani/web-quality-skills | `skills/accessibility/SKILL.md` | afa8da9 | MIT | `licenses/addyosmani-web-quality-skills.LICENSE` |
| `accessibility-wcag22.md` | addyosmani/web-quality-skills | `skills/accessibility/references/WCAG.md` | afa8da9 | MIT | `licenses/addyosmani-web-quality-skills.LICENSE` |
| `accessibility-patterns.md` | addyosmani/web-quality-skills | `skills/accessibility/references/A11Y-PATTERNS.md` | afa8da9 | MIT | `licenses/addyosmani-web-quality-skills.LICENSE` |
| `refactoring-ui.md` | wondelai/skills | `refactoring-ui/SKILL.md` | c526558 | MIT | `licenses/wondelai-skills.LICENSE` |
| `refactoring-ui-patterns.md` | wondelai/skills | `refactoring-ui/references/advanced-patterns.md` | c526558 | MIT | `licenses/wondelai-skills.LICENSE` |
| `frontend-design-copy.md` | anthropics/skills | `skills/frontend-design/SKILL.md` | e4bb5d9 | Apache-2.0 | `licenses/anthropics-skills.LICENSE` |
| `frontend-design-principles.md` | anthropics/skills | `skills/frontend-design/SKILL.md` | e4bb5d9 | Apache-2.0 | `licenses/anthropics-skills.LICENSE` |
| `visual-colour.md` | pbakaus/impeccable | `skill/reference/colorize.md` | b49177d | Apache-2.0 | `licenses/pbakaus-impeccable.LICENSE` |
| `visual-type.md` | pbakaus/impeccable | `skill/reference/typeset.md` | b49177d | Apache-2.0 | `licenses/pbakaus-impeccable.LICENSE` |
| `visual-layout.md` | pbakaus/impeccable | `skill/reference/layout.md` | b49177d | Apache-2.0 | `licenses/pbakaus-impeccable.LICENSE` |
| `visual-passes.md` | pbakaus/impeccable | `skill/reference/polish.md`, `quieter.md`, `bolder.md`, `distill.md` | b49177d | Apache-2.0 | `licenses/pbakaus-impeccable.LICENSE` |
| `product-brief-template.md` | pbakaus/impeccable; anthropics/skills | `skill/reference/init.md`, `shape.md`; `skills/frontend-design/SKILL.md` | b49177d; e4bb5d9 | Apache-2.0 | `licenses/pbakaus-impeccable.LICENSE`; `licenses/anthropics-skills.LICENSE` |
| `critique-rubric.md` | pbakaus/impeccable | `skill/reference/critique-rubric.md` | b49177d | Apache-2.0 | `licenses/pbakaus-impeccable.LICENSE` |
| `ui-ux-pro-max/SKILL.md`, `ui-ux-pro-max/references/*.md` | nextlevelbuilder/ui-ux-pro-max-skill | `.claude/skills/ui-ux-pro-max/SKILL.md`, `references/` | 2de1134 | MIT | `licenses/nextlevelbuilder-ui-ux-pro-max-skill.LICENSE` |
| `ui-ux-pro-max/data/**` (CSV, JSON), `ui-ux-pro-max/scripts/**` (`.py`) | nextlevelbuilder/ui-ux-pro-max-skill | `.claude/skills/ui-ux-pro-max/data/`, `scripts/` | 2de1134 | MIT | `licenses/nextlevelbuilder-ui-ux-pro-max-skill.LICENSE` |

These CSV, JSON and `.py` files carry no header line; they are byte-identical to upstream except that four tests and their fixtures are dropped, as `ui-ux-pro-max/NOTICE-teachdash.md` states.

`checklist.md` also carries three points from pbakaus/impeccable `skill/reference/audit.md` @b49177d, in our own words. The files in `licenses/` are verbatim upstream licences; anthropics/skills has no root licence, so its file is `skills/frontend-design/LICENSE.txt` @e4bb5d9.

## NOTICE

pbakaus/impeccable has a `NOTICE.md`. Its notices are about iOS and Android references, fonts and consent rules, none of which is in `critique-rubric.md` or the checklist points. Apache-2.0 §4(d) excludes notices that do not pertain to any part of the derivative work, so no copy is included.
