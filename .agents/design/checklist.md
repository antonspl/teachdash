Source: vercel-labs/web-interface-guidelines, `command.md` @434b7f9, MIT, licence in `.agents/design/licenses/vercel-labs-web-interface-guidelines.LICENSE`; modified by teachdash.

# Web interface checklist

Check the slice's UI code against the rules below: the `frontend-implementer` before it
reports, the `code-gate-reviewer` on the diff, the `frontend-researcher` at Clarify.
`project-rules.md` wins over this file.

## Rules

### Accessibility

- Icon-only buttons need `aria-label`
- Form controls need `<label>` or `aria-label`
- Native elements first (`<button>`, `<a>`, `<input>`, `<select>`, `<dialog>`): they handle the keyboard; key handlers only on custom widgets, never on a `<button>` (C7)
- `<button>` for actions, `<a>`/`<Link>` for navigation (not `<div onClick>`)
- Images need `alt` (or `alt=""` if decorative)
- Decorative icons need `aria-hidden="true"`
- Async updates (toasts, validation) need `aria-live="polite"`
- Use semantic HTML (`<button>`, `<a>`, `<label>`, `<table>`) before ARIA
- Headings hierarchical `<h1>`–`<h6>`; include skip link for main content
- `scroll-margin-top` on heading anchors
- Meaningful media needs captions, transcripts, or descriptions as applicable
- Media controls need keyboard support; decorative media needs assistive-tech hiding

### Focus States

- Interactive elements need visible focus: an outline in token values (`--color-accent`; a new width is a new role-named token, C3), or the browser default
- Never `outline: none` without focus replacement
- Use `:focus-visible` over `:focus` (avoid focus ring on click)
- Group focus with `:focus-within` for compound controls
- Sticky headers/footers/overlays must not cover the focused element

### Forms

- Inputs need a meaningful `name`; personal-data fields need their `autocomplete` token (`name`, `email`, `current-password`, `new-password`; WCAG 1.3.5); never turn autocomplete off to avoid password managers (C8)
- Use correct `type` (`email`, `tel`, `url`, `number`) and `inputmode`
- Never block paste (`onPaste` + `preventDefault`)
- Labels clickable (`htmlFor` or wrapping control)
- Disable spellcheck on emails, codes, usernames (`spellCheck={false}`)
- Checkboxes/radios: label + control share single hit target (no dead zones)
- Submit button stays enabled until request starts; spinner during request
- Errors inline next to fields; focus on error follows web-client R14-S12 and R17-S3 (C12)
- Placeholders end with `…` and show example pattern
- Inputs with `value` need `onChange` (or use `defaultValue` for uncontrolled)
- Native `<select>`: explicit `background-color` and `color` from `--color-` tokens (Windows contrast themes)
- Warn before navigation with unsaved changes (`beforeunload` or router guard)

### Animation

- Honor `prefers-reduced-motion`: remove non-essential motion, keep state feedback (C6)
- Animate `transform`/`opacity` only (compositor-friendly)
- Never `transition: all`—list properties explicitly
- Set correct `transform-origin`
- SVG: transforms on `<g>` wrapper with `transform-box: fill-box; transform-origin: center`
- Animations interruptible—respond to user input mid-animation
- Autoplay motion >5 seconds alongside other content needs pause, stop, or hide controls
- Muted decorative loops must stop under `prefers-reduced-motion`

### Typography

- `…` not `...`
- Quotes follow each language's typography in the i18n files: curly `“` `”` in en, `«` `»` in ru; no straight `"` in copy (C11)
- Non-breaking spaces: `10&nbsp;MB`, `⌘&nbsp;K`, brand names
- Loading states end with `…`: `"Loading…"`, `"Saving…"`
- `font-variant-numeric: tabular-nums` for number columns/comparisons
- Use `text-wrap: balance` or `text-wrap: pretty` on headings (prevents widows)

### Content Handling

- Text containers handle long content: `text-overflow: ellipsis` (with `overflow: hidden` and `white-space: nowrap`), `line-clamp`, or `overflow-wrap: anywhere`
- Flex children need `min-width: 0` to allow text truncation
- Handle empty states—don't render broken UI for empty strings/arrays; an empty state is text plus a `src/ui/` action (C14)
- User-generated content: anticipate short, average, and very long inputs

### Images

- An image element needs explicit `width` and `height` attributes (prevents layout shift)
- Below-fold images: `loading="lazy"`
- Above-fold critical images: `fetchpriority="high"`

### Performance

- Large lists (>50 items): `content-visibility: auto` on the rows, or paging; no list library (no new dependencies)
- No layout reads in render (`getBoundingClientRect`, `offsetHeight`, `offsetWidth`, `scrollTop`)
- Batch DOM reads/writes; avoid interleaving
- Prefer uncontrolled inputs; controlled inputs must be cheap per keystroke
- Prefer `<video autoplay muted loop playsinline>` over animated GIF; provide a still alternative
- Short non-essential loops: Safari H.264 MP4 `<picture>` source, `prefers-reduced-motion` media condition, and still fallback

