# Task Execution Policy

## Current Stage

This repository is in `EXECUTION_AUTHORIZED` for `scenario-runner-and-baseline`.

The current stage is authoritative only as recorded in `docs/00-project/current-state.md`.

## Execution Rule

- Execute the scope of the active task directly.
- Do not infer an implementation mission from a documentation task.
- Do not branch into unrelated capabilities or framework expansion.
- The only active capability change is `scenario-runner-and-baseline`.
- Do not begin a neighboring change without a successor version design, OpenSpec change, and updated authorization in `docs/00-project/current-state.md`.

## Autonomy Rule

- When a task authorizes a bounded sequence, complete it without pausing between internal steps unless a real blocker appears.
