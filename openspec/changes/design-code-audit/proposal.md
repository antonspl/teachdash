## Clarifications

1. **What product brief guides the audit?** Use the product brief from PR #189 with the five corrections below. [Revised product brief](../../../.agents/design/product-brief.md) · [Source PR #189](https://github.com/antonspl/teachdash/pull/189)
   - Tutoring spans any subject. Every course has an instruction language, initially `en` or `ru`, and may have a target language (§9). [Anton's review comment](https://github.com/antonspl/teachdash/pull/189#discussion_r4659260401)
   - Planned sharing lets teachers send a course or lesson link to another teacher, who can preview it read-only and make an independent copy (§8). [Anton's review comment](https://github.com/antonspl/teachdash/pull/189#discussion_r4659265790)
   - The planned teacher cabinet schedule (#14) is a separate task. Class details include the student, a comment, rescheduling or cancellation status, and links to the meeting and whiteboard. [Anton's review comment](https://github.com/antonspl/teachdash/pull/189#discussion_r4659270344)
   - Teachers and students are desktop-first. The interface remains usable at 360 CSS px (`specs/web-client` R7). [Anton's review comment](https://github.com/antonspl/teachdash/pull/189#discussion_r4659274680)
   - The domain governs data storage and protects access. The frontend presents data the user is allowed to see. [Anton's review comment](https://github.com/antonspl/teachdash/pull/189#discussion_r4659295751)
2. **Which design direction should future frontend changes use?** The engineer chose Working desk after comparing two directions on the same profile and admin screens. It uses a dark blue header, compact aligned lists, and two profile forms side by side on desktop. At 360 px, forms and list content stack in reading order.
3. **Where does the audit end?** Issue #171 covers the brief, design guidance, the screenshot baseline, and an ordered frontend backlog. Product UI, behavior, workflow configuration and the design reference sources are outside this change.

## Why

The current interface grew screen by screen. Issue #171 establishes a shared design direction and a reference for comparing the redesign with what users can reach today.

## Research

- **Current screen inventory and baseline.** The audit mapped six screen families, including both role cabinets, and produced an initial local set of 30 paired captures at 1280 px and 360 px. PR #189 provides supplemental evidence and reports 35 paired states. The screen and state inventory will reconcile the sets, determine the published coverage, and document any remaining gaps. [PR #189 baseline gallery](https://github.com/antonspl/teachdash/pull/189#issuecomment-7082114274)
- **Visual direction.** Two concepts used the same profile and teacher management content. Working desk made desktop rows easier to scan while retaining a usable single column on a phone.

## What Changes

- Update `.agents/design/product-brief.md` with the text from PR #189 and the five review corrections.
- Complete `.agents/design/design-code.md` with the selected layout, token plan, shared component inventory, screen patterns and durable references to the four layout sketches.
- Audit baseline coverage against the current specs and UI, capture missing reachable states, and publish the evidence and any remaining gaps to issue #171.
- Sequence follow-up frontend work in changes of up to six requirements. Keep existing work with #165, #168 and #172 and cross-link dependent work.

## Capabilities

### New Capabilities

### Modified Capabilities

## Impact

The product brief and design code guide future frontend changes. The baseline and linked issues provide evidence for their design and review. This documentation-only change uses `skip_specs: true` under constitution §1.
