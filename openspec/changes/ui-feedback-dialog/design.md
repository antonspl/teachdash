# Design

## Context

The baseline is the #245 head `d598859`, which supplies the token set, field errors and busy buttons. `StatusMessage` currently renders every tone as a status. `Dialog` already opens a native modal and restores its opener, and its first-focus fallback only runs if the browser did not focus within the dialog. The admin list and issued-link box already have the agreed loading, empty, clipboard and placement behaviors.

## Goals / Non-Goals

Apply the shared feedback and overlay rules to the existing screens and cover the resulting semantics and keyboard transitions. The UI design retains the current page layouts and account operations. The next admin table change owns the link-row placement.

## Decisions

1. **Use one component for the banner roles and icon treatment.** Derive the decorative shared icon and role from the existing tone in `StatusMessage`. A component class or wrapper can maintain the current feedback spacing in Page, Form and retry groups when a banner changes roles. Keep the retry button outside the message element. A shared mapping keeps form errors, reachability, row errors, logout and session startup consistent. Field descriptions still use `FieldError` and their R30 associations.
2. **Keep authorization checks and list transitions where they are.** Render the existing body text primitive inside the startup page while the current-user request is pending. Admin's list loading and empty branches already use that treatment. Give the teacher badge component an icon from the shared set and let the existing badge group wrap independently of each individual badge.
3. **Give the native dialog an explicit initial target.** The owners can name the Name field and the safe Cancel action, so the new header button cannot take initial focus by DOM position. Use a native header button with the shared Close icon and an accessible name. It follows the same idle dismissal path as footer Cancel. Keep the existing separate handling for native close and for a busy modal that needs to reopen. The create submit and deactivate Confirm retain #245 busy labels and their current data and error paths. Keep return focus tied to the recorded opener rather than the browser's click focus, and retain the replacement action behavior after deactivation.
4. **Associate focus return with the link that is shown.** The admin page owns the issuing action and knows the fallback. Record the issuing control from the successful create or row action for the current link, including when a new link replaces one already shown. On dismissal use it if still present and enabled, otherwise use Add teacher. The link's existing copy status and remembered internal focus survive a move from above the list into the teacher card.
5. **Align source guidance where it describes changed behavior.** In `.agents/design/design-code.md` under Button roles, include the shared Dialog header Close icon exception. In `.agents/design/accessibility.md` under Live Regions and `.agents/design/accessibility-patterns.md` under Live regions and notifications, describe the error alert and success/neutral status behavior of `StatusMessage`.

## Risks / Trade-offs

- A role-only migration can remove vertical spacing or leave old absence tests green when an alert appears. Update spacing with the component change and scope the absence assertions to the corresponding form, checking both roles.
- A header close button can take focus after `showModal()` or after a native close during a request. Check initial targets and busy native-close recovery in actual browser tests.
- A reload can replace the issuing element or move the link box. Exercise the current issuer, the Add teacher fallback and the existing link remount path independently.

## Migration Plan

The frontend ships through the existing deployment. This change adds no dependencies, API changes or Flyway migrations.

## UI design

### Screens touched

- Admin teachers list and toolbar, IssuedLink, create teacher dialog and deactivate confirmation.
- Login, set-password, profile details, profile password, role cabinets and guarded startup.

### Layout (desktop and 360 px)

Keep the current page columns and list structure. Give the dialogs a dedicated header row and a footer whose actions stay right-aligned on desktop and stack in the existing order at 360 px. On narrow screens, allow complete badges to wrap as a group and give long messages their own text column beside the icon.

```text
Desktop dialog                          360 px dialog
┌───────────────────────────────┐       ┌───────────────────────┐
│ Title                       × │       │ Title with a long   × │
│                               │       │ name                  │
│ Fields or confirmation text  │       │ Fields or confirmation│
│                               │       │ text                  │
│           [Cancel] [Action]   │       │ [Cancel]              │
└───────────────────────────────┘       │ [Action]              │
                                        └───────────────────────┘
```

### States per screen (empty, error, loading, disabled, success)

| Screen | States |
| --- | --- |
| Admin list | Empty copy and available Add teacher. Plain loading text before the first list. List error with separate retry and row action error in the list feedback area. Populated cards show Active, Waiting for password, Deactivated and Just added. Row eligibility and busy labels from #245 continue to apply. |
| IssuedLink | Idle link, success after Copy, neutral manual-copy state with selection, and standalone link above the list when reloading fails or the teacher is still absent. |
| Create dialog | Initial Name focus, language-load failure with retry, form-level alert or field error on failure, submitted state with dismissal disabled, success returning to the list and showing the new link. After an ordinary failure, the dialog retains its values and normal keyboard flow. |
| Deactivate dialog | Initial Cancel focus and submitted state with dismissal disabled. On failure the dialog closes, the list shows the row action error, and focus returns to the available opener. Success can hand focus to the replacement Reactivate action. |
| Login and set-password | Login reachability alert, neutral session-ended notice, localized form errors and existing busy submit. An incomplete set-password link shows an alert below the title. |
| Profile and cabinets | Profile form successes and failures next to the relevant form, language retry, existing busy submit and logout errors under the titles of Profile and role cabinets. |
| Guarded startup | Plain localized loading text while the request is pending on a role cabinet, Profile, Login or the root redirect. Localized alert and separate retry on failure. The requested route or its existing redirect handles success. |

### Copy (i18n keys and text)

