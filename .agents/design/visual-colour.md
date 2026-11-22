Source: pbakaus/impeccable, `skill/reference/colorize.md` @b49177d, Apache-2.0, licence in `.agents/design/licenses/pbakaus-impeccable.LICENSE`; modified by teachdash.

# Colour: roles, strategy and perception

Introduce colour as hierarchy, meaning and atmosphere. Preserve confirmed brand and semantic conventions; do not replace a visual world under the guise of colouring it. `project-rules.md` wins over this file: colours are role tokens from `frontend/src/styles/tokens.css`, and a missing role becomes a new role-named token (C3). The role list, the "tint greys" advice and the WCAG contrast minimums are in `refactoring-ui.md` section 4 and are not repeated here.

## Operate and read surfaces

teachdash is a tool for operating and reading: colour primarily encodes action, selection, status, wayfinding and reading hierarchy. Rarity gives an accent force.

## Audit before choosing

Read the tokens, assets, current themes and representative states. Identify:

- which colours are confirmed brand commitments;
- current surface, text, action and semantic roles;
- places where grayscale obscures hierarchy or state;
- contrast failures and colour-only communication;
- light/dark or data-visualization requirements;
- whether the task asks for more colour or a new identity (a new identity is an engineer decision).

## Choose a strategy

Name the intended emotional temperature, dominant relationship, contrast range and colour dosage before editing. The strategy may be restrained or immersive; it must follow the brief rather than a fixed percentage rule.

Use the project's existing colour space. For a new web palette, prefer OKLCH because lightness and chroma can be adjusted predictably. Choose hue from product meaning and visual direction, never from a default category association.

## Apply at system scale

- Let the strongest colour own a deliberate region or role instead of scattering tiny accents.
- Keep the primary action easy to find; do not spend its colour on decoration.
- Tint neutrals only when the brand hue genuinely creates cohesion. Neutral gray is valid when it serves the world.
- On coloured surfaces, derive secondary text from the foreground or surface hue rather than using washed-out generic gray.
- Keep semantic meanings consistent, but respect platform and domain conventions instead of assuming fixed hues.
- For data, use distinct lightness, chroma, shape, label or pattern so colour is not the only code.
- In dark mode, design surface elevation and contrast explicitly; do not invert the light theme mechanically.
- Theme changes should normally remap semantic roles, not primitive values.

Decoration without a relationship to hierarchy, state, content or the visual world is not a colour strategy.

## Perception

Do not rely on eyesight alone. Check interactive states, overlays, text on images, disabled content and both themes. Simulate common vision deficiencies. Information conveyed by colour also needs text, shape, iconography or position.

When deriving OKLCH ramps, vary lightness and reduce chroma near white and black. Do not keep high chroma at extreme lightness merely to make the math uniform. Prefer explicit colours over chains of translucent overlays when alpha would make contrast context-dependent.

## Verify

- Every colour has a stable role or a world-specific atmospheric purpose.
- Attention lands on the intended action, content or state.
- The palette works across quiet, dense, interactive, error and empty states.
- Light and dark themes are each composed, not mechanically inverted.
- Contrast and non-colour cues pass in all relevant states.
- The result is recognizably this product, not a generic "colourful" treatment.
