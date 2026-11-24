# Design code

## Working desk

Teachers and students primarily work on desktop and also use the client on phones. Use a dark app header for identity and session actions, compact aligned rows for scanning, and one column at 360 CSS px. Give dialogs elevation to distinguish them from the page. Use the layout and copy of the current workflow as the starting point for each screen. See the [product brief](product-brief.md).

The change's spec governs its behavior and controls their placement. Follow [project rules](project-rules.md) and use the [four approved profile and admin sketches](../../openspec/changes/design-code-audit/design.md#approved-profile-and-admin-layouts) for the selected layouts.

## Foundations and token plan

These are target values for subsequent frontend changes. Reuse existing tokens with the same role. In the foundations change, list each proposed change to an existing token for engineer approval under C5, and list new role-named tokens in its UI design. Define tokens in `frontend/src/styles/tokens.css` and consume them in shared component CSS.

| Role | Target | Token plan |
| --- | --- | --- |
| Canvas and surface | `#EFF4F8` and `#FFFFFF` | Update `--color-background` and reuse `--color-surface`. |
| Primary and muted text | `#142633` and `#495F6F` | Update `--color-text` and `--color-text-muted`. |
| Primary action and hover | `#174E71` and `#103A56`, with white text | Use `--color-accent` after updating its value, reuse `--color-on-accent`, and add a primary hover role. |
| Header | `#17364B` background, `#EAF4F9` text, `#96B4C4` outline | Add header roles for background, text and outline. Use the light outline for focus on the dark header. |
| Control outline and decorative divider | `#647E90` and `#CAD7DF` | Give controls and decorative dividers separate roles. Audit the consumers of `--color-border` and coordinate the control outline with #168. |
| Active status | `#205B47` text on `#E2F3EB` | Map the active status to the success feedback roles. Review the existing `--color-success-text` and `--color-success-background` and their action aliases before changing them. |
| Waiting for password status | `#66410A` text on `#FFF0D6` | Add waiting status roles. Keep neutral feedback available for other messages. |
| Deactivated status and errors | `#842735` text on `#FCEAEC` | Map these to the danger roles. Review consumers of `--color-danger-text` and `--color-danger-background`, including invalid fields and destructive buttons. |
| Typeface and hierarchy | System stack. Page heading 32/36 px, section heading 20/26 px, body 16/24 px, label and small text 14/20 px. | Reuse `--font-body`, `--text-title` for the page heading, and the body size and line height. Review the current `--text-large` use for narrow page headings and section headings. Give each heading and label its approved line height. |
| Spacing and controls | 4/8/12/16/24/32/48 px scale, 16 px narrow inset, 44 px main controls, 6 px radius | Reuse the 8, 12, 16 and 24 px equivalents from the existing space and button-gap tokens where the roles fit. Reuse the 44 px equivalent of `--control-height` for main controls. Review `--space-4` consumers and the existing `--radius` under C5. The 24 CSS px floor in C9 and C18 applies to touch targets. |
| Header and page measures | 56 px desktop header, 1040 px profile, 1120 px teacher management | Keep `--content-width` for narrow tasks where it fits. Give the header and wider layouts their own roles. Let the header grow when its contents wrap. |
| Focus | 2 px outline with 2 px separation | Use visible focus on each interactive control. Give outline and offset their own roles and check the color against the adjacent surfaces. |
| Elevation | Dialog only, with 12 px vertical offset and 32 px blur at 18% opacity | Apply the dialog shadow to `Dialog` only. Review `--shadow-card` under C5 and audit every consumer, including the header, list rows, `IssuedLink` and `StatusMessage`. |

Check the palette in its implemented combinations, including hover, header controls, focus indicators, field borders and status labels. Keep status text visible alongside color. Preserve accessible state feedback under reduced motion.

## Shared components

Compose pages using `frontend/src/ui/` components. Use native controls, existing i18n messages and sentence case. Keep keyboard focus order aligned with rendered reading order, and leave space for Russian text, long names and email addresses.