- Add `ui.closeDialog`: “Close dialog” / «Закрыть диалог». The glyph has no text of its own.
- Reuse `ui.loading`: “Loading…” / «Загрузка…», and `ui.tryAgain`: “Try again” / «Повторить попытку».
- Keep the existing `adminTeachers.create.*`, `adminTeachers.deactivate.*`, `adminTeachers.empty`, `adminTeachers.status.*` and `adminTeachers.justAdded` labels. `adminTeachers.deactivate.cancel` is “Cancel” / «Отмена» and `adminTeachers.deactivate.confirm` is “Confirm” / «Подтвердить».
- Keep `issuedLink.copy` (“Copy” / «Скопировать»), `issuedLink.close` (“Close” / «Закрыть»), and the existing localized label, note, copied confirmation and manual-copy prompt. Login, profile, startup and the other error surfaces reuse their current translations.

### A11y (focus order, keyboard, aria-*)

The icon-only header button has the localized `aria-label`. Open create on Name and the confirmation on Cancel. Close, Cancel and Escape return to the actual opener while idle. While submitting or confirming, both dismissal buttons are disabled and Escape cannot abandon the visible request. If the browser closes the active modal anyway, recover it. After an ordinary create failure, keep focus within the dialog so tabbing can resume. After a failed deactivation, return focus to the available Deactivate opener. When a successful deactivation replaces the opener, focus Reactivate if focus was lost or the opener still had focus. Respect any intervening move to another available control.

Error banners are alerts, success and neutral banners are statuses, and the decorative icons are hidden from assistive technology. Retry stays outside the alert. Keep field descriptions associated with their controls and the focus order for field errors from #245. On failed clipboard access, focus and select the URL. On Close, return focus to the action that issued the currently visible link when available, with Add teacher as the fallback.

### Components and tokens (reused or new)

Reuse `StatusBadge`, `StatusMessage`, `Text`, `ErrorWithRetry`, native `Dialog`, `IssuedLink`, the shared icons and the existing button roles. Keep the existing color pairs, global focus ring, control sizes and spacing scale. The modal uses `--dialog-width`, `--shadow-overlay` and `--color-backdrop`. The complete token use by surface is below. No new tokens.

### Visual plan

**Shared components**

- Use the current token set. Badge status is secondary to the teacher’s name. Use `--text-small`, `--line-height-small`, `--weight-strong`, `--icon-size-small`, the matching tone pair and `--radius`. Use `--space-adjacent` by `--space-inline` padding. Keep each icon and label together. Allow the badge group to wrap beneath long content at 360 px.
- Use a flat banner with `--space-group` padding, `--space-inline` between `--icon-size` and message, `--radius`, and the tone’s text and background. Align the decorative icon to the first line so long English and Russian messages wrap below their text. The message leads, and a retry action sits separately with space around it. Error banners are alerts. Success and neutral banners are statuses.
- Use the global focus ring, native controls and tokenized disabled feedback. Keep the current field error and busy button treatments. No new token is needed.

**Admin teachers list and issued link**

- Keep the existing admin toolbar and teacher list. In the empty state, Add teacher is the primary action and `adminTeachers.empty` is its explanatory line. During loading, `ui.loading` is plain body text in the existing content area. Keep the page width, header and toolbar steady.
- In populated states the name and row actions lead, with badge, email and time zone supporting. Show Active, Waiting for password, Deactivated and Just added in both viewports. At 360 px the existing card layout and the wrapping badge group accommodate long names and Russian status phrases.
- Keep IssuedLink in its teacher’s card, with the standalone box above the list when the teacher is absent. Its label and selectable URL are primary, its note is muted, and Copy leads Close. Use surface, divider, radius, sunken field and control border tokens. The URL stays in a read-only text input. Fit the field and actions within both viewports, with action wrapping as needed. Show success and manual-copy banners below the controls.
- Capture populated and empty lists, held loading, list error with Retry, row action error, issued link idle, copied, manual copy and standalone link with a reload error. Seed local accounts for populated states. Use a controlled empty list response for a repeatable empty state, held responses for transient states, and controlled API/clipboard failures for error paths.

**Create and deactivate dialogs**

- The native overlay is `min(var(--dialog-width), 100% - 2 * var(--space-group))` wide, with `--space-block` padding, surface, radius, overlay shadow and backdrop tokens. The dialog has no border. Keep a scrollable interior when content exceeds the available height.
- The h2 uses `--text-large`, `--line-height-large` and strong weight. Place the native close button in its own header position, pulled `--space-inline` into the right padding. Use the shared Close icon at `--icon-size` and a `--control-height` target. Let long titles wrap clear of the close target.
- On desktop, right-align the footer buttons with `--space-button-gap`. On the design-code narrow layout, stack them in DOM order. The create fields lead to Create teacher, with Cancel as the secondary action. The confirmation names the teacher and consequence, then offers the safe choice before the danger action.
- Capture create with first field focused, ordinary form-level error, language error with Retry, and a held submission. Capture confirmation with its safe button focused and while confirming. Test header Close, Escape, footer Cancel and focus return separately from screenshots.

**Login, set-password, Profile, cabinets and startup**

- Keep each existing body column and message location. In login, show the danger reachability warning and neutral session-ended notice below the title; the sign-in form remains the primary action. Form errors in login and set-password sit with the affected form. Set-password with an incomplete link has an error banner as its main content beneath the title.
- Profile form and password form messages stay beside the form they describe. Capture each form’s error and success plus the language retry state. The applicable form’s action remains primary. Logout errors on Profile and role cabinets stay beneath the page title without hiding the existing navigation or actions.
- During guarded startup, show plain “Loading…” / «Загрузка…» in the page body. Startup failure shows the error banner and a separate Try again button. No spinner, skeleton or substitute heading. Use held and failed session responses and the existing localized fixture paths.
- Capture before and after at desktop and 360 CSS px on the same local states, with Russian at 360. Reuse the route patterns in `frontend/e2e/russianFit.spec.ts` and `frontend/e2e/hold.ts`. Record unavailable states by name.
