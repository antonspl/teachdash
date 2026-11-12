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

### D1 The arrow is a pseudo-element on a wrapper, not a background image

`Select.tsx` wraps the `<select>` in an element with `position: relative`. The select
gets `appearance: none` and a right padding of inset + arrow width + gap. The wrapper's
`::after` draws a chevron, placed `--space-1` from the right border, vertically centred,
with `pointer-events: none` so a click on it still opens the list.

- The chevron is coloured from the text colour token, so it follows the theme. A
  data-URI SVG background on the select cannot read CSS variables; that is why it was
  rejected.
- A `<select>` cannot carry its own `::after` in all browsers, hence the wrapper.
- No image file and no external URL (constitution section 10).
- Text that does not fit is cut off by the select itself (`text-overflow: ellipsis`);
  the right padding keeps it away from the arrow.
- New sizes (arrow width) are added as tokens if the token check requires it.

### D2 Per-field hints come from `passwordRules.ts`

Add `passwordHints(password, repeat, submitted)` returning
`{ password?: PasswordProblem; repeat?: PasswordProblem }`:

- `password`: `tooShort` or `tooLong` when the new password is not empty and breaks the
  rule; when `submitted`, an empty one is `tooShort`.
- `repeat`: `mismatch` when the repeat is not empty and differs; when `submitted`, also
  when it is empty and the new password is not.

`passwordProblem` stays as the gate on submit, so both functions share the constants and
the byte count. Unit tests cover the table of cases.

### D3 Forms pass hints to the fields; the form message is for server replies

Both forms compute the hints on every render from their state and pass them to
`PasswordInput` through the existing `error` prop. Each form keeps a `submitted` flag:
set when a submit is stopped by a broken rule, cleared on a successful change (profile
form; the set-password page navigates away).

On submit with a broken rule the form no longer copies the problem into its
`StatusMessage`; the hint under the field is the message. `StatusMessage` keeps showing
success and backend errors, so web-profile R3-S2, R3-S6 to R3-S10 are untouched.

Alternative considered: keep the form message and add the hints. Rejected: the same
sentence would be shown twice.

### D4 Existing scenario tests keep their IDs

web-profile R3-S3 to R3-S5 and web-auth R5-S3, R5-S4 and R5-S8 say that a message is
shown and no request is sent on submit. That stays true; only where the tests look for
the text may change (a field description, not a status message). Their assertions on the
text and on "no request" are not weakened.

### D5 Tests

- R13: Playwright, because it is about real layout. Gaps are measured from element boxes
  and computed styles in the desktop and phone projects. R13-S4 holds the save request
  open with a route handler.
- R14: Vitest with Testing Library, one test per scenario, each covering both forms
  where the scenario says so.
- Before the code gate, screenshots of `/profile` and the "Add teacher" dialog at
  desktop and 360-pixel width are looked at by eye, in Chromium, Firefox and WebKit.

### D6 Stacked branch

The branch starts from `ui-profile` and the PR's base is `ui-profile`. After #84 is
merged the base is changed to `main` (no rebase needed after a merge commit). Changing
the base dismisses a GitHub approval, so it is done before the final review is asked.

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
