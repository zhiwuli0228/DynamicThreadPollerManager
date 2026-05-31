# v0.1.0 Change Decomposition Plan

## Header

- Version name: `v0.1.0`
- Document purpose: outline how the version could later be split into bounded work packages
- Status: `DRAFT`
- Authorization status: `NOT AUTHORIZED`

## 1. Decomposition principles

- Decompose by capability boundary, not by file layout.
- Keep the experiment loop intact during decomposition.
- Separate observation from control.
- Keep queue behavior isolated from policy behavior.

## 2. Suggested work packages

1. Scenario generator and experiment runner.
2. Runtime snapshot collector.
3. Policy abstraction and baseline policy.
4. Adaptive policy implementation.
5. Executor mutation adapter.
6. Result recording and summary generation.
7. Optional analysis surface.

## 3. Dependency order

- Scenario generator before policy validation.
- Snapshot collector before adaptive evaluation.
- Policy abstraction before alternative strategies.
- Executor adapter before any actual scaling logic.
- Result recording before comparison analysis.

## 4. Authorization note

This is a planning-only decomposition. It is not an execution authorization and does not create an OpenSpec change.
