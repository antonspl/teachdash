# Spec Delta

## MODIFIED Requirements

### Requirement: R23 Icons on buttons

The plus of "Add teacher" SHALL use the set's plus (`M10 4v12M4 10h12`), SHALL be the first
child of the button before the label, and SHALL be `aria-hidden="true"` and
`focusable="false"`. Icons SHALL NOT appear on primary, form or dialog buttons, except a
create action's plus and the dialog header Close icon. "Create teacher" (the dialog's submit) SHALL have no icon.

#### Scenario: web-client/R23-S1 The plus of Add teacher uses the set's plus
- **GIVEN** a logged-in admin on `/admin`
- **WHEN** the "Add teacher" button is read by role and name
- **THEN** its first child is an `svg` that is `aria-hidden="true"` and `focusable="false"`, followed by the label
- **AND** its path is `M10 4v12M4 10h12`

#### Scenario: web-client/R23-S2 Create teacher and the other dialog buttons have no icon
- **GIVEN** a logged-in admin with the "Add teacher" or deactivation dialog open, one per run
- **WHEN** its buttons are read by role and name
- **THEN** the header's "Close dialog" button contains the shared close icon, with `aria-hidden="true"` and `focusable="false"`
- **AND** Create teacher, Cancel and Confirm, where present, have no icon

### Requirement: R26 Control borders have 3:1 contrast

`--color-control-border` SHALL have a contrast of at least 3:1 against every background it
can sit on, and the text pairs of the design code SHALL meet their ratios. Edges a person
operates SHALL use the control border; decorative edges SHALL use `--color-divider`, below
3:1 by design (scenarios).

#### Scenario: web-client/R26-S1 The control border passes 3:1 on every background
- **GIVEN** the colour values of `tokens.css`, read as text
- **WHEN** the contrast ratio of `--color-control-border` against each of `--color-surface`, `--color-background`, `--color-surface-sunken`, `--color-rail`, `--color-accent-subtle`, `--color-neutral-background`, `--color-success-background` and `--color-danger-background` is computed
- **THEN** every ratio is at least 3

#### Scenario: web-client/R26-S2 Control edges and decorative edges use their own tokens
- **GIVEN** the CSS of Field, Button, IssuedLink, List, Dialog and AppHeader, read as text
- **WHEN** their border rules are read
- **THEN** the text input, the select, the password toggle, the secondary button and the issued-link input use `--color-control-border`
- **AND** the List item and the IssuedLink box use `--color-divider`, the Dialog has no border, and the AppHeader has a bottom border of `--border-width` solid `--color-divider`
- **AND** the field error border uses `--border-width-strong` and `--color-danger-text` (web-client R30)

#### Scenario: web-client/R26-S3 Text pairs meet their ratios
- **GIVEN** the colour values of `tokens.css`, read as text, and the contrast helper of R26-S1
- **WHEN** the ratios of the pairs listed in the design-code "Colour" section are computed
- **THEN** `--color-text` and `--color-text-muted` on `--color-surface`, `--color-background`, `--color-surface-sunken`, `--color-rail` and `--color-accent-subtle` are at least 4.5
- **AND** `--color-accent` (link text) on `--color-surface` and on `--color-rail`, `--color-on-accent` on `--color-accent` and on `--color-danger-action`, and the neutral, success and danger text on their own backgrounds are at least 4.5

## ADDED Requirements

### Requirement: R31 Shared page and form feedback

The web client SHALL show each page or form banner with localized text and a decorative tone icon. Error banners SHALL use `role="alert"`; neutral and success banners SHALL use `role="status"`. An offered retry SHALL be a separate action. Field-level descriptions SHALL retain the behavior in R17 and R30. Guarded startup SHALL show plain localized loading text in the page body while the session is being checked.

#### Scenario: web-client/R31-S1 Banner semantics follow the tone
- **GIVEN** a banner with a success, neutral or error tone
- **WHEN** the banner is rendered
- **THEN** the localized text remains available, with the shared check, info or alert icon respectively hidden from assistive technology
- **AND** only the error banner has `role="alert"`, and the other two have `role="status"`

