# Task Execution Policy

## Current Stage

This repository is in `VERSION_FUNCTIONAL_DESIGN_AUTHORIZED` for `v0.4.0`.

The current stage is authoritative only as recorded in `docs/00-project/current-state.md`.

## Execution Rule

- Execute the scope of the active task directly.
- Do not infer an implementation mission from a documentation task.
- Do not branch into unrelated capabilities or framework expansion.
- SR functional design is currently allowed only for the version named in `docs/00-project/current-state.md`.
- No Java implementation scope is active unless `docs/00-project/current-state.md` names one explicitly.
- Do not begin a neighboring change without a successor version design, OpenSpec change, and updated authorization in `docs/00-project/current-state.md`.
- Do not implement executor mutation, queue resizing, scenario changes, persistence, external API, or new dependencies without explicit successor authorization.
- For future implementation work, do not start coding unless `docs/02-harness/managed-change-standard.md` gates are satisfied: IR closure, SR closure, OpenSpec authorization, and synchronized current-state authority.

## Autonomy Rule

- When a task authorizes a bounded sequence, complete it without pausing between internal steps unless a real blocker appears.
- A missing managed-change gate is a real blocker; report it instead of inferring permission.
