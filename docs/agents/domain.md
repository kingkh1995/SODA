---
type: Agent Workflow
title: Domain Docs
description: When exploring the codebase, writing an ADR reference, or naming a domain concept — read CONTEXT.md first, find decisions through docs/adr/_index.md, and use the glossary's vocabulary.
status: stable
---

# Domain Docs

How the engineering skills consume this repo's domain documentation. SODA is single-context: one `CONTEXT.md` at the repo root, one `docs/adr/` for every decision.

## Before exploring, read these

- **`CONTEXT.md`** at the repo root — the glossary: what each domain concept is called, and which synonyms it explicitly avoids.
- **[`adr/_index.md`](../adr/_index.md)** — the ADR theme index. Start here to find the ADRs touching the area you're about to work in, then read those ADRs before you hypothesise or propose.

If any of these don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `CONTEXT.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal — either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-NNNN (the decision it makes) — but worth reopening because…_

## Done when

- Every domain concept your output names uses the glossary term, or the gap is noted for `/domain-modeling`.
- Every conflict with an existing ADR is written out with its ADR id instead of being silently overridden.