### Navigation & State

- URL reflects state—filters, tabs, pagination, expanded panels in query params
- Links use `<a>`/`<Link>` (Cmd/Ctrl+click, middle-click support)
- Deep-link all stateful UI (if it uses `useState`, consider syncing it to the URL through the router; no new library)
- Destructive actions need a confirmation in a native `<dialog>` (`src/ui/Dialog`) or an undo window—never immediate

### Touch & Interaction

- Touch targets at least 24 CSS px (WCAG 2.5.8); controls use `--control-height` (C9)
- `touch-action: manipulation` (prevents double-tap zoom delay)
- `-webkit-tap-highlight-color` set intentionally
- `overscroll-behavior: contain` in modals/drawers/sheets
- Page rubber-band suppression: `overscroll-behavior: none` on `<html>` inside `@media (pointer: fine)`; keep coarse-pointer page defaults for pull-to-refresh. Test hybrid touch devices: `pointer` describes the primary input, and `none` also disables scroll chaining and native swipe navigation
- During drag: disable text selection, `inert` on dragged elements
- Drag/swipe/pinch/path gestures need tap/click and keyboard alternatives unless essential
- `autoFocus` sparingly—desktop only, single primary input; avoid on mobile

### Safe Areas & Layout

- Full-bleed layouts need `env(safe-area-inset-*)` for notches
- Avoid unwanted scrollbars: `overflow-x: hidden` on containers, fix content overflow
- Flex/grid over JS measurement for layout
- Gaps use `--space-*` tokens; buttons never touch fields, cards or each other

### Locale & i18n

- Dates/times: use `Intl.DateTimeFormat` not hardcoded formats
- Numbers/currency: use `Intl.NumberFormat` not hardcoded formats
- Detect language via `Accept-Language` / `navigator.languages`, not IP
- Brand names, code tokens, identifiers: wrap with `translate="no"` to prevent garbled auto-translation

### Hover & Interactive States

- Buttons/links need a `:hover` state (visual feedback)
- Interactive states increase contrast: hover/active/focus more prominent than rest

### Content & Copy

- All copy goes through i18n (`frontend/src/i18n/`)
- Active voice: "Install the CLI" not "The CLI will be installed"
- Sentence case for headings/buttons (C1)
- Numerals for counts: "8 deployments" not "eight"
- Specific button labels: "Save password" not "Continue"
- Error messages say what happened and how to fix it, not just the problem (C10)
- Second person, neutral tone, no apologies; avoid first person (C10)

### Extra checks

Three points from the pbakaus/impeccable audit guide (`skill/reference/audit.md` @b49177d), written in our own words:

- Reduced motion must still show state: when motion is reduced, a change of state (opened, saved, failed) stays visible as an instant change or a fade. Flag a global rule that sets every animation and transition duration near zero, because it also removes that feedback.
- Interrupted touch gestures: a drag, swipe or slider must recover when the gesture is cut short. Clear drag state on `pointercancel`, on `lostpointercapture` and on window `blur`; set `touch-action` on a surface driven by pointer events, so it neither fights page scroll nor loses the drag to it. A screenshot at 360 px proves the layout, not the gesture: say what was and was not tested.
- `will-change` is a targeted hint, not a default: set it only on the element about to run a known expensive animation and remove it afterwards. Flag it when it is applied broadly or left on at rest.

### Anti-patterns (flag these)

- `user-scalable=no` or `maximum-scale=1` disabling zoom
- `onPaste` with `preventDefault`
- `transition: all`
- `outline: none` without focus-visible replacement
- Inline `onClick` navigation without `<a>`
- `<div>` or `<span>` with click handlers (should be `<button>`)
- Images without dimensions
- Large arrays `.map()` with no `content-visibility` or paging
- Form inputs without labels
- Icon buttons without `aria-label`
- Hardcoded date/number formats (use `Intl.*`)
- `autoFocus` without clear justification
- Animated GIF when compressed video is suitable
- Gesture-only action without tap/click and keyboard alternative

## Output Format

Group by file. Use `file:line` format (VS Code clickable). Terse findings.

```text
## src/Button.tsx

src/Button.tsx:42 - icon button missing aria-label
src/Button.tsx:18 - input lacks label
src/Button.tsx:55 - animation missing prefers-reduced-motion
src/Button.tsx:67 - transition: all → list properties

## src/Modal.tsx

src/Modal.tsx:12 - missing overscroll-behavior: contain
src/Modal.tsx:34 - "..." → "…"

## src/Card.tsx

✓ pass
```

State issue + location. Skip explanation unless fix non-obvious. No preamble.
