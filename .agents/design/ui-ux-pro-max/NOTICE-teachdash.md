# ui-ux-pro-max in teachdash

This folder is a copy of `.claude/skills/ui-ux-pro-max/` from nextlevelbuilder/ui-ux-pro-max-skill at commit 2de1134 (MIT, Copyright (c) 2024 Next Level Builder). The licence text is `.agents/design/licenses/nextlevelbuilder-ui-ux-pro-max-skill.LICENSE`.

CSV, JSON and `.py` files carry no header line. They are byte-identical to the upstream blobs at 2de1134 (checked with `git hash-object` against the pinned tree). The changes by teachdash are:

- `SKILL.md`: frontmatter removed; commands written as `python3 .agents/design/ui-ux-pro-max/scripts/search.py ...`; stack detection, the README and install mention removed; the steps for `--persist`, `--preview` and `--tokens` replaced by a statement that they are not to be run; a header line added.
- `references/*.md`: a header line added, text unchanged.
- `scripts/tests/`: five tests that need the upstream repo root (`test_catalog_refresh`, `test_skill_script_paths`, `test_relevance_evaluator`, `test_catalog_summary_line_endings`, `test_design_system_tokens`) and their fixtures (`fixtures/relevance-*.json`, `fixtures/catalogs/`) removed.
- This file added.

The tests that remain are run by hand with `python3 -m unittest discover -s .agents/design/ui-ux-pro-max/scripts/tests`; no CI job covers them. Re-sync by fetching the path at a newer commit and adapting it again.
