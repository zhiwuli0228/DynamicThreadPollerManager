# Task Execution Policy

## Current Stage

This repository is in `EXECUTION_AUTHORIZED` for the `metrics-snapshot-and-recording` change.

## Execution Rule

- Execute the scope of the active task directly.
- Do not infer an implementation mission from a documentation task.
- Do not branch into unrelated capabilities or framework expansion.
- The only currently authorized change is `metrics-snapshot-and-recording`; do not begin a neighboring change without updating the version design and `docs/00-project/current-state.md`.

## Autonomy Rule

- When a task authorizes a bounded sequence, complete it without pausing between internal steps unless a real blocker appears.
