# V1 Change Decomposition and Autonomous Execution Plan

## 1. Purpose

This plan decomposes the V1 design into a small set of authorized changes that a future Claude Code autonomous mission can execute continuously.

## 2. V1 Authorized Change Set

| Order | Change Name | Purpose | Depends On | Implementation May Proceed Automatically After Verification |
|---|---|---|---|---|
| 1 | `establish-springboot-management-foundation` | Introduce the web, validation, actuator, and error-handling foundation for the management surface. | None | YES |
| 2 | `establish-local-managed-executor-registry` | Add the in-memory registry, executor snapshot model, and runtime update use cases. | 1 | YES |
| 3 | `expose-experiment-workloads-and-observability` | Add controlled workloads, status queries, and metrics-backed experiment feedback. | 2 | YES |

## 3. Why This Split

- The first change establishes the management surface without pretending the runtime behavior already exists.
- The second change introduces the core executor management behavior and its atomic update rules.
- The third change closes the experiment loop by making the runtime change observable and repeatable.
- Dynamic scheduled task reconfiguration is intentionally excluded, so the V1 set stays small and testable.

## 4. Branch and Closeout Strategy

Selected strategy: **single implementation branch with sequential autonomous changes**.

```text
claude_master
  -> ai/v1-implementation
  -> sequential SuperSpec changes on the same branch
  -> commit and push after each verified change
  -> final validation
  -> gh PR or merge closeout selected by the mission
  -> integrate accepted result back toward claude_master
```

### Why this strategy

- It minimizes branch churn.
- It preserves traceability through one continuous evidence branch.
- It keeps rollback and review simple.
- It avoids artificial pauses between changes.

## 5. Autonomous Execution Rules

- The mission may proceed from one authorized change to the next without per-change human approval.
- Each change must be verified before the next change begins.
- Any required auto-fix inside the authorized V1 scope is allowed.
- Any scope expansion beyond the V1 exclusions must return `BLOCKED`.

## 6. OpenSpec / SuperSpec Relationship

- The future Claude Code mission is responsible for creating the actual `openspec/changes/<change-name>/` artifacts for each authorized change.
- This design package is the input to those change artifacts.
- Each change should cite the relevant V1 sections for scope, validation, and exclusion rules.
- `openspec validate` and the change verification step are the gate to the next authorized change.

## 7. Evidence Expectations

Each authorized change should leave:

- a commit,
- a push,
- test output,
- verification output,
- and a remote branch state that can be confirmed through `gh`.
