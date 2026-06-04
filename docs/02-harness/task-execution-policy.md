# Task Execution Policy

## Current Stage

This repository is in `EXECUTION_AUTHORIZED` for `v0.4.0`.

The current stage is authoritative only as recorded in `docs/00-project/current-state.md`.

## Execution Rule

- Execute the scope of the active task directly.
- Do not infer an implementation mission from a documentation task.
- Do not branch into unrelated capabilities or framework expansion.
- Change execution is currently allowed only for the active change named in `docs/00-project/current-state.md`.
- Java implementation scope is active only for the bounded change named in `docs/00-project/current-state.md`.
- Do not begin a neighboring change without a successor version design, OpenSpec change, and updated authorization in `docs/00-project/current-state.md`.
- Do not implement executor mutation, queue resizing, scenario changes, persistence, external API, or new dependencies without explicit successor authorization.
- For implementation work, do not code outside the active change boundary even if adjacent packages appear related.

## Autonomy Rule

- When a task authorizes a bounded sequence, complete it without pausing between internal steps unless a real blocker appears.
- A missing managed-change gate is a real blocker; report it instead of inferring permission.
