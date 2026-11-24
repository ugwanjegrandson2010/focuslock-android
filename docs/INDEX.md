# Focus Lock — Plan Document Index

All planning artifacts for the Android focus-blocking application live in this directory.

## Reading order

| # | Document | Audience | Purpose |
|---|---|---|---|
| 1 | `prompt.md` | Implementation agent | Self-contained build brief: copy this into the agent that will write code. |
| 2 | `spec.md` | Implementer, tester | Requirements, acceptance criteria, data model, state machine, API map. |
| 3 | `prerequisite.md` | Implementer, release engineer | Toolchain, permissions, device-owner provisioning, Phase 0 checklist. |
| 4 | `architecture.md` | Implementer | Module graph, components, sequences, threading, ADRs. |
| 5 | `implementation-roadmap.md` | Implementer, project owner | Phases P0–P7, milestones, dependencies, effort, definition of done. |
| 6 | `ui-ux-plan.md` | UI implementer | Screens, navigation, design system, copy and honesty guidelines. |
| 7 | `test-plan.md` | Tester | Test levels, environment matrix, manual cases TC-01..TC-23, exit criteria. |
| 8 | `security-privacy-plan.md` | Implementer, reviewer | Data classification, threat model, PIN handling, validation, privacy notice. |
| 9 | `risk-register.md` | Project owner | Risks, severity, mitigations, go/no-go criteria. |
| 10 | `release-operations-plan.md` | Release engineer | Versioning, signing, distribution, provisioning runbook, support, release checklist. |

## Quick map

- **What to build:** prompt.md → spec.md → ui-ux-plan.md
- **How to build:** prerequisite.md → architecture.md → implementation-roadmap.md
- **How to verify:** test-plan.md → risk-register.md
- **How to ship:** security-privacy-plan.md → release-operations-plan.md

## Status

Plan phase complete. No application source code has been created — implementation requires switching out of Plan mode to a build agent.
