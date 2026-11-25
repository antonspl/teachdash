# Visual criteria

The shared yardstick for a frontend change: the implementer's self-critique, the spec critic's coverage check and the `visual-reviewer`'s per-criterion report all use it. Answer each question yes or no against a screenshot or the change's "UI design" section. A "no" is a finding. Where this file disagrees with `project-rules.md`, that file wins (C0); C-items are named where they apply.

## 1. Hierarchy

- Is there one clearly primary element per screen, and does it read first?
- Are secondary and tertiary elements quieter through size, weight or colour, not only through size?
- Is a primary action the only one of its kind in its group (no two equal-weight primaries)?

Sources: [visual hierarchy](refactoring-ui.md#1-visual-hierarchy), [layout assessment](visual-layout.md#layout-assessment), [quieter](visual-passes.md#quieter).

## 2. Spacing scale

- Is every gap, padding and margin a token from the scale, with no raw pixel values (C3)?
- Is space inside a group smaller than space between groups, so grouping is visible?
- Do buttons, fields and cards keep clear space between each other and never touch?

Sources: [spacing and sizing](refactoring-ui.md#2-spacing-and-sizing), [spatial thesis](visual-layout.md#set-the-spatial-thesis).

## 3. Type scale

- Does text come from a small set of token sizes and weights, using `--font-body` only (C3, C4)?
- Are headings, body and captions told apart by size and weight, in sentence case (C1, C2)?
- Are body lines shorter than about 80 characters, with comfortable line height?

Sources: [typography](refactoring-ui.md#3-typography), [type system](visual-type.md#set-the-system), [typographic assessment](visual-type.md#typographic-assessment), [design principles](frontend-design-principles.md#design-principles).

## 4. Colour roles

- Is every colour a role token (C3), with no raw hex or one-off value?
- Is accent colour kept for actions and place, never for status, and does colour never carry meaning alone?
- Does text keep AA contrast on its background in every state shown (default, hover, disabled, error)?

Sources: [colour](refactoring-ui.md#4-colour), [colour at system scale](visual-colour.md#apply-at-system-scale), [colour audit](visual-colour.md#audit-before-choosing).

## 5. Alignment

- Do elements share clear left edges and a consistent column or grid?
- Are labels, inputs and their messages aligned the same way on every form?
- Is any element off by a few pixels, or is text not optically aligned with icons and controls?

Sources: [layout and composition](refactoring-ui.md#7-layout-and-composition), [optical alignment](visual-type.md#optical-alignment), [polish](visual-passes.md#polish).

## 6. Density

- Does the screen show only what the task needs, with nothing that could be removed?
- Is the density the same as comparable screens (tables, lists, forms), not looser or tighter?
- Do empty, loading and error states keep the layout steady, with no jumps?

Sources: [distill](visual-passes.md#distill), [common mistakes](refactoring-ui.md#common-mistakes), [verify](visual-layout.md#verify).

## 7. Generic AI defaults to avoid

- Is there no accent on a single word in a heading, and no label above content that adds nothing?
- Is there no decoration with no meaning: numbered markers on non-sequences, gradients, glow, icon-in-circle rows, or a card around everything?
- Is motion limited to answering a person's action, with no entrance effects on every section (C6)?

Sources: [design principles](frontend-design-principles.md#design-principles), [common mistakes](refactoring-ui.md#common-mistakes), [bolder: why it reads flat](visual-passes.md#why-it-reads-flat).
