# Architecture Decision Records

## Purpose

This directory carries decisions that alter long-lived architecture across
versions. ADRs are not required for wording fixes or temporary version-scoped
choices.

## Naming Rule

Use:

`ADR-<four-digit-sequence>-<kebab-case-decision-topic>.md`

Examples:

- `ADR-0001-adopt-redis-lease-for-distributed-coordination.md`
- `ADR-0002-support-virtual-thread-execution-mode.md`

Do not create an ADR in the current documentation-framework-only task.

## Status Flow

`PROPOSED -> ACCEPTED -> SUPERSEDED | REJECTED`

## Architecture Change Handling

### Documentation Clarification

Typos, clarifying wording and diagram readability corrections update living
architecture documents directly. No ADR is required.

### Version-Scoped Decision

A decision affecting only one future version belongs in:

`docs/04-development/versions/<version>/decision-log.md`

It does not become an ADR unless it establishes a long-lived architecture rule.

### Long-Lived Architecture Decision

A decision that changes cross-version architecture boundaries, supported
technology direction, domain contracts, persistence/coordination strategy or
execution mode must:

1. create an ADR here;
2. update the related living architecture documents in the same authorized work;
3. be referenced from the authorizing version design or future change.

### Concrete Implementation Change

Concrete implementation belongs in:

`openspec/changes/<change-name>/`

only after a version design authorizes change decomposition.

### Implemented Behavior

After future verified delivery and OpenSpec archive/synchronization:

- implemented behavior belongs in `openspec/specs/`;
- historical implementation change artifacts belong in `openspec/changes/archive/`;
- living architecture reflects accepted long-lived structure;
- ADRs remain permanent records of accepted or superseded decisions.

## Current Status

No ADR exists or is authorized in the current
`DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY` stage.
