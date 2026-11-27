Source: addyosmani/web-quality-skills, `skills/accessibility/SKILL.md` @afa8da9, MIT, licence in `.agents/design/licenses/addyosmani-web-quality-skills.LICENSE`; modified by teachdash.

# Accessibility (a11y)

Accessibility guidelines based on WCAG 2.2, level AA. Goal: make content usable by everyone, including people with disabilities. Read this file when the slice adds or changes interactive elements, forms or dialogs. `project-rules.md` wins over this file. The criteria list is in `accessibility-wcag22.md`, the longer code patterns in `accessibility-patterns.md`.

## Evidence-led check workflow

When the slice changes a rendered page:

1. Run the Playwright suite: `npm run test:e2e` in `frontend/`, against the built app on port 8081 (`docker compose up --build --wait` first). Locate elements in tests by role and name (`getByRole`, `getByLabel`): a test that finds a button by its name proves the button has that accessible name.
2. Do a keyboard pass over the changed flow: Tab and Shift+Tab reach every control in a logical order, focus is always visible, Enter and Space activate, Escape closes a dialog and focus goes back to the control that opened it.
3. Take Playwright screenshots of each page the slice touches and each state it changes, at desktop and 360 px, as the frontend-implementer role says. Check them for contrast, visible focus, target size, reflow and states shown by more than colour.
4. Fix the source, then run the same tests and the keyboard pass again.

Automated checks find only a subset of accessibility barriers: passing tests are not WCAG conformance. No new tools or dependencies are added for this (project rules).

## WCAG principles and level

POUR: content is **P**erceivable through different senses, the interface is **O**perable by all users, content and interface are **U**nderstandable, and content is **R**obust with assistive technologies. Level A (minimum) and level AA (standard) both must pass: new UI meets WCAG 2.2 AA (project rules).

The code snippets below are examples of the patterns only, not code to copy. In our code everything goes through `src/ui/` components: pages set no `className`, and non-interactive elements get no inline handlers.

## Perceivable

### Text alternatives (1.1)

**Images require alt text.** Every image element has an `alt` attribute: a description when it carries meaning (for example "Bar chart showing 40% increase in Q3 sales"), or `alt=""` when it is decorative. A complex image gets a longer description through `aria-describedby` or a `<figcaption>`. Empty states use no images (C14).

**Icon buttons need accessible names:**
```html
<!-- ❌ No accessible name -->
<button><svg><!-- menu icon --></svg></button>

<!-- ✅ Using aria-label -->
<button aria-label="Open menu">
  <svg aria-hidden="true"><!-- menu icon --></svg>
</button>

<!-- ✅ Using visually hidden text -->
<button>
  <svg aria-hidden="true"><!-- menu icon --></svg>
  <span class="visually-hidden">Open menu</span>
</button>
```

**Visually hidden class.** It lives in a `src/ui/` component (pages set no `className`); its size is a new role-named token of one CSS pixel (C3):
```css
.visually-hidden {
  position: absolute;
  width: var(--size-visually-hidden);
  height: var(--size-visually-hidden);
  margin: calc(-1 * var(--size-visually-hidden));
  padding: 0;
  border: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
}
```

### Color contrast (1.4.3, 1.4.11)

| Text size | AA minimum |
|-----------|------------|
| Normal text (below 18 pt, or below 14 pt bold) | 4.5:1 |
| Large text (at least 18 pt, or 14 pt bold; `--text-large` and `--text-title` are large) | 3:1 |
| UI components and graphics (borders, focus indicators, icons) | 3:1 |

All colours are `--color-*` tokens; check the pair a slice uses:
```css
/* ❌ Divider token used for text: about 1.4:1 on the surface */
.low-contrast {
  color: var(--color-divider);
  background: var(--color-surface);
}

/* ✅ Muted text on the surface: about 6.5:1 */
.muted {
  color: var(--color-text-muted);
  background: var(--color-surface);
}
```

**Don't rely on color alone:**
```html
<!-- ❌ Only color indicates error -->
<input class="error-border">

<!-- ✅ Color + text, wired to the field -->
<input aria-invalid="true" aria-describedby="email-error">
<span id="email-error" class="error-message">
  Check the email: it should look like name@example.com
</span>
```

`src/ui/TextInput` and `src/ui/Select` already set `aria-invalid` and `aria-describedby` from their `error` prop.

### Media alternatives (1.2)

