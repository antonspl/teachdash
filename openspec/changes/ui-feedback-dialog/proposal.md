# Proposal

## Clarifications

The engineer confirmed the following for [#198](https://github.com/antonspl/teachdash/issues/198).

1. **How does the dialog open and dismiss?** Use a native modal with a header close icon, named `ui.closeDialog` (“Close dialog” / «Закрыть диалог»). It dismisses like Cancel and is disabled during the relevant request. Put initial focus on Name for create and on the safe action for deactivation. Keep opener restoration and recovery from native close. Keep the current Cancel and Confirm copy for #198. The Dialog example in the design code shows different copy scheduled for #203.
2. **How are badges and banners presented?** Use flat, icon-led banners, with check for success, info for neutral and alert for errors. The decorative icon aligns with the first line. Errors use `role="alert"`; neutral and success use `role="status"`. A badge keeps its icon and label together and the group can wrap. Use success/right for Active, neutral/clock for Waiting for password, neutral/ban for Deactivated and neutral/check for Just added. Keep the current placement through #198. The relocation belongs to #203.
3. **How does the issued link behave?** Keep it in its teacher's card or above the list when the teacher is absent. Copy is primary and Close is secondary. When clipboard access fails, the neutral manual-copy prompt appears and the URL field receives focus with the full text selected. After Close, focus returns to the action that issued the link when it is still available, or to Add teacher.
4. **How do page and form states fit the new components?** Keep the admin loading and empty states, use plain localized loading text while checking the session, and keep retries separate from live banner messages. Keep existing field-level feedback and request behavior from #245. Use existing components and tokens.
5. **How is the visual change reviewed?** Compare the same states before and after at desktop and 360 CSS pixels, using Russian at 360. The before set uses `d598859` from [PR #245](https://github.com/antonspl/teachdash/pull/245). The UI design lists the screens and states.

## Research

The agreed frontend research angle was to compare badges, banners, empty and loading states, Dialog and IssuedLink with the design code and the #245 baseline, including accessibility and affected scenarios. The existing components and tokens cover the change. The shared message component already owns the error surfaces, but it gives every tone a status role and some spacing selectors rely on that role. Dialog's native initial focus needs an explicit target when the header button is added. The current tests for button icons and divider borders require two spec exceptions. IssuedLink can retain its clipboard and remount behavior while adding focus return on dismissal.

## Why

The existing shared feedback and admin dialogs predate the visual direction in #171. Restyling them together makes their states consistent and gives form and page errors the correct announcement role.

## What Changes

- Restyle teacher status badges and shared banners, including form, reachability, retry and session feedback, with localized statuses and decorative icons.
- Show plain localized body text during guarded startup and retain the admin list's empty, loading and recovery states.
- Restyle the create and deactivate dialogs and add a localized close control with explicit focus behavior.
- Retain the issued-link layout and clipboard feedback, and place focus on an available action after dismissal.

## Capabilities

### Modified Capabilities

- `web-client`: shared feedback roles, guarded loading text, modal close and focus behavior, and the dialog's exceptions to button icon and divider rules.
- `web-admin-teachers`: teacher badges and list states, and the issued link's feedback and dismissal focus.

## Impact

- React UI components, admin teacher actions, auth guards, form feedback consumers, English and Russian translations, and frontend scenario tests.
- The PR is based on the #245 branch. Reachability, field-error and profile scenarios keep their existing message guarantees and their tests check the roles defined by the shared feedback requirement.

## Out of Scope

- Tooltip and IconButton (#199), the admin table and link-row or badge relocation (#203), and the deactivation button copy update (#203).
- Backend endpoints, data ownership, migrations, dependencies, and changes to clipboard, field-error or request semantics beyond the confirmed feedback and focus behavior.
