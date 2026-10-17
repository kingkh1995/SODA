---
type: Agent Workflow
title: "Issue tracker: Local Markdown"
description: When a skill says publish to or fetch from the issue tracker, or a wayfinder map and its child tickets are read or updated — read this file for the directory layout and the open/claimed/resolved status line.
status: stable
---

# Issue tracker: Local Markdown

Issues and specs for this repo live as markdown files in `.scratch/`.

## Conventions

- One feature per directory: `.scratch/<feature-slug>/`
- The spec is `.scratch/<feature-slug>/PRD.md`
- Implementation issues are one file per ticket at `.scratch/<feature-slug>/issues/<NN>-<slug>.md`, numbered from `01` — never a single combined tickets file
- Triage state is recorded as a `Status:` line near the top of each issue file — role strings in [triage-labels.md](triage-labels.md)
- Naming a domain concept in an issue title or spec → [domain.md](domain.md)
- Blocking dependencies recorded as a `Blocked by:` line near the top
- Comments and conversation history append to the bottom of the file under a `## Comments` heading

## When a skill says "publish to the issue tracker"

Create a new file under `.scratch/<feature-slug>/` (creating the directory if needed).

## When a skill says "fetch the relevant ticket"

Read the file at the referenced path. The user will normally pass the path or the issue number directly.

## Wayfinding operations

Used by `/wayfinder`. The **map** is a file with one **child** file per ticket.

- **Map**: `.scratch/<effort>/map.md` — the Notes / Decisions-so-far / Fog body.
- **Child ticket**: `.scratch/<effort>/issues/NN-<slug>.md`, numbered from `01`, with the question in the body. A `Type:` line records the ticket type (`research`/`prototype`/`grilling`/`task`); a `Status:` line records `open` → `claimed` → `resolved` — a new ticket starts `open`.
- **Blocking**: a `Blocked by: NN, NN` line near the top. A ticket is unblocked when every file it lists is `resolved`.
- **Frontier**: scan `.scratch/<effort>/issues/` for files whose `Status:` is `open`, unblocked, and unclaimed; first by number wins.
- **Claim**: set `Status: claimed` and save before any work.
- **Resolve**: append the answer under an `## Answer` heading, set `Status: resolved`, then append a context pointer (gist + link) to the map's Decisions-so-far in `map.md`.

## Done when

- Every ticket you created or touched has a `Status:` line that matches its actual state (`open` / `claimed` / `resolved`), and each `resolved` answer is findable from the map's Decisions-so-far.
