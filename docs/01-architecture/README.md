# Living Architecture

This directory carries the long-lived target-system design for the repository. It describes the architecture we want to preserve and evolve toward, not the current implementation.

## Current Status

- Architecture baseline: established as a documentation framework.
- Implementation status: no business capability has been implemented.
- ADR status: no ADR has been created yet.
- Version design status: not started in this stage.

## Documents

1. `docs/01-architecture/system-context-and-quality-attributes.md`
2. `docs/01-architecture/logical-architecture-and-package-boundaries.md`
3. `docs/01-architecture/managed-executor-domain-model.md`
4. `docs/01-architecture/scheduling-reconfiguration-and-recovery-model.md`
5. `docs/01-architecture/observability-and-experiment-strategy.md`
6. `docs/01-architecture/operational-and-evolution-boundaries.md`
7. `docs/01-architecture/decisions/README.md`

## Reading Rule

- Read this directory after `docs/00-project/current-state.md`.
- Use this directory to understand target boundaries before any future version design.

## What Belongs Here

`docs/01-architecture/` carries long-lived architecture design shared across
future versions, including system context, package boundaries, domain models,
observability principles and evolution boundaries.

It does not carry:

- a concrete version scope;
- implementation tasks;
- OpenSpec change artifacts;
- implementation receipts.

## Naming Rule

Living architecture documents use stable semantic kebab-case names:

- `system-context-and-quality-attributes.md`
- `logical-architecture-and-package-boundaries.md`
- `managed-executor-domain-model.md`
- `scheduling-reconfiguration-and-recovery-model.md`
- `observability-and-experiment-strategy.md`
- `operational-and-evolution-boundaries.md`

Do not create version-suffixed copies such as `architecture-v1-final.md`.
Accepted long-lived changes update the living document; Git preserves history.

## Decision Records

Long-lived architectural decisions belong under `docs/01-architecture/decisions/`.
