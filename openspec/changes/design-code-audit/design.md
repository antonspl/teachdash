# Design

## Context

The audit covers the current login, set-password, role cabinet, profile, teacher management, and not-found screens. The route inventory comes from `frontend/src/App.tsx`, and `web-auth`, `web-profile`, `web-admin-teachers`, and `web-client` define their behavior. This cross-screen plan gives future OpenSpec changes a common layout and component reference grounded in the desktop-first workflows described in the [product brief](../../../.agents/design/product-brief.md).

## Goals

Make repeat desktop tasks quick to scan and keep keyboard and touch use practical at 360 px. Give each subsequent frontend issue a clear starting point for tokens, components, layout, and states. Follow C1–C24 in `.agents/design/project-rules.md`.

## Decisions

### Working desk foundations

The engineer chose the compact direction after comparing it with a roomier single-column option. Use a dark, 56 px desktop header for app identity and session actions, with a flexible wrapped header on narrow screens. Use lightly ruled rows and aligned columns for scanning. Give dialogs the only elevation so they read as a separate layer over the page.

| Role | Proposed value |
| --- | --- |
| Canvas and surface | `#EFF4F8` and `#FFFFFF` |
| Primary and muted text | `#142633` and `#495F6F` |
| Primary action and hover | `#174E71` and `#103A56`, with white text |
| Header, on-header text and outline | `#17364B`, `#EAF4F9`, and `#96B4C4` |
| Control outline and decorative divider | `#647E90` and `#CAD7DF` |
| Active status text and fill | `#205B47` and `#E2F3EB` |
| Waiting for password status text and fill | `#66410A` and `#FFF0D6` |
| Deactivated and error text and fill | `#842735` and `#FCEAEC` |
| Typography | Existing system font stack. Page heading 32/36, section heading 20/26, body 16/24, label and small text 14/20. Sizes and line heights are in px. |
| Spacing and controls | 4/8/12/16/24/32/48 px scale, 16 px mobile inset, 44 px main controls, 6 px radius |
| Focus and elevation | 2 px focus outline with 2 px separation, light on the dark header. Dialog elevation only, using a 12 px offset and 32 px blur at 18% opacity. |

Map roles to existing tokens where the role already matches. The foundations follow-up will list existing tokens to replace and role-named tokens to add, including separate control and decorative borders. Record changes under C5 for the engineer to approve in that change. Verify contrast in the implemented palette, including controls, on-header focus and state labels. Use text alongside status colors.

### Layout and components

| Screen | Desktop | At 360 px |
| --- | --- | --- |
| Login and set password | One compact form with a clear heading and supporting text. Keep field errors beside their fields and form-level or link errors near the task. | Keep labels, fields, password visibility controls, errors and actions in reading order in one column. |
| Teacher and student cabinets | Shared header with identity, profile and logout. Keep the user's role and current cabinet content. | Wrap the header and keep its actions reachable. |
| Profile | Put heading and account email above a 1040 px area with 55:45 form columns. Align form headings and keep a submit action for each form. | Put personal details above password changes, with actions following each form and long text wrapping. |
| Teacher management | Use a 1120 px area. Place Add teacher beside the title and align identity, time zone, status and available actions in compact rows. Keep issued-link feedback with its row. | Move the add action below the title. Keep identity, time zone, status, feedback and actions grouped in each record and let actions wrap. |
| Not found | Present the heading, short explanation and route back home together. | Maintain the same order in one column. |

Inventory shared navigation and page headings, forms and field wrappers, native inputs and selects, password visibility, buttons, inline errors and form feedback, status labels, list rows, and dialogs. In the design code, map them to the existing shared components and use cases. Give layouts the same focus order as the rendered reading order. Record which existing states apply to each screen and keep current i18n copy and behavior as the reference, including loading, disabled, validation, server failure, success, and dialog states where supported. Test long names and email addresses in row layouts and plan space for English and Russian text.

### Approved profile and admin layouts

Keep these four sketches with the change and link to this section from `.agents/design/design-code.md`. When archiving, update that link to the archived path and check that it opens in a clean checkout. The locally generated previews supplement these sketches.