| Use | Existing components and anatomy |
| --- | --- |
| Navigation and headings | `AppHeader` contains app name, signed-in name and role, route action and Log out. Use `Page` for the content measure, `Title` for the page heading, `Section` for form headings, and `Toolbar` for the list action. Use `TextLink` for the not-found route home. |
| Form layout | `Form` groups native fields and actions. Keep the label, control, field hint or error and submit action in reading order. Reuse `ButtonRow` for spaced, wrapping actions. |
| Fields and password visibility | Reuse `TextInput`, `Select`, `LanguageSelect` and `PasswordInput`. Keep autofill attributes, error associations and the select arrow text inset. Reserve space for long options. Keep the password visibility control next to its input with a visible, translated Show or Hide label and a gap. |
| Buttons | Reuse `Button` and its action variants, with `PlusIcon` where already used. Keep the text label and native disabled behavior. Leave gaps between buttons and nearby fields. |
| Form, page and retry feedback | `useFieldErrors` associates errors with fields. Preserve the field and form fallback paths, edit-to-clear behavior, and focus rules in `web-client` R14, R17 and R18. Use `StatusMessage` for whole-form feedback and `ErrorWithRetry` where the user can retry. Keep successful results visible as text. |
| Teacher rows and status | Adapt `List` and `ListItem` to aligned, lightly ruled records. Use `StatusBadge` for status text, `ButtonRow` for row actions, and `IssuedLink` for the selectable link, copy feedback and close action. Keep the link with its row when available and preserve the fallback when the row cannot render. |
| Dialogs | Reuse native `Dialog` for add teacher and deactivate confirmation. Keep the title, fields or confirmation text, and spaced actions in order. Preserve initial focus, cancellation, busy behavior and focus return. Fit the dialog and its actions within the 360 px viewport. |

## Screen layouts and states

Use the current routes, actions, validation and i18n copy described in the [auth](../../openspec/specs/web-auth/spec.md), [client](../../openspec/specs/web-client/spec.md), [profile](../../openspec/specs/web-profile/spec.md), and [teacher administration](../../openspec/specs/web-admin-teachers/spec.md) specs. Where route guards check the session, preserve startup loading, retryable startup failure and session-ended handling.

| Screen | Desktop | At 360 CSS px | States and feedback |
| --- | --- | --- | --- |
| Login | Compact single-column form with heading, email, password, submit and existing access-link help. Place reachability or session feedback near the heading and form errors near submit. | Keep the labels, controls, password toggle, feedback, action and help in reading order. | Empty and edited form, native and server field validation, credential and account errors, general failure, backend unreachable, session ended, submitting and successful navigation. |
| Set password | Compact form with heading, new password and repeat. Keep link and form errors near the task. Preserve the missing-link message where the form is unavailable. | Keep password fields, toggles, hints and submit in a single column. | Empty and edited form, length and mismatch hints, field or form rejection, missing or invalid link, submitting and successful navigation. |
| Teacher and student cabinets | Shared dark header with name and role, Profile and Log out. Keep the existing role heading and cabinet content. | Wrap the header and keep both actions reachable. | Both role variants, startup and logout states, with failure text near the cabinet heading. |
| Profile | Put the heading and read-only account email above a 1040 px content area. Align the personal details and password section headings in 55:45 columns, with a submit action in each form. | Stack personal details above password changes. Each action follows its own form and long text wraps. | All three roles, with native language for teachers. Keep values, edits, language loading or retry, pending and disabled submits, field errors, form failure and success, and logout feedback. |
| Teacher management | Use a 1120 px content area. Place Add teacher beside the title. Align identity, time zone, status and actions in compact rows. Keep the issued-link feedback with its teacher. | Move Add teacher below the title. Keep each record's identity, time zone, status, feedback and actions together. Wrap text and actions without horizontal scrolling. | Loading, empty, retryable list failure and populated states. Include active, waiting and deactivated rows, add and deactivate dialogs, cancel and pending actions, language loading or retry, validation and server errors, add and reactivate results, issued-link copy or manual fallback, and action failures. |
| Not found | Keep the heading, explanation and link back home together in a narrow column. | Preserve the same order and allow text to wrap. | Unknown route and the route home. |

Review the longest labels, names and emails using both UI languages when implementing each screen. Use the approved sketches for the profile and teacher management layouts and the current screen inventory for behavior.
