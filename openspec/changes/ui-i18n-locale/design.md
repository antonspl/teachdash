# Design

## Context

- `frontend/src/i18n/index.ts` runs a top-level `await i18next.init({lng:'en', fallbackLng:'en', resources:{en}})`; `main.tsx` imports it before the first render. Tests import the same singleton.
- `index.html` has `<html lang="en">`; nothing changes it.
- `ui/AppHeader.tsx` has a `.user` group (flex, wrap) with the name, the navigation button and "Log out". `LoginPage` and `SetPasswordPage` render `<Page>` (a `<main>`) with a `<Title>` and a `<Form>`.
- `ui/Select.tsx` is the native select with the R13 arrow; `ui/LanguageSelect.tsx` is a different thing: it loads course and native languages from `/api/languages`.
- Errors are codes translated in the frontend (`api/client.ts`); `errors.generic` is the fallback.

## Goals / Non-Goals

**Goals:** choose, store, switch and announce (`<html lang>`) the UI language; make the switch visible now; close #92 and #172's UI part; one neutral tone in both languages.

**Non-Goals:** the full Russian translation (#103); content language (#114); the Profile menu and login band (#201, #202, #205); a detection library; backend changes. No new dependency, so there is nothing to justify. No Flyway migration.

## Decisions

### D1 Detector in `frontend/src/i18n/`

A pure function `detectLanguage(supported, stored, browserLanguages)` returns the stored value if supported, else the first browser language whose primary subtag (before `-`, lowercased) is supported, else `en`. A small `storage` wrapper reads and writes `teachdash.lang` inside try/catch and keeps the last value in a module variable, so a throwing localStorage still lets the session work. `index.ts` computes `supported = Object.keys(resources)`, calls the detector with `navigator.languages`, passes the result as `lng`, and sets `document.documentElement.lang`. An `i18next` `languageChanged` listener sets `lang` again, so the attribute follows every change. A stored unsupported value is only ignored: nothing writes to storage until the user picks a language.

Alternative: `i18next-browser-languagedetector`. Rejected: a new dependency for about 15 lines (constitution section 7).

### D2 Resources

`ru.json` holds `uiLanguage.label` ("Язык"), `uiLanguage.name.en` and `.ru`, and `errors.TARGET_ACCOUNT_DEACTIVATED`; `en.json` holds the same keys plus everything else. `uiLanguage.name.*` has the same value ("English", "Русский") in both files, so an endonym never depends on the UI language. i18next's `fallbackLng: 'en'` already falls back per key. The supported list comes from the `resources` object, so #103 adds languages or strings without touching the detector.

### D3 `UiLanguageSelect`

A new `ui/UiLanguageSelect.tsx` wraps `Select` and gives it a visually hidden label and per-option `lang`. `Select` gets two small optional props for this: `hideLabel` (the label gets the visually hidden class) and `optionLang` (a map from option value to `lang`). It reads `i18n.language` and calls `i18next.changeLanguage` and the storage wrapper on change. The select is not controlled by a page's state, so the pages stay unaware of it. It is a separate file from `LanguageSelect`, which keeps its own role.

### D4 Hidden label and endonyms

The label is in the current UI language ("Language" or "Язык") while the options are endonyms. Someone who cannot read the label can still find their language by its name. The label is visually hidden because the endonym text is the visible cue; it still gives the select its accessible name.

### D5 Tokens and class

`tokens.css` gets `--lang-select-width: 9rem` and `--size-visually-hidden: 1px`. A `.visuallyHidden` class (in `Field.module.css`) uses `--size-visually-hidden` for width and height, with the usual clip technique. The token lint (`npm run lint`) must accept both.

### D6 Neutral copy

`en.json` strings are rewritten (key, new text); the tone stays second person, sentence case, no apologies, plain verbs.

| Key | New |
|---|---|
| `errors.INVALID_CREDENTIALS` | The email and password do not match. |
| `fieldErrors.TOO_SHORT` | This value is too short. |
| `fieldErrors.TOO_LONG` | This value is too long. |
| `fieldErrors.INVALID_FORMAT` | This value is not valid. Please check it. |
| `fieldErrors.INVALID_TYPE` | same as INVALID_FORMAT |
| `login.linkHint` | Do you have no password, or did you forget it? Ask your teacher or administrator for a new link. |
| `issuedLink.note` | You can see this link only now. It works once, within 7 days, and replaces any earlier unused link. Do not open it in this browser, or your admin session will end. |
| `issuedLink.copied` | Copied. |
| `issuedLink.copyByHand` | The link could not be copied. It is selected, so you can copy it by hand. |
| `adminTeachers.empty` | No teachers yet. Use the button above to add the first one. |
| `adminTeachers.create.validationFailed` | Some values were not accepted. Please check the name, the email, the time zone and the native language. |
| `adminTeachers.deactivate.body` | {{name}} will no longer be able to sign in, and their students will be released. The students are not restored if you reactivate the account later. |
| `profile.saved` | Saved. |
| `profile.validationFailed` | Some values were not accepted. Please check the name and the time zone. |
| `profile.validationFailedTeacher` | Some values were not accepted. Please check the name, the time zone and the native language. |
| `profile.passwordChanged` | Your password has been changed. You were signed out on your other devices. |
| `profile.wrongPassword` | The current password is incorrect. Please try again. |
| `profile.passwordValidationFailed` | The passwords were not accepted. Please check them and try again. |

Kept as they are: `adminTeachers.justAdded` ("Just added"), `profile.title`, `profile.email`, "Please", "Loading...". The implementer checks each "now" text against `en.json` before the rewrite. The new `errors.TARGET_ACCOUNT_DEACTIVATED` is in both files (R2-S4). Only two existing test literals break, `useFieldErrors.test.tsx:103` and `api/client.test.ts:76`; both read `en.<key>` after this change. The design docs listed in Clarifications 2 are edited in the same change so they do not contradict the copy.

### D7 Tests

- Vitest: the detector as a pure function with a stubbed `navigator.languages` (R19-S1 to R19-S4); the storage failure with a throwing `localStorage` (R19-S5); `<html lang>` for `en` and `ru` (R19-S6, R20-S3, R20-S4); R2-S2 and R2-S3 with a test instance, like R2-S1; R2-S4 in `client.test.ts`; R20-S1 to R20-S4 on the real pages. Tests that change the language or storage reset them in `afterEach` (`changeLanguage('en')`, `localStorage.clear()`, `document.documentElement.lang`), so they are not order-dependent.
- Playwright: `locale: 'en-US'` in `use` of `playwright.config.ts` pins all three projects, so the English selectors keep working; R20-S5 seeds `teachdash.lang` as `ru` before the page loads (`addInitScript`); R20-S6 checks no horizontal scroll at 360 in `en` and `ru` on the login page and the header.
- Existing E2E tests are not otherwise changed.

## UI design

### Screens touched

Cabinet (teacher and student), Profile and admin pages (header); login and set-password pages. The not-found page and loading and guard pages are not touched.

### Layout (desktop and 360 px)

- Header: the language select is the last child of `.user` (flex, wrap, gap `--space-button-gap`), after "Log out". Height `--control-height`, width `--lang-select-width`. At 360 it wraps below the buttons and does not overflow (`web-client` R7); the header can grow by one line. A longer Russian name still fits 9rem. No extra row on desktop.
- Login and set-password: the first child of `<main>` is a block with the select right-aligned, `--space-block` above the title, `--lang-select-width` wide, outside `<Form>`, so Enter in the form does not touch it. Same width at 360.

```
Header (desktop)
[ teachdash ]            Anna P. (teacher) [Profile] [Log out] [English v]

Login / set password
                                                      [English v]
  Log in
  [ Email          ]
  ...
```

### States per screen

No empty, error or loading state: the language list is fixed. Disabled is not used. Focus is the standard `:focus-visible` ring. Success is the instant re-render.

### Copy

Keys: `uiLanguage.label` (hidden label; "Language" / "Язык"), `uiLanguage.name.en` ("English") and `uiLanguage.name.ru` ("Русский"), same values in both files; `errors.TARGET_ACCOUNT_DEACTIVATED`. The neutral rewrites are in D6. Russian strings in this change use the formal "вы".

### A11y

- Focus order: in the header the select is last, after "Log out"; on login and set-password it is first in the DOM, before the title and the form.
- Keyboard: the native select behaviour; Enter in the form does not reach it.
- Name: the visually hidden label gives the accessible name; each `<option>` has its own `lang`; `<html lang>` follows the UI language.
- On change, focus stays on the select; no `aria-live` announcement is added, because the page text itself changes language and `lang` is updated.

### Components and tokens

- Reused: `Select` (R13 arrow, surface fill, `--color-border` edge, `--radius`, inset arrow), `AppHeader`, `Page`.
- New component: `UiLanguageSelect`. New props on `Select`: `hideLabel`, `optionLang`.
- New tokens: `--lang-select-width: 9rem` (no existing token fits) and `--size-visually-hidden: 1px` (named by the design code, not yet in `tokens.css`). No other new token. New class: `.visuallyHidden`.

### Visual plan per screen and state

Cabinet, Profile and admin header, desktop and 360, English and Russian:
- Hierarchy: tertiary. Primary is the page title and content. The header keeps its current order (name, who, "Profile", "Log out"); the language select comes last and must not outweigh "Log out".
- Control: native select on the `Select` styling, visible text is the endonym, label hidden, each option with its own `lang`.
- Layout and density: as in Layout above.

Login and set-password, desktop and 360:
- Hierarchy: tertiary, above the title row. The title and the form stay primary.
- Layout: as in Layout above. No band and no globe icon; both are reserved for #205 and #197.
- Behaviour: on change, the page text, the select's own label and `<html lang>` update. Focus stays on the select. Storage is written once on change.

Later steps (not for this change): #201 and #202 replace the header slot with the Profile menu `menuitemradio` items. #205 puts the same component in the login band. They reuse the component, its keys and its `lang` handling, and drop only the header wrapper.

## Risks / Trade-offs

- [Unguarded localStorage throws inside the top-level await] -> blank page. The storage wrapper catches every access (R19-S5).
- [Unit tests leak language or storage] -> order-dependent failures. Every test that changes them resets them in `afterEach` (D7).
- [Name clash with `LanguageSelect`] -> a separate file and name, with a comment in each pointing to the other's role.
- [A Russian user sees mostly English until #103] -> accepted by the engineer (Clarifications 3).
- [The hidden label is in the UI language, the options are endonyms] -> intended (D4).
- [Design docs say informal] -> edited in the same change (tasks 6.1).

## Migration Plan

None. No backend or data changes. Existing users have no stored choice and get the browser language or `en`.
