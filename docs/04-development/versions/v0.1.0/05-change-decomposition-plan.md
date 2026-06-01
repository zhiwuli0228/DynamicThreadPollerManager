# v0.1.0 Change Decomposition Plan

## Header

- Version name: `v0.1.0`
- Document purpose: outline how the version is split into bounded work packages
- Status: `BASELINE_DELIVERED`
- Authorization status: `BASELINE_DELIVERED` (work packages 1 and 2 executed and archived; remaining packages deferred to a successor version)

## 1. Decomposition principles

- Decompose by capability boundary, not by file layout.
- Keep the experiment loop intact during decomposition.
- Separate observation from control.
- Keep queue behavior isolated from policy behavior.

## 2. Work packages

1. Scenario generator and experiment runner. → delivered as `experiment-foundation` (archived 2026-06-01)
2. Runtime snapshot collector. → delivered as `metrics-snapshot-and-recording` (archived 2026-06-02)
3. Policy abstraction and baseline policy. (deferred to successor version)
4. Adaptive policy implementation. (deferred to successor version)
5. Executor mutation adapter. (deferred to successor version)
6. Result recording and summary generation. (deferred to successor version)
7. Optional analysis surface. (deferred to successor version)

## 3. Dependency order

- Scenario generator before policy validation.
- Snapshot collector before adaptive evaluation.
- Policy abstraction before alternative strategies.
- Executor adapter before any actual scaling logic.
- Result recording before comparison analysis.

## 4. Authorization note

Work packages 1 and 2 are delivered and archived. The remaining work packages (3–7) are explicitly deferred to a successor version design; they are not authorized under `v0.1.0`. Any future change touching this scope must be authorized by a successor version package reaching `READY_FOR_CHANGE_DECOMPOSITION` or `EXECUTION_AUTHORIZED`.
