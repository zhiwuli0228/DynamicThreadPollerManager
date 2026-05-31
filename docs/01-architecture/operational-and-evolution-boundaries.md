# Operational and Evolution Boundaries

## Purpose

This document states the limits that future work should respect.

## Evolution Boundaries

- Do not expand into Redis, Kafka, database persistence, frontend, authentication, or multi-node coordination without a revised design.
- Do not introduce virtual-thread mode as a hidden default.
- Do not replace queue capacity or rejection strategy at runtime without explicit design and safety coverage.

## Operational Boundaries

- Any runtime operation must be intentionally bounded.
- Any destructive operation must have explicit safety semantics.
- Any future change must remain traceable to an approved version design.