**Profile desktop · 1040 px content**

```text
TeachDash                           Anna (Teacher) [Back to cabinet] [Log out]
Your profile
Your email: anna.petrova@example.com
+ About you (55%) ---------------------+    + Password (45%) ----------------+
| Name                                 |    | Current password        [Show] |
| Time zone                            |    | New password            [Show] |
| Native language (teacher only)       |    | Repeat the new password [Show] |
| [Save]                               |    | [Change password]              |
+--------------------------------------+    +--------------------------------+
```

**Profile at 360 px · 16 px inset**

```text
TeachDash
Anna (Teacher)
[Back to cabinet] [Log out]

Your profile
Your email: anna.petrova@example.com
+ About you --------------------------+
| Name                                |
| Time zone                           |
| Native language (teacher only)      |
| [Save]                              |
+-------------------------------------+
+ Password ---------------------------+
| Current password             [Show] |
| New password                 [Show] |
| Repeat the new password      [Show] |
| [Change password]                   |
+-------------------------------------+
```

**Admin desktop · 1120 px content**

```text
TeachDash                                  Alena (Admin) [Profile] [Log out]
Admin cabinet                                                  [Add teacher]
+----------------------------------------------------------------------------+
| Anna / email  | Europe/Moscow | Active               | [Reset] [Deactivate]|
| David / email | Europe/Berlin | Waiting for password | [Invite] [Deactivate]|
| Maria / email | Asia/Tbilisi  | Deactivated          | [Reactivate]         |
+----------------------------------------------------------------------------+
```

**Admin at 360 px · 16 px inset**

```text
TeachDash
Alena (Admin)
[Profile] [Log out]

Admin cabinet
[Add teacher]
+-------------------------------------+
| Anna                                |
| anna.petrova@example.com            |
| Europe/Moscow                       |
| Active                              |
| [Password reset link] [Deactivate]  |
| ----------------------------------- |
| David                               |
| david.morozov.lessons@example.com     |
| Europe/Berlin                       |
| Waiting for password                |
| [New invite link] [Deactivate]       |
+-------------------------------------+
```

The desktop sketch abbreviates two action labels to fit, and the mobile sketch shows the first two records. Use the existing “Password reset link” and “New invite link” labels at both widths.

### Evidence and rollout

The local baseline gallery starts with 30 paired captures at 1280 px and 360 px. It includes both cabinet roles and labels controlled network conditions in the captions. The PR #189 gallery supplements the local captures and reports 35 paired states. Reconcile its coverage and reported gaps with the local set. [PR #189 baseline gallery](https://github.com/antonspl/teachdash/pull/189#issuecomment-7082114274)

Inventory each screen against its applicable states and role variants using the current UI and the `web-auth`, `web-profile`, `web-admin-teachers` and `web-client` specs. Check startup loading and retryable startup errors wherever the route shows them, field and form feedback, disabled and success states, list loading and empty states, and admin dialog failures including server errors and native-language loading, failure and retry. Capture missing reachable states at both widths. Record a reason for any remaining gap or inapplicable state. Use baseline captures as the reference for current behavior and the sketches as the reference for intended layout.

Publish the coverage inventory and captures through GitHub issue #171. Check that the files and share links are accessible to reviewers and that screenshots contain only test data and masked or unusable invite/reset URLs. Link the resulting evidence from follow-up issues by screen and state. Order the issues as foundations, shared UI components, then pages. Size every follow-up for one OpenSpec change with at most six requirements. Reuse #168 for control borders, #165 for shared required fields, and #172 for page and content language. Cross-link dependent work to the issue that owns it and keep ownership of each requirement in one issue. Check that the ordered backlog covers all current screen families.

## Risks and trade-offs

- Compact columns improve scanning and can crowd long content. Keep stable columns at desktop widths and let content wrap within a record on phones.
- The prototype used representative synthetic content. Check real layout constraints and focus behavior when each follow-up is implemented.
- Repository-local galleries are ignored by Git. Keep the public review trail on #171 and verify its links before closing the audit.

## Migration Plan

The change publishes design documentation and evidence. It requires zero new runtime dependencies and zero Flyway migrations.
