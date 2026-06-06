# Implementation Plan

## 0. Preconditions

- Read `docs/00-project/current-state.md` and confirm it is `EXECUTION_AUTHORIZED` before writing Java code.
- Read `docs/04-development/versions/v0.5.0/20-sr.md`.
- Read this change's `proposal.md`, `design.md`, `specs/executor-adapter-and-adjustment-evidence/spec.md`, and `tasks.md`.
- Do not implement during `CHANGE_DECOMPOSITION_ONLY`.

## 1. Adjustment Contracts

1. Add failing tests for `ScaleAdjustmentCommand` deterministic id and no-op rejection.
2. Implement `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/ScaleAdjustmentCommand.java`.
3. Add tests for `ExecutorStateSnapshot` required pool fields and read-only queue fields.
4. Implement `ExecutorStateSnapshot`.
5. Add tests for `AdjustmentStatus`, `AdjustmentFailureCode`, `AdjustmentResult`, and `AdjustmentEvidence`.
6. Implement status enums, result model, and evidence model.
7. Run targeted adjustment contract tests.

Suggested commit point: `feat(adjustment): add command and evidence contracts`

## 2. Runtime Safety Gate

1. Add failing tests for `NOT_READY`, `READY_WITH_RISK` without acceptance, cooldown, immediate opposite direction, per-run limit, and no-op.
2. Implement `RuntimeAdjustmentSafetyGate` interface.
3. Implement default config with:
   - `cooldownDecisionIntervals=2`
   - `maxAdjustmentsPerRun=5`
   - `blockImmediateOppositeDirection=true`
   - `allowReadyWithRisk=false`
4. Implement default safety gate decision model.
5. Run targeted safety gate tests.

Suggested commit point: `feat(adjustment): add runtime safety gate`

## 3. Adapter and In-Memory Probe

1. Add failing tests for `ExecutorAdjustmentAdapter.currentState()`.
2. Implement adapter contract and in-memory adjustable executor probe.
3. Add failing tests for `APPLIED`, `REJECTED`, `NO_OP`, and `FAILED`.
4. Implement adapter apply behavior.
5. Confirm no production `ThreadPoolExecutor` integration exists.
6. Run targeted adapter tests.

Suggested commit point: `feat(adjustment): add executor adapter probe`

## 4. Evidence and Boundary Tests

1. Add evidence tests for `evidenceType=runtime_adjustment`.
2. Verify rejected/failed evidence preserves before state and reason.
3. Add source inspection tests for forbidden dependencies:
   - policy package must not reference adjustment package;
   - analysis package must not invoke adjustment mutation;
   - adjustment package must not define `QueueCapacityController`;
   - adjustment package must not mutate queue capacity.
4. Confirm `pom.xml` has no new dependency.

Suggested commit point: `test(adjustment): add evidence and boundary coverage`

## 5. Full Verification

1. Run `openspec.cmd validate --all --json`.
2. Run `.\mvnw.cmd test`.
3. Update `tasks.md` checkboxes only after implementation and tests pass.
4. Generate `apply.md`.
5. Run verify and generate `verify.md`.

## Non-Scope Guard

Do not implement:

- queue resizing;
- closed-loop scheduler/controller;
- production `ThreadPoolExecutor` integration;
- persistence, REST, UI, or external dependencies;
- throughput improvement claims.
