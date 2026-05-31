# Version Designs

This folder is reserved for future version design packages.

## Current Status

- No active version directory exists yet.
- No version is currently authorized for change decomposition.
- The repository is in `DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY`.

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
- No concrete version directory may be created while current stage is `DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY`.
- Do not create `openspec/changes/**` from this directory unless the version design authorizes it.
