# Tasks

## 1. Design code

- [ ] 1.1 Fill `.agents/design/design-code.md` with the Working desk foundations and token plan, shared component inventory and states, and layout guidance for all six screen families at desktop width and 360 px. Link to the four tracked layout sketches. In a clean checkout, check the link, role colors, typography, component anatomy and profile and admin layouts against the tracked design and C1–C24. Record the check in the PR.

## 2. Audit evidence and follow-up issues

- [ ] 2.1 Compare the local captures and the PR #189 gallery linked in `design.md` against the screen and applicable state inventory from the current specs and UI. Capture missing reachable states at 1280 px and 360 px, and document any remaining gaps and inapplicable states with reasons. Publish the inventory and paired captures through per-screen comments on #171, using meaningful alt text. Check that reviewers can open all pairs, that the inventory accounts for all six screen families and applicable states, and that images use synthetic data with invite and reset values masked or invalidated. Record the comment links in the PR.
- [ ] 2.2 File or update the linked frontend issues in dependency order from foundations to shared UI to pages. Keep each issue within one change and six requirements. Reuse #165, #168 and #172 for their existing scope and cross-link related issues to the owner of each requirement. Verify coverage against the design code and six-screen inventory. Record scope, dependency and baseline links in the issues and summarize them in the PR.

## Workflow follow-up

- Start these tasks after the engineer approves the spec gate.
- Run the code-gate review on the documentation, evidence and issue links. Check that the product brief follows the template order and incorporates all five of Anton's review clarifications. Check that visual choices are documented in the design code. Request engineer approval with the implementation commit range and the proof recorded in the PR.
- Archive the approved change in the same PR. Update the design code link to the archived sketches and check it in a clean checkout. Mark the PR ready for final code owner review.
