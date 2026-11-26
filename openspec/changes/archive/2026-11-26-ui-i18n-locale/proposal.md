# Proposal

Issues: #102 (closes it), #92 (closes it), #172 (closes it, except the content-language part, which goes to #114).

## Clarifications

Decisions by the engineer:

1. New requirement numbers are `web-client` R19-R20. R13, R14, R17 and R18 are taken; R15-R16 stay reserved for #103.
2. UI copy uses a neutral tone in both UI languages. This change rewrites the informal `en` strings (table in design D6) and edits the design docs that still require informal copy, so they agree: `.agents/design/design-code.md:367`, `.agents/design/project-rules.md:21-22` and rule C10 (around line 56), `.agents/design/frontend-design-copy.md:34`, `.agents/design/checklist.md:136`. "Informal and friendly, like talking to a friend" becomes a neutral tone; the rest of those rules (sentence case, second person, no apologies, plain verbs) stays unless it contradicts neutral. Editing `.agents/design/` is required by the task (AGENTS.md permits it then); it is a tooling/docs change per constitution section 1.
3. The language switch is visible in this change. `ru.json` ships with only this change's own strings (switch label, language names, `errors.TARGET_ACCOUNT_DEACTIVATED`); i18next falls back per key to `en`, so a Russian user sees mostly English until #103 delivers the full Russian strings. #103's line "the language switch becomes visible" is superseded by this change.
4. Control: a new component `UiLanguageSelect` in `frontend/src/ui/`, not the existing `LanguageSelect` (which picks course and native languages and stays separate). A native `<select>` reusing the `Select` styling (R13 arrow), with a visually hidden label "Language" / "Язык". Options are endonyms "English" and "Русский", each `<option>` with its own `lang`. Endonyms live in keys `uiLanguage.name.en` and `uiLanguage.name.ru` with the same value in both resource files (the `languages.*` keys are not reused). Width `--lang-select-width` (9rem).
5. Placement: the cabinet, Profile and admin pages show it as the last control in `AppHeader`'s user group, after "Log out", wrapping with the buttons at 360 px. The login and set-password pages show it in a right-aligned first row inside `<main>`, `--space-block` above the title, outside the form. No switch on the not-found page or on loading and guard pages. #201, #202 and #205 later move it and reuse the component.
6. On change: immediate re-render, focus stays on the select, the choice is saved to localStorage, `document.documentElement.lang` is set at start and on every change. No reload, no toast.
7. Detection: own code in `frontend/src/i18n/` (about 15 lines, no new dependency): stored choice, then `navigator.languages` in order with the primary subtag lowercased (`ru-RU` becomes `ru`), first supported, then `en`. localStorage key `teachdash.lang`. The supported list is derived from the keys of the bundled resources. A stored value that is not supported is ignored (and not overwritten until the user picks again). localStorage get and set are wrapped in try/catch: on failure the choice is kept in memory for the session and the app still starts. Detection runs before the first render (the existing top-level `await` init in `i18n/index.ts`). `index.html` keeps `lang="en"` as a fallback; no inline script.
8. E2E: `locale: 'en-US'` in `use` of `frontend/playwright.config.ts`, applying to all three projects (desktop-chromium, Pixel 7, iPhone 14), plus one E2E test that seeds `ru` as the stored choice and checks the stored-choice path (the switch shows "Русский", `<html lang="ru">`).
9. Tokens: add `--lang-select-width: 9rem` and `--size-visually-hidden: 1px` to `tokens.css` now, with the names and values of `design-code.md:116` and `:131`; add a small visually hidden CSS class.
10. Copy details: keep "Just added" (`design-code.md:311` uses it); keep "Please"; Russian strings in this change use the formal "вы"; the new `errors.TARGET_ACCOUNT_DEACTIVATED` is "This account has been deactivated, so a reset link cannot be created. Reload the list to see the current status."