#### Scenario: web-client/R31-S2 A page error is announced in its page
- **GIVEN** separate runs with login reachability failure, an incomplete set-password link, an admin row action failure, and logout failure on Profile or a role cabinet
- **WHEN** the page detects the failure
- **THEN** the existing localized message is an alert below the page heading or, for an admin row action, in the list's feedback area
- **AND** a login notice that the session ended is a neutral status when shown

#### Scenario: web-client/R31-S3 Form-level errors belong to the affected form
- **GIVEN** separate runs with a request failure requiring a form-level message in login, set-password, Add teacher, the profile details form or the profile password form
- **WHEN** that form is submitted and the request fails
- **THEN** the affected form shows its existing localized error as an alert and the other form on Profile does not receive that alert
- **AND** when a request yields only accepted field errors, the affected form shows no form-level alert or status for that submission, and the focus and descriptions follow web-client R17 and R30

#### Scenario: web-client/R31-S4 Retry is outside the alert
- **GIVEN** separate runs where guarded startup, the admin list, or native-language choices in Add teacher or Profile fail to load
- **WHEN** the failure is shown
- **THEN** the localized error is an alert in the affected area and the Try again button is available outside the alert
- **AND** activating Try again repeats the failed operation and removes the error after a successful response

#### Scenario: web-client/R31-S5 Startup loading is plain text
- **GIVEN** a held current-user request on a guarded route, with one run each in English and Russian
- **WHEN** the route starts
- **THEN** the main page body shows `ui.loading` as “Loading…” or «Загрузка…» in plain text without a banner, spinner or skeleton

#### Scenario: web-client/R31-S6 Successful changes announce their own message
- **GIVEN** a logged-in user on Profile
- **WHEN** the details form is saved or the password form succeeds, one run per form
- **THEN** that form shows its existing localized success message as a status

### Requirement: R32 Admin modal dialog dismissal and focus

The create and deactivate dialogs SHALL be native modals with their titles as accessible names and a localized header Close button. Initial focus SHALL go to Name in create and Cancel in deactivate. Close, Cancel and Escape SHALL dismiss an idle dialog and restore focus to its opener. The dialogs SHALL stay open during their submit or confirm request, and focus SHALL follow the replacement Reactivate action after successful deactivation if the opener disappeared.

#### Scenario: web-client/R32-S1 The first field or safe action receives focus
- **GIVEN** a logged-in admin and an available teacher to deactivate
- **WHEN** the admin opens Add teacher and the deactivate confirmation in separate runs
- **THEN** each is a modal named by its visible title
- **AND** Name has focus in Add teacher and Cancel has focus in the confirmation, with Cancel before Confirm in the footer order

#### Scenario: web-client/R32-S2 Idle dismissals return focus to the opener
- **GIVEN** an idle Add teacher or deactivate dialog opened by its page action, with a run for each dismissal method and UI language
- **WHEN** the admin activates the header button named “Close dialog” or «Закрыть диалог», presses footer Cancel, or presses Escape
- **THEN** the dialog closes without sending its action request, and focus returns to the element that opened it

#### Scenario: web-client/R32-S3 A running request keeps the dialog open
- **GIVEN** a held create or deactivate request after the corresponding dialog is submitted
- **WHEN** the admin tries to use either dismissal button or presses Escape, including a browser that still closes the native modal
- **THEN** header Close and footer Cancel are disabled, the action keeps its existing busy treatment, and the dialog stays open or recovers from the native close while the request is running

#### Scenario: web-client/R32-S4 The replacement action receives focus after deactivation
- **GIVEN** a logged-in admin who opened a teacher's deactivate dialog
- **WHEN** confirmation succeeds and the refreshed row replaces Deactivate with Reactivate
- **THEN** focus goes to Reactivate if it was lost with the opener and the admin has not moved focus elsewhere
