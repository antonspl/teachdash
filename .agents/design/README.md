# Design sources

Frontend design guidance. Its readers are the four frontend roles (`frontend-implementer`, `frontend-researcher`, `frontend-spec-critic`, `frontend-reviewer`) and the `code-gate-reviewer` for a PR touching `frontend/src`; each reads this file first. Roles read these files by path; no tool lists them.

## Reading order

1. `README.md` (this file).
2. `project-rules.md`: teachdash's own rules and the resolved conflicts C0-C14.
3. `checklist.md`: the web interface checklist, for every frontend slice.
4. `accessibility.md`, when the slice adds or changes interactive elements, forms or dialogs.
5. The rest on demand: `accessibility-wcag22.md`, `accessibility-patterns.md`, `refactoring-ui.md`, `refactoring-ui-patterns.md`, `frontend-design-copy.md`, `critique-rubric.md`.

## Precedence

The change's spec wins, then the design code (from #171, not yet written), then `project-rules.md`, then the vendored community sources (C0). Where a source says something else, follow `project-rules.md`. The sources are adapted copies: their own Title Case, uppercase labels, web fonts, raw values and scores are already removed.

## Sources

Each vendored file starts with a header line naming its source, path, commit, licence and licence file, and says it was modified by teachdash. To re-sync, fetch the path at a newer commit and adapt it again; nothing syncs automatically.

| File | Source | Path | Commit | Licence | Licence file |
|---|---|---|---|---|---|
| `project-rules.md` | ours | - | - | - | - |
| `checklist.md` | vercel-labs/web-interface-guidelines | `command.md` | 434b7f9 | MIT | `licenses/vercel-labs-web-interface-guidelines.LICENSE` |
| `accessibility.md` | addyosmani/web-quality-skills | `skills/accessibility/SKILL.md` | afa8da9 | MIT | `licenses/addyosmani-web-quality-skills.LICENSE` |
| `accessibility-wcag22.md` | addyosmani/web-quality-skills | `skills/accessibility/references/WCAG.md` | afa8da9 | MIT | `licenses/addyosmani-web-quality-skills.LICENSE` |
| `accessibility-patterns.md` | addyosmani/web-quality-skills | `skills/accessibility/references/A11Y-PATTERNS.md` | afa8da9 | MIT | `licenses/addyosmani-web-quality-skills.LICENSE` |
| `refactoring-ui.md` | wondelai/skills | `refactoring-ui/SKILL.md` | c526558 | MIT | `licenses/wondelai-skills.LICENSE` |
| `refactoring-ui-patterns.md` | wondelai/skills | `refactoring-ui/references/advanced-patterns.md` | c526558 | MIT | `licenses/wondelai-skills.LICENSE` |
| `frontend-design-copy.md` | anthropics/skills | `skills/frontend-design/SKILL.md` | e4bb5d9 | Apache-2.0 | `licenses/anthropics-skills.LICENSE` |
| `critique-rubric.md` | pbakaus/impeccable | `skill/reference/critique-rubric.md` | b49177d | Apache-2.0 | `licenses/pbakaus-impeccable.LICENSE` |

`checklist.md` also carries three points from pbakaus/impeccable `skill/reference/audit.md` @b49177d, in our own words. The files in `licenses/` are verbatim upstream licences; anthropics/skills has no root licence, so its file is `skills/frontend-design/LICENSE.txt` @e4bb5d9.

## NOTICE

pbakaus/impeccable has a `NOTICE.md`. Its notices are about iOS and Android references, fonts and consent rules, none of which is in `critique-rubric.md` or the checklist points. Apache-2.0 §4(d) excludes notices that do not pertain to any part of the derivative work, so no copy is included.
