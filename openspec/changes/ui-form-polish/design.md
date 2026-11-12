# Design

## Context

See proposal.md for the motivation.

- `ui/Select.tsx` renders a native `<select>` with the `.input` class of
  `ui/Field.module.css`. The browser draws the arrow and ignores our padding. The select
  is used by the profile form and the "Add teacher" dialog.
- `passwordRules.ts` has one function, `passwordProblem(password, repeat)`, which returns
  the first broken rule. `PasswordForm` and `SetPasswordPage` call it on submit and show
  the result in the form's single `StatusMessage`.
- `ui/TextInput.tsx` (and so `PasswordInput`) already takes an `error` text: it shows it
  under the field, marks the input `aria-invalid` and links the text with
  `aria-describedby`. Nothing uses it for passwords yet.
- `npm run lint` runs `scripts/check-design-tokens.mjs`, so CSS values come from tokens.

## Goals / Non-Goals

**Goals:**

- One arrow style for every select, defined once in the shared component.
- One definition of "which hint goes under which field", used by both password forms.

**Non-Goals:**

- A custom dropdown list. The control stays a native `<select>`.
- Changing the password rules or their translation keys.
- A live region for the hints (#73, item 3).

## Decisions

### D1 The arrow is a real element beside the select, not a background image

`Select.tsx` wraps the `<select>` in an element with `position: relative` that stretches
to the full width of the field, as the select does today. Inside it:

- the `<select>` with `appearance: none`, `width: 100%`, `min-width: 0`,
  `text-overflow: ellipsis` and a right padding of inset + arrow width + gap. `.input`
  has `flex: 1`, which only works in a flex row, so the width is set explicitly;
- an `aria-hidden` element with an inline SVG chevron, positioned `--space-1` from the
  inner edge of the right border, vertically centred, with `pointer-events: none` so a
  click on it reaches the select.

Why this shape:

- The chevron's stroke is `currentColor` from the text colour token, so it follows the
  theme. A data-URI SVG background on the select cannot read CSS variables.
- A real element has a box, so the Playwright tests can measure where the arrow is. A
  `::after` pseudo-element has none. A `<select>` cannot carry its own `::after` in all
  browsers anyway.
- The SVG's drawing fills its box, so the visible gap is the box gap.
- No image file and no external URL (constitution section 10).
- New sizes (arrow width) go into `styles/tokens.css`; the token check forbids raw sizes
  elsewhere.

### D2 Per-field hints come from `passwordRules.ts`

Add `passwordHints(password, repeat)` returning
`{ password?: PasswordProblem; repeat?: PasswordProblem }`:

- `password`: `tooShort` or `tooLong` when the new password is not empty and breaks the
  rule;
- `repeat`: `mismatch` when the repeat is not empty and differs from the new password.

An empty field gives no hint. There is no "was submitted" state: every password field is
`required`, so the browser stops a submit with an empty field and shows its own message,
as it does today. `passwordProblem` stays as the gate on submit, so both functions share
the constants and the byte count. Unit tests cover the table of cases.

Alternative considered: a `submitted` flag that turns an empty repeat into "do not match"
after a stopped press. Rejected after the spec critique: it needs rules for when the
flag is cleared on each page and shows "at least 8 characters" on a field the user has
just cleared to retype, for a case the browser already handles.

### D3 Forms pass hints to the fields; the form message is for server replies

Both forms compute the hints on every render from their state and pass them to
`PasswordInput` through the existing `error` prop.

On submit with a broken rule the form no longer copies the problem into its
`StatusMessage`; the hint under the field is the message. `StatusMessage` keeps showing
success and backend errors, so web-profile R3-S2, R3-S6 to R3-S10 are untouched.

Both forms clear their form message before the rule check. `PasswordForm` already does;
`SetPasswordPage` clears it only after the check today, which would leave an old backend
error next to a new hint (R14-S11).

Alternative considered: keep the form message and add the hints. Rejected: the same
sentence would be shown twice.

### D4 Existing scenario tests keep their IDs

web-profile R3-S3 to R3-S5 and web-auth R5-S3, R5-S4 and R5-S8 say that a message is
shown and no request is sent on submit. That stays true; only where the tests look for
the text may change (a field description, not a status message). The hint is now there
before the press, so finding the text no longer proves the gate. These tests already
press the real button and check that no request was sent; each gets one more check,
that the hint is still shown after the press. Their helpers are not rewritten.

### D5 Tests

- R13: Playwright, because it is about real layout. Positions come from the bounding
  boxes of the select and the arrow element and from computed styles: `appearance: none`
  for "the browser's arrow is switched off", the paddings and border widths for the
  gaps and the text area, computed `text-overflow: ellipsis` for "cut off with an
  ellipsis" (rendered text in a select cannot be measured), `scrollWidth <= clientWidth`
  for "no horizontal scroll". R13-S4 holds the save request open with a route handler.
- R13-S3 clicks with `page.mouse.click(x, y)` at the centre of the arrow's box; a locator
  click on the arrow would time out, because the arrow is never the hit target. The main
  check is that `document.elementFromPoint(x, y)` is the select; the focus is the second.
- The Playwright projects are Chromium and the Pixel 7 and iPhone 14 profiles. Firefox
  has no automated check; it is covered by the screenshots below.
- R14: Vitest with Testing Library. One parameterised test per scenario (`it.each` over
  the two forms, the title starting with the scenario ID); `check-scenario-coverage.sh`
  matches IDs by prefix and accepts this. Scenarios for one form are plain tests.
- R14 tests press the real submit button and fill every required field the scenario
  does not leave empty. `fireEvent.submit` would skip the browser's `required` check, and
  an empty required field would make "no request" pass for the wrong reason.
- R14-S9 asserts that the fetch mock has no calls.
- Before the code gate, screenshots of `/profile` and the "Add teacher" dialog at
  desktop and 360-pixel width are looked at by eye, in Chromium, Firefox and WebKit.

### D6 Stacked branch

The branch starts from `ui-profile` and the PR's base is `ui-profile`. After #84 is
merged the base is changed to `main`. This repository merges PRs with a merge commit, so
no rebase is needed; if #84 is squash-merged instead, the branch is first moved with
`git rebase --onto main ui-profile`, which needs the engineer's word because it rewrites
a pushed branch. Changing the base dismisses a GitHub approval, so it is done before the
final review is asked.

No new dependencies. No Flyway migrations.

## Risks / Trade-offs

- [A hint from the first character is noisy: "at least 8 characters" shows for the first
  seven keystrokes] → The engineer's choice (proposal, Clarifications). The hint is one
  short line under the field and the layout below moves by one line only.
- [Hints are not announced by screen readers while typing] → They are linked to the field
  and read when the field is focused; a live region is #73, item 3.
- [`appearance: none` differs between browsers] → Checked by eye in three engines (D5);
  the Playwright projects cover Chromium and the phone profiles.
- [The layout jumps when a hint appears] → Accepted; reserving an empty line under every
  field would add space to all forms.
