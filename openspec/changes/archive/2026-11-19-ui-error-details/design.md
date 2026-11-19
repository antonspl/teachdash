# Design

## Context

- `frontend/src/api/client.ts`: `ApiError` has `status`, `code`, `isProblem`;
  `toApiError` reads the body once and `parseProblemCode` takes only `code`.
- Every form keeps one form-level error string, shown by `StatusMessage` (login,
  set password, dialog) or the forms' `{tone, text}` message (profile, password).
  `ProfileForm` and `PasswordForm` clear it on edit; login, set password and the dialog
  clear it only on the next submit.
- `ui/TextInput.tsx` (and `PasswordInput` through it) has an `error` prop: a message
  under the field, `aria-invalid` and `aria-describedby`. The R14 hints use it.
  `ui/Select.tsx` has no such prop.
- Field names: the request bodies use the backend's names, except the password form's
  `current`/`next` state (`currentPassword`/`newPassword`) and the set-password page's
  `token`, which comes from the URL and has no input.

## Goals / Non-Goals

**Goals:** one shared way to read, place, focus and clear backend field errors, used by
all five forms; existing tests keep passing, except the one for the modified R3-S10.

**Non-Goals:** a form library; changing how client-side hints work; field errors for
responses other than `VALIDATION_FAILED`, `EMAIL_TAKEN` and `WRONG_PASSWORD` (the
backend sends `errors` only for these).

## Decisions

### D1 `ApiError.fieldErrors`

`ApiError` gets `readonly fieldErrors: readonly FieldError[]` (`{field, code}`, empty
when there are none). `parseProblemCode` becomes `parseProblem(text)` returning
`{code, fieldErrors}`; entries that are not objects with string `field` and `code` are
dropped. An empty list rather than `undefined`, so callers need no null checks. The
message stays `messageForCode(code)`.

Alternative: parse `errors` in each form. Rejected: five copies of the same guard.

### D2 `useFieldErrors` hook

`frontend/src/ui/useFieldErrors.ts`:

```ts
useFieldErrors<K extends string>(fields: { key: K; backend: string; ref: RefObject<HTMLElement> }[],
                                 messages?: Partial<Record<string, string>>)
// returns { errors: Partial<Record<K, string>>, place(error): boolean, clear(key), clearAll() }
```

- `fields` is in the form's order; `backend` maps the backend name to the form key.
- `place(error)` returns `false` and changes nothing when `fieldErrors` is empty, or any
  entry names an unknown field, or has a code without a message. Otherwise it stores one
  message per field (the first entry for a field wins) and asks for focus on the first
  field, in `fields` order, that has an error. The form shows its form-level message
  only when `place` returned `false`.
- Messages: `fieldErrors.<CODE>` from `en.json` for `REQUIRED`, `TOO_SHORT`, `TOO_LONG`,
  `INVALID_FORMAT`, `INVALID_TYPE`; `EMAIL_TAKEN` uses `errors.EMAIL_TAKEN`;
  `WRONG_PASSWORD` uses the form's override (`profile.wrongPassword`), passed in
  `messages`.
- `clear(key)` is called from the field's `onChange`; `clearAll()` at the start of
  every submit.

Alternative: a per-form `useState` and a shared pure `placeFieldErrors` function.
Rejected: focus and clearing would be repeated in five forms.

### D3 Focus after the inputs are enabled again

The profile, password and dialog inputs are `disabled` while the request runs, and
`focus()` on a disabled input does nothing. `place` stores the key to focus; an effect
in the hook focuses it after the render in which `submitting` turned false, then clears
the stored key. The effect runs once per stored key, so StrictMode's double effect does
not move the focus twice. The dialog's `mounted` guard also covers `place`.

Alternative: `flushSync` in the catch block. Rejected: harder to read, and the dialog
can unmount in between.

### D4 Client hint before backend error

Not in the spec, because it cannot be reached through the UI (engineer's decision after
the spec critique). For the password fields the `error` prop gets `hint ?? backendError`. In practice the
two never meet: a submit with a hint sends nothing, and editing a field removes its
backend error before a hint for it can appear. The repeat field has no backend name.

### D5 Select error slot

`ui/Select.tsx` gets the same `error?: string` prop as `TextInput`, with the same
markup and styles, so the message sits under the arrow row and the select is marked
invalid with the message as its description.

### D6 Set-password form-level message (#89)

`SetPasswordPage` clears its form-level `error` in the fields' `onChange`, like
`ProfileForm`. Login and the dialog keep clearing it only on submit (out of scope).

### D7 Tests

- Unit (Vitest): R5-S5, R5-S6 in `client.test.ts`; R17-S1 to R17-S4, R17-S8, R18-S1 and R18-S2 in the pages' tests
  with mocked `fetch` responses that carry `errors`, pressing the real button.
  R17-S2 is one parameterised test over the five forms.
- E2E (Playwright, real backend): R17-S5, R17-S6, R17-S7. All other scenarios use mocked
  responses.
- Existing tests follow the fallback path, because none of their fixtures carries
  `errors`; web-profile R3-S6 and R3-S7 now say so in their GIVEN, and their tests stay
  as they are. Only the web-profile R3-S10 test changes, to follow its modified scenario.

## Risks / Trade-offs

- [The backend adds a new code] → no field message, so the form falls back to its
  form-level message (R17-S4).
- [A real backend with `errors` makes the old form-level texts rare] → they stay for
  old or unexpected responses; removing them is a later clean-up.
- [Login clears the password on failure] → kept (engineer's decision); a `password`
  error is shown under the now empty field (R17-S2), and the user retypes it anyway.
- [Stacked on two open PRs] → re-base to `main` after #84 and #88 are merged.

## Migration Plan

None. No backend or data changes.
