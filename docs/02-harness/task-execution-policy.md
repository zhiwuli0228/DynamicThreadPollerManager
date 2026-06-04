# Task Execution Policy

## Current Stage

This repository is in `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED` after archiving `adaptive-policy-and-control-gate`.

The current stage is authoritative only as recorded in `docs/00-project/current-state.md`.

## Execution Rule

- Execute the scope of the active task directly.
- Do not infer an implementation mission from a documentation task.
- Do not branch into unrelated capabilities or framework expansion.
- No Java implementation scope is active unless `docs/00-project/current-state.md` names one explicitly.
- Do not begin a neighboring change without a successor version design, OpenSpec change, and updated authorization in `docs/00-project/current-state.md`.
- Do not implement executor mutation, queue resizing, scenario changes, persistence, external API, or new dependencies without explicit successor authorization.

## Autonomy Rule

- When a task authorizes a bounded sequence, complete it without pausing between internal steps unless a real blocker appears.
