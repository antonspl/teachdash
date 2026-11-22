Source: pbakaus/impeccable, `skill/reference/layout.md` @b49177d, Apache-2.0, licence in `.agents/design/licenses/pbakaus-impeccable.LICENSE`; modified by teachdash.

# Layout: reading order, grouping and rhythm

Layout turns product priority into reading order, grouping, rhythm and usable space. Diagnose the structural problem before moving boxes. `project-rules.md` wins over this file: pages compose `src/ui/` components and spacing comes from the `--space-*` tokens. The spacing scale, "group spacing exceeds within-group spacing", constrained widths, left alignment and the form and list patterns are in `refactoring-ui.md` sections 2 and 7 and are not repeated here.

## Operate and read surfaces

Predictable structure, stable density and navigable linearity are affordances. Preserve the established visual world: a layout change moves structure inside it.

## Layout assessment

Inspect representative states and viewports. Answer every question with rendered or source evidence:

- **Reading order:** Apply the squint test. With detail blurred, can you still identify the primary element, the secondary element and the major groups in order?
- **Grouping:** Are related items close and distinct groups separated, or are containers compensating for weak proximity?
- **Rhythm:** Do tight and generous intervals create a deliberate cadence, or is one spacing value repeated until everything has equal weight?
- **Structure:** Does the topology match the content and task? Are repeated cards, columns or sections genuinely equivalent, or merely a framework default?
- **Density:** Does the amount of information per region fit use frequency and decision complexity?
- **Adaptation:** At narrow, intermediate, wide, zoomed and localized states, what reorders, collapses, wraps, scrolls or remains fixed? Does DOM and focus order still agree with the visual order?
- **Extremes:** Do long content, empty states, overlays, sticky elements, safe areas and small touch targets expose structural failures?

Also inspect arbitrary spacing, overflow, stacking and container behavior by reading the source. A clean read cannot prove hierarchy or rhythm.

## Set the spatial thesis

Before editing, name:

- the primary reading or task path;
- what belongs together and what must separate;
- which element leads and which supports;
- the intended density and spacing rhythm;
- how the structure changes across containers, viewports, input modes and content extremes.

Choose the simplest structural model that expresses those relationships. Name reusable spacing and container roles semantically.

## Apply

- Create rhythm through deliberate contrast between tight and generous intervals.
- Let hierarchy follow product priority, not framework defaults.
- Keep distinct content visually distinct without turning every group into an isolated component.
- Make responsive behavior structural: reorder, collapse, reflow or reveal based on what remains important.
- Prefer container-aware components when the same component appears in different contexts.
- Use `gap` for sibling rhythm when it expresses the relationship more directly than child margins.
- Keep touch targets usable even when their visible marks are small.
- Use depth only when it clarifies state or hierarchy.
- Make optical corrections only after inspecting the rendered result.

Variation is not a goal by itself. Repetition should support recognition; break it only when content or priority changes.

## Verify

- The squint test still reveals the primary, secondary and major groups in order.
- The reading and task path remains clear at every supported size.
- Related content groups naturally; unrelated content does not blur together.
- Tight and generous spacing create intentional rhythm instead of monotonous repetition.
- Density matches use frequency and content complexity.
- Long text, empty states, localization, zoom and dynamic content do not break the structure.
- Keyboard, touch and assistive-technology order agree with the visual order.

Answer each item with rendered or source evidence. Do not substitute a bare "yes" for verification.
