## Clarifications

1. **Who is the design for?** Private tutors teach individual students across disciplines. Plan for around 10 active students per tutor on average, often more. Most students are adults. Tutors teach from a computer, and students use a computer or phone to find materials and stay in touch between lessons. Tutor phone use is occasional. The agreed audience and workflows are recorded in [the product brief](../../../.agents/design/product-brief.md).
2. **Which design direction should future frontend changes use?** The engineer chose Working desk after comparing two directions on the same profile and admin screens. It uses a dark blue header, compact aligned lists, and two profile forms side by side on desktop. At 360 px, forms and list content stack in reading order.
3. **Where does the audit end?** Issue #171 covers design guidance, the screenshot baseline, and an ordered frontend backlog. Product UI, behavior, workflow configuration and the design sources are outside this change. Materials, chat and the future student roster inform the brief and add no capabilities to this change.

## Why

The current interface grew screen by screen. Issue #171 establishes a shared design direction and a reference for comparing the redesign with what users can reach today.

## Research

- **Current screen inventory and baseline.** The audit mapped six screen families, including both role cabinets, and produced an initial set of 30 paired captures at 1280 px and 360 px. The screen and state inventory will determine the published set and document any remaining gaps.
- **Visual direction.** Two concepts used the same profile and teacher management content. Working desk made desktop rows easier to scan while retaining a usable single column on a phone. The wider roster described in the product brief informed the comparison.

## What Changes

- Complete `.agents/design/design-code.md` with the selected layout, token plan, shared component inventory, screen patterns and durable references to the four layout sketches.
- Audit baseline coverage against the current specs and UI, capture missing reachable states, and publish the evidence and any remaining gaps to issue #171.
- Sequence follow-up frontend work in changes of up to six requirements. Keep existing work with #165, #168 and #172 and cross-link dependent work.

## Capabilities

### New Capabilities

### Modified Capabilities

## Impact

The product brief and design code guide future frontend changes. The baseline and linked issues provide evidence for their design and review. This documentation-only change uses `skip_specs: true` under constitution §1.