Video gets a captions `<track>` (and a descriptions track where the picture carries meaning); audio gets a transcript, for example in a `<details>` element next to it.

## Operable

### Keyboard accessible (2.1)

**All functionality must be keyboard accessible.** Prefer native interactive elements (C7): `<button>`, `<a href>`, `<dialog>` and form controls handle Enter and Space activation, focus and assistive-tech semantics for free. Only add manual keyboard handling when you cannot use a native element.

```html
<!-- ❌ Non-interactive element with click only: not focusable, no keyboard activation -->
<div class="card" onclick="handleAction()">Open</div>

<!-- ✅ Best: use a native button -->
<button type="button" onclick="handleAction()">Open</button>
```

```javascript
// ✅ Only when you MUST use a non-interactive element (e.g. div with role="button"):
// make it focusable AND handle keyboard activation. Never add this to a native
// <button> (C7): Enter and Space already fire click, so you'd double-trigger.
element.setAttribute('role', 'button');
element.setAttribute('tabindex', '0');
element.addEventListener('click', handleAction);
element.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault();
    handleAction();
  }
});
```

**No keyboard traps.** Users must be able to Tab into and out of every component. Dialogs use `src/ui/Dialog`, a native `<dialog>` opened with `showModal()`: it keeps focus inside while open, Escape cancels it, and focus goes back to the opener. Do not build a focus trap by hand (see [Dialogs](accessibility-patterns.md#dialogs)).

### Focus visible (2.4.7)

```css
/* ❌ Never remove focus outlines without a replacement */
*:focus { outline: none; }

/* ✅ :focus-visible for keyboard focus, in token values (C3); --color-accent is about
   6.7:1 on the surface. --focus-ring-width and --focus-ring-offset are examples of
   new role-named tokens. */
:focus-visible {
  outline: var(--focus-ring-width) solid var(--color-accent);
  outline-offset: var(--focus-ring-offset);
}
```

The browser's default focus ring is also fine. A custom ring must reach 3:1 against every background it lands on (1.4.11).

### Focus not obscured (2.4.11), new in 2.2

When an element receives keyboard focus, it must not be entirely hidden by other author-created content such as sticky headers, footers or overlapping panels.

```css
/* ✅ Focused items and anchor targets clear a sticky bar;
   --sticky-bar-height is an example of a new role-named token (C3) */
:target,
:focus {
  scroll-margin-top: var(--sticky-bar-height);
}
```

### Skip links (2.4.1)

Provide a skip link so keyboard users can bypass repetitive navigation. See the [skip link pattern](accessibility-patterns.md#skip-link).

### Target size (2.5.8), new in 2.2

Interactive targets must be at least **24 × 24 CSS pixels** (AA, C9). Exceptions: inline text links, elements where the browser controls the size, and targets where a 24 CSS px circle centred on the bounding box does not overlap another target. Our controls take `min-height: var(--control-height)`, which is above the floor.

### Dragging movements (2.5.7), new in 2.2

Any action that requires dragging must have a single-pointer alternative (e.g., buttons, inputs). See the [dragging movements pattern](accessibility-patterns.md#dragging-movements).

### Timing (2.2)

Let users extend time limits. A warning is a `src/ui/Dialog` with sentence-case copy from i18n (C1, C10), for example the title "Session expiring" and the text "Your session ends in 2 minutes."

```tsx
// ✅ Native <dialog> through src/ui/Dialog; the safe choice comes first
<Dialog title={t('session.expiringTitle')} onCancel={extendSession} onClose={extendSession}>
  <Text>{t('session.expiringText')}</Text>
  <ButtonRow>
    <Button onClick={extendSession}>{t('session.extend')}</Button>
    <Button variant="secondary" onClick={logout}>{t('session.logOut')}</Button>
  </ButtonRow>
</Dialog>
```

### Motion (2.3)

Reduced motion removes non-essential motion but keeps state feedback (C6): a change of state stays visible, as an instant change or a fade instead of a slide. No global rule that sets every duration near zero.

```css
/* ✅ Slide plus fade normally; fade only when motion is reduced */
.panel {
  transition-property: transform, opacity;
}

@media (prefers-reduced-motion: reduce) {
  .panel {
    transition-property: opacity; /* the open state still shows */
  }
  .decorative-loop {
    animation: none;
  }
  html {
    scroll-behavior: auto;
  }
}
```

## Understandable

### Page language (3.1.1, 3.1.2)

`<html lang="…">` names the UI language; a phrase in another language gets its own `lang`, for example `<span lang="fr">bonjour</span>`.

### Consistent navigation (3.2.3)

Navigation is consistent across pages: same order, same labels, `aria-current="page"` on the current link, a `<nav>` with an `aria-label`.

### Consistent help (3.2.6), new in 2.2

If a help mechanism (contact info, chat widget, FAQ link, self-help option) is repeated across multiple pages, it must appear in the **same relative order** each time.

### Form labels (3.3.2)

Every input needs a programmatically associated label. See the [form labels pattern](accessibility-patterns.md#form-labels).

### Error handling (3.3.1, 3.3.3)

Show each error under its field, set `aria-invalid="true"` on the field and link the message with `aria-describedby`; the message says what happened and how to fix it (C10). Where focus goes on a failed submit follows the specs (C12): `openspec/specs/web-client/spec.md` R14-S12 (focus on the first field with a hint) and R17-S3 (focus on the topmost field with a backend error). `src/ui/useFieldErrors.ts` implements this. See the [error handling pattern](accessibility-patterns.md#error-handling).

### Redundant entry (3.3.7), new in 2.2

Don't force users to re-enter information they already provided in the same session. Auto-populate from earlier steps, or let users select from previously entered values. Exceptions: security re-confirmation and content that has expired.

### Accessible authentication (3.3.8), new in 2.2

Login flows must not rely on cognitive function tests (e.g., remembering a password, solving a puzzle) unless at least one of:
- A copy-paste or autofill mechanism is available
- An alternative method exists (e.g., passkey, SSO, email link)
- The test uses object recognition or personal content

Allow paste in password fields and give them their autocomplete tokens (C8): `autocomplete="current-password"` on login, `autocomplete="new-password"` when setting one.

## Robust

### ARIA usage (4.1.2)

**Prefer native elements:**
```html
<!-- ❌ ARIA role on div -->
<div role="button" tabindex="0">Click me</div>
<div role="checkbox" aria-checked="false">Option</div>

<!-- ✅ Native elements -->
<button>Click me</button>
<label><input type="checkbox"> Option</label>
```

**When ARIA is needed,** use the correct roles and states. See the [ARIA tabs pattern](accessibility-patterns.md#aria-tabs).

### Live regions (4.1.3)

Use `aria-live` regions to announce dynamic content changes without moving focus; `src/ui/StatusMessage` renders `role="status"`. See the [live regions pattern](accessibility-patterns.md#live-regions-and-notifications).

## Testing checklist

Automated: `npm run test:e2e` (Playwright) with role and label locators, plus the screenshots of the workflow above. Manual:

- [ ] **Keyboard navigation:** Tab through the changed flow, use Enter/Space to activate
- [ ] **Screen reader:** Test with VoiceOver (Mac), NVDA (Windows), or TalkBack (Android)
- [ ] **Zoom:** Content usable at 200% zoom
- [ ] **Reflow:** No horizontal scroll at 360 px (the narrow screenshot); WCAG 1.4.10 sets the threshold at 320 CSS px
- [ ] **High contrast:** Test with Windows High Contrast Mode
- [ ] **Reduced motion:** Test with `prefers-reduced-motion: reduce`; state changes still show
- [ ] **Focus order:** Logical and follows visual order
- [ ] **Target size:** Interactive elements meet the 24 CSS px floor

See the [screen reader commands reference](accessibility-patterns.md#screen-reader-commands) for VoiceOver and NVDA shortcuts.

## Common issues

Findings are triaged as `project-rules.md` C13 says, not by these groups.

- **Forms and names:** missing form labels, missing image alt text, missing accessible names on icon buttons, missing error identification
- **Seeing:** insufficient color contrast, no focus indicators
- **Moving around:** keyboard traps, missing skip links, missing landmark regions, missing heading structure, inconsistent navigation
- **Content:** missing page language, non-descriptive link text, auto-playing media, timing without controls

## References

- [WCAG 2.2 Quick Reference](https://www.w3.org/WAI/WCAG22/quickref/)
- [WAI-ARIA Authoring Practices](https://www.w3.org/WAI/ARIA/apg/)
- [WCAG criteria reference](accessibility-wcag22.md)
- [Accessibility code patterns](accessibility-patterns.md)
