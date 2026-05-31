# Living Architecture Index

## Purpose

This directory describes the target system model, architectural boundaries, quality attributes, and experiment strategy. It is a design baseline, not a statement of current implementation.

## Current Status

- Architecture baseline status: established in Phase 02.
- Current implementation status: no dynamic thread-pool business capability implemented.
- First-version design status: pending unified planning after framework completion.

## Document Map

1. `docs/architecture/00-system-context-and-quality-attributes.md`
2. `docs/architecture/01-logical-architecture-and-package-boundaries.md`
3. `docs/architecture/02-managed-executor-domain-model.md`
4. `docs/architecture/03-scheduling-reconfiguration-and-recovery-model.md`
5. `docs/architecture/04-observability-and-experiment-strategy.md`
6. `docs/architecture/05-operational-and-evolution-boundaries.md`
7. `docs/architecture/06-v1-unified-design-planning-framework.md`

## Reading Rules

- Governance or scope work must read Harness first.
- Unified version design must read all architecture documents.
- A bounded implementation task reads only the architecture documents relevant to its approved change.
- No document in this directory proves implementation completion.