11. Login and set-password row spacing: add `--space-block` (1.25rem, name and value from `design-code.md:70`) to `tokens.css` now; the row sits `--space-block` above the title.
12. On login and set-password the select is first in the DOM and in tab order (the first Tab stop, before email); visual order equals DOM order. The cost is in design Risks.
13. Header at 360 px: when the select wraps under the buttons it is left-aligned (the `.user` flex-wrap default), with gap `--space-button-gap`. The login row is a right-aligned block in `<main>`, width `--lang-select-width`.
14. An error already on screen when the user switches stays in the old language until the next action (`api/client.ts` resolves `errors.<code>` at throw time). Accepted; stated in R20 and its scenario R20-S7. Amended by 22.
15. The Russian `errors.TARGET_ACCOUNT_DEACTIVATED` is drafted by the spec-writer in formal "вы" (design D2) and the engineer approves it at the spec gate. A scenario with the Russian language active (R2-S5) keeps the key from going missing in `ru.json`.
16. `login.linkHint` becomes "No password, or forgot it? Ask your teacher or administrator for a new link."
17. Detection edge cases: if `navigator.languages` is empty or undefined, use `navigator.language`, then `en`; a stored value in another case ("RU") is lowercased before the supported check; if reading the `window.localStorage` property itself throws (SecurityError), not only `getItem` and `setItem`, the app starts with the choice kept in memory.
18. No sync between tabs: another open tab keeps its language until it is reloaded.
19. One function changes the language: `setUiLanguage(lng)` in the i18n module changes the language and persists it. `UiLanguageSelect` calls it now, the Profile menu (#201) later. Startup detection never writes storage. The `languageChanged` listener only sets `<html lang>`.
20. Select states are described in "UI design / States per screen": hover, open, focus ring, and disabled (not used).
21. WebKit adds an internal inline style whenever a native `<select>` is on the page (reproduced with a bare `<select>` and no app CSS). The CSP (`default-src 'self'`, no `style-src`) blocks it, and WebKit logs "Refused to apply a stylesheet because its hash, its nonce, or 'unsafe-inline' appears in neither the style-src directive nor the default-src directive of the Content Security Policy." and "CSP violation: style-src-attr inline". `[phone-safari] web-client/R8-S1` collects console errors and now fails, because the login page (where `/` redirects) has the switch. Decision (b): the CSP stays unchanged (no `style-src-attr 'unsafe-inline'`) and the native select stays. R8-S1 ignores exactly these two WebKit messages, only in WebKit and only when the page has a native `<select>`; any other console error or CSP violation still fails it. Rejected: (a) loosening the CSP, (d) a custom listbox (contradicts R20). The change therefore has four requirements: `web-client` R2, R8, R19 and R20.

22. Manual check on `/admin`: switching the language reloads the teacher list, and an error banner on screen (`TARGET_ACCOUNT_DEACTIVATED`) disappears instead of staying in the old language. Decision (a): keep the code. R20-S7 is amended so that an error already on screen either stays in the old language until the next action, or, on a page that reloads its data when the language changes (the admin teacher list), is cleared by that reload. Rejected: (b) stopping the admin list from reloading on a switch.

## Research angles and conclusions

Angles agreed with the engineer: i18n and detection today (frontend), backend error handling and #92, unit and E2E test impact, copy audit, and the UI plan (`frontend-researcher`).

- i18n today: `frontend/src/i18n/index.ts` runs a top-level `await init({lng:'en', fallbackLng:'en', resources:{en}})`, and `main.tsx` imports it before render. Only `i18next` 26.6.0 and `react-i18next` 17.1.10 are installed, no detector. `index.html` has `<html lang="en">` and nothing sets it. `web-client` R2 says "English is the only UI language for now" and must change. Constitution section 9: UI in en and ru, every UI string through i18n.
- Backend: errors are codes translated in the frontend (`api/client.ts`, falls back to `errors.generic`); nothing depends on Accept-Language. The backend returns 409 `TARGET_ACCOUNT_DEACTIVATED` (`accounts` R8) and `en.json` lacks it, so users see `errors.generic`.
- Unit tests: vitest + jsdom (`navigator.language` is `en-US`); tests import the `'../i18n'` singleton and reset with `changeLanguage('en')` and `localStorage.clear()` in `afterEach`. The detector is a pure function tested with a stubbed `navigator.languages`. `translations.test.tsx` R2-S1 uses its own instance with `lng:'en'`.
- E2E: Playwright 1.64, three projects, no locale set; 119 role, text and label selectors assume English. With the pin, none break.
- Copy audit: among the unit tests only two literals break, `useFieldErrors.test.tsx:103` and `api/client.test.ts:76`; both switch to read `en.<key>`. The audit did not cover the E2E specs and the other `*.test.tsx` files, so task 2.1 greps all of them for the old strings. No spec scenario quotes the rewritten strings. The constitution has no tone rule.
- Risks (design): an unguarded localStorage throwing inside the top-level await would give a blank page; unit tests that do not clear storage become order-dependent; the name clash with `LanguageSelect`; the hidden label is in the current UI language while the option text is the endonym.

## What changes

- `web-client` R2 modified (en and ru resources, per-key fallback, the `TARGET_ACCOUNT_DEACTIVATED` message); R8 modified (scenario R8-S1 ignores two WebKit console messages for a native `<select>`); new R19 (detection, stored choice, `<html lang>`) and R20 (language switch).
- Detection and storage in `frontend/src/i18n/`, `ru.json` with this change's strings, `errors.TARGET_ACCOUNT_DEACTIVATED` in both files.
- `UiLanguageSelect` placed in the header and on the login and set-password pages.
- Neutral rewrite of the informal `en` strings; the design docs' tone rules follow (also `product-brief.md` lines 45 and 60 and `refactoring-ui-patterns.md:22`).
- The placement (header, and inside `<main>` on login and set-password) is temporary: it is not the login band of `design-code.md:354`, until #201, #202 and #205.
- Playwright locale pin and one stored-choice E2E test.

## Out of scope

- The full Russian translation (#103, which also reserves R15-R16).
- The content language of lessons (`lang` on content blocks, #114). #114 owns the second "Done when" bullet of #172; a task notes this on #172 when it is closed.
- Synchronising the language between open tabs.
- Moving the switch into the Profile menu or the login band (#201, #202, #205) and the icon-led select (#197).
- Dark mode, a globe icon, detection libraries, backend changes, new dependencies.
- A switch on the not-found page and on loading or guard pages.
