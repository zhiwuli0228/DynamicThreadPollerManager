# Operational and Evolution Boundaries

## Purpose

This document states the limits that future work should respect.

## Evolution Boundaries

- Do not expand into Redis, Kafka, database persistence, frontend, authentication, or multi-node coordination without a revised design.
- Do not introduce virtual-thread mode as a hidden default.
- Queue capacity and rejection strategy are now runtime-configurable (implemented in v0.9.0 and v0.10.0). Any future modification must maintain the existing safety coverage.

## Operational Boundaries

- Any runtime operation must be intentionally bounded.
- Any destructive operation must have explicit safety semantics.
- Any future change must remain traceable to an approved version design.
- Every archive, finalize, or delivery-closeout step must synchronize `docs/00-project/current-state.md` to the actual repository state before the change is considered complete.
- Verification, summary, and archive evidence must not claim stronger semantic coverage than the implementation and tests actually prove.
