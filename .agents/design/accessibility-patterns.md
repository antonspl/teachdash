Source: addyosmani/web-quality-skills, `skills/accessibility/references/A11Y-PATTERNS.md` @afa8da9, MIT, licence in `.agents/design/licenses/addyosmani-web-quality-skills.LICENSE`; modified by teachdash.

# Accessibility code patterns

Patterns for common accessibility requirements, linked from [accessibility.md](accessibility.md). `project-rules.md` wins over this file: CSS uses tokens only (C3), pages use `src/ui/` components and set no `className`.

## Dialogs

Use `src/ui/Dialog`. It is a native `<dialog>` opened with `showModal()`: the rest of the page is inert, so Tab stays inside; Escape cancels it (`onCancel`); focus moves to the first button or field inside and goes back to `returnFocusTo` when it closes. Put the safe choice first. Do not write a focus trap by hand.

## Skip link

Allows keyboard users to bypass repetitive navigation and jump straight to main content.

```html
<a href="#main-content" class="skip-link">Skip to main content</a>
<header><!-- navigation --></header>
<main id="main-content" tabindex="-1"><!-- main content --></main>
```

```css
.skip-link {
  position: absolute;
  top: 0;
  left: 0;
  transform: translateY(-100%);
  background: var(--color-surface);
  color: var(--color-accent);
  padding: 0 var(--space-group);
  z-index: 100;
}

.skip-link:focus {
  transform: none;
}
```

## Error handling

Mark the invalid field with `aria-invalid="true"` and link its message with `aria-describedby` (markup in [accessibility.md](accessibility.md#color-contrast-143-1411), "Don't rely on color alone"). The message says what happened and how to fix it (C10).

Where focus goes on a failed submit follows the specs, not a generic pattern (C12): `openspec/specs/web-client/spec.md` R14-S12 (a submit with a broken rule sends nothing and focuses the first field that shows a hint) and R17-S3 (backend field errors move focus to the topmost field with an error). Focus moves once, to that field, which reads its description; no error summary takes the focus after it. `src/ui/useFieldErrors.ts` does this, and `src/ui/TextInput` and `src/ui/Select` wire `aria-invalid` and `aria-describedby` from their `error` prop.

## Form labels

Every input needs an associated label: either explicit (`for`/`id`) or implicit (wrapping `<label>`). Personal-data fields get their autocomplete tokens (C8).

```html
<!-- ❌ No label association -->
<input type="email" placeholder="Email">

<!-- ✅ Explicit label -->
<label for="email">Email address</label>
<input type="email" id="email" name="email"
       autocomplete="email" required>

<!-- ✅ Implicit label -->
<label>Email address <input type="email" name="email" autocomplete="email" required></label>

<!-- ✅ With instructions -->
<label for="password">Password</label>
<input type="password" id="password" autocomplete="new-password"
       aria-describedby="password-requirements">
<p id="password-requirements">At least 8 characters.</p>
```

## Dragging movements

Any action triggered by dragging must offer a single-pointer alternative (WCAG 2.5.7).

```html
<!-- ❌ Drag-only reorder -->
<ul class="sortable-list" draggable="true">
  <li>Item 1</li>
</ul>

<!-- ✅ Drag + button alternatives -->
<ul class="sortable-list">
  <li>
    <span>Item 1</span>
    <button aria-label="Move item 1 up">↑</button>
    <button aria-label="Move item 1 down">↓</button>
  </li>
</ul>
```

Also applies to sliders, map panning, colour pickers, and similar drag-based widgets: always provide an equivalent click/tap or keyboard path.

## ARIA tabs

Tabs require `role="tablist"`, `role="tab"`, and `role="tabpanel"` with proper `aria-selected`, `aria-controls`, and keyboard support.

```html
<div role="tablist" aria-label="Product information">
  <button role="tab" id="tab-1" aria-selected="true"
          aria-controls="panel-1">Description</button>
  <button role="tab" id="tab-2" aria-selected="false"
          aria-controls="panel-2" tabindex="-1">Reviews</button>
</div>
<div role="tabpanel" id="panel-1" aria-labelledby="tab-1">
  <!-- Panel content -->
</div>
<div role="tabpanel" id="panel-2" aria-labelledby="tab-2" hidden>
  <!-- Panel content -->
</div>
```

Arrow keys should move focus between tabs; the active tab receives `tabindex="0"` while inactive tabs use `tabindex="-1"`. Put the arrow-key handler on the `tablist` element, not on the tab buttons (C7).

## Live regions and notifications

Use `aria-live` to announce dynamic content changes to screen readers without moving focus. `src/ui/StatusMessage` renders `role="status"`.

```html
<!-- Status updates (polite: waits for pause in speech) -->
<div aria-live="polite" aria-atomic="true" class="status">
  <!-- Content updates announced to screen readers -->
</div>

<!-- Urgent alerts (assertive: interrupts) -->
<div role="alert" aria-live="assertive">
  <!-- Interrupts current announcement -->
</div>
```

```javascript
function showNotification(message, type = 'polite') {
  const container = document.getElementById(`${type}-announcer`);
  container.textContent = '';
  requestAnimationFrame(() => {
    container.textContent = message;
  });
}
```

Clear the container before writing to ensure the same message triggers a new announcement.

## Screen reader commands

Quick reference for the most common screen reader shortcuts.

| Action | VoiceOver (Mac) | NVDA (Windows) |
|--------|-----------------|----------------|
| Start/Stop | ⌘ + F5 | Ctrl + Alt + N |
| Next item | VO + → | ↓ |
| Previous item | VO + ← | ↑ |
| Activate | VO + Space | Enter |
| Headings list | VO + U, then arrows | H / Shift + H |
| Links list | VO + U | K / Shift + K |
