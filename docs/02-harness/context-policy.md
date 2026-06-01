# Context Policy

## Source of Truth Order

1. `docs/00-project/current-state.md`
2. `docs/02-harness/context-policy.md`
3. `docs/01-architecture/README.md`
4. `docs/03-openspec/README.md`
5. `docs/04-development/versions/README.md`

## Current Stage Rule

- If the current state is `DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY`, do not create version design or capability changes.
- The current stage is `EXECUTION_AUTHORIZED` for the `metrics-snapshot-and-recording` change; only that change and its bounded tasks may be implemented.
- A new capability change requires a new OpenSpec change, a new authorization stage in `docs/00-project/current-state.md`, and an updated version design entry.

## Context Hygiene

- Use archive material only as history.
- Use current-state documents for authority.
- Do not mix future version ideas into current authorization.
