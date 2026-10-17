---
type: Agent Workflow
title: Triage Labels
description: When a skill says apply a triage label, or an issue's triage state is read — the five canonical roles mapped to this repo's label strings.
status: stable
---

# Triage Labels

The skills speak in terms of five canonical triage roles. This file maps those roles to the actual label strings used in this repo's issue tracker.

| Label in mattpocock/skills | Label in our tracker | Meaning                                  |
| -------------------------- | -------------------- | ---------------------------------------- |
| `needs-triage`             | `needs-triage`       | Maintainer needs to evaluate this issue  |
| `needs-info`               | `needs-info`         | Waiting on reporter for more information |
| `ready-for-agent`          | `ready-for-agent`    | Fully specified, ready for an AFK agent  |
| `ready-for-human`          | `ready-for-human`    | Requires human implementation            |
| `wontfix`                  | `wontfix`            | Will not be actioned                     |

When a skill mentions a role (e.g. "apply the AFK-ready triage label"), use the corresponding label string from this table.

The label is written in the `Status:` line near the top of the issue file; the directory layout and that line's contract live in [issue-tracker.md](issue-tracker.md).

## Done when

- The label you applied is one of the five strings above, written as the issue's `Status:` value.
