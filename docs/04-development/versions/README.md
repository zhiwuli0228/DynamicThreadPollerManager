# Version Designs

This folder is reserved for version design packages.

## Current Status

- Three version packages exist: `v0.1.0`, `v0.2.0`, and `v0.3.0`.
- `v0.1.0` is `BASELINE_DELIVERED`.
- `v0.2.0` is `BASELINE_DELIVERED`.
- `v0.3.0` is `READY_FOR_CHANGE_DECOMPOSITION`; the next OpenSpec change candidate is `adaptive-policy-and-control-gate`.
- See `docs/00-project/current-state.md` for the authoritative execution status.

## Lifecycle

Version designs should move through these states:

1. `DRAFT`
2. `BASELINED`
3. `READY_FOR_CHANGE_DECOMPOSITION`
4. `EXECUTION_AUTHORIZED`
5. `IMPLEMENTED`
6. `SUPERSEDED`

## Version Directory Naming

Use semantic version directory names for concrete version designs:

- `v0.1.0/` for the first exploratory runnable version.
- `v0.2.0/` for a later experimental increment.
- `v1.0.0/` only when a stable version boundary is intentionally established.

Do not use ambiguous paths such as `v1-final/`, `latest/` or `new-design/`.

## Version Document Set

A future authorized version design uses:

docs/04-development/versions/<version>/
├─ README.md
├─ 00-objectives-and-scope.md
├─ 01-requirements-and-use-cases.md
├─ 02-solution-design.md
├─ 03-api-and-observability-design.md
├─ 04-testing-and-acceptance-design.md
├─ 05-change-decomposition-plan.md
└─ decision-log.md

## Rule

- Create a version directory only after a task explicitly authorizes a version design.
- Do not create additional concrete version directories unless the current state explicitly allows it.
- Do not create `openspec/changes/**` from this directory unless the version design authorizes it.
