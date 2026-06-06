# Apply Notes — `executor-adapter-and-adjustment-evidence`

## Scope Confirmation

- Authoritative branch: `claude_master`.
- Authorized change: `executor-adapter-and-adjustment-evidence` only.
- Files added or modified are confined to:
  - `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/**`
  - `src/test/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/**`
  - `openspec/changes/executor-adapter-and-adjustment-evidence/{apply.md,tasks.md,verify.md}`
- `pom.xml` was not modified. No new dependencies.
- No mutation of queue capacity, no production `ThreadPoolExecutor`
  integration, no closed-loop scheduler/controller, no
  persistence, no REST/API/UI, no throughput improvement claims.

## Implementation Walk-through

### Task 1 — Adjustment Contracts

The change creates a new `experiment.adjustment` package from
scratch. The contract types are intentionally narrow so they
can be consumed by the runtime safety gate, the adapter, and the
evidence record without leaking through the package boundary.

- `ScaleAdjustmentCommand` — deterministic
  `commandId = "<runId>:<decisionTimestamp>:<currentPoolSize>-><targetPoolSize>"`.
  Constructed via the static `create(...)` factory, which rejects
  no-op targets (caller must use `noOp(...)` instead), blank
  reason, blank source decision ref, null run id, null decision
  timestamp, null clock, negative sizes. A package-private raw
  constructor is exposed so the adapter and tests can build
  intentionally invalid commands to exercise the adapter's own
  validation layer.
- `ExecutorStateSnapshot` — required pool fields
  `observedAt`, `corePoolSize`, `maximumPoolSize`; optional
  read-only `activeCount`, `queueSize`, `queueCapacity` fields.
  Builder enforces `1 <= corePoolSize <= maximumPoolSize`,
  non-negative queue fields, non-null `observedAt`.
- `AdjustmentStatus` — five values: `APPLIED | REJECTED | NO_OP |
  FAILED | DEFERRED`.
- `AdjustmentFailureCode` — eight codes: `NOT_READY,
  RISK_NOT_ACCEPTED, COOLDOWN_ACTIVE, OPPOSITE_DIRECTION,
  RUN_LIMIT_EXCEEDED, INVALID_COMMAND, PROBE_FAILURE, UNSUPPORTED`.
- `AdjustmentResult` — command, status, before/after state,
  requested/applied pool size, reason, failure code (required
  for `REJECTED`/`FAILED`/`DEFERRED`, forbidden otherwise), source
  decision ref, decision timestamp.
- `AdjustmentEvidence` — `evidenceType` is fixed to
  `"runtime_adjustment"` via the `EVIDENCE_TYPE` constant. Same
  status / failure code rules as the result. Carries
  `recordedTimestamp` distinct from the source
  `decisionTimestamp`.

Tests: `AdjustmentContractsTest` (34 tests) covers deterministic
id, no-op rejection, blank/null validation, queue read-only,
builder invariants, status and failure code consistency, and
evidence `evidenceType` immutability.

### Task 2 — Runtime Safety Gate

- `SafetyGateConfig` — immutable record of
  `cooldownDecisionIntervals`, `maxAdjustmentsPerRun`,
  `blockImmediateOppositeDirection`, `allowReadyWithRisk`. The
  static `defaults()` factory pins the design values: `2`, `5`,
  `true`, `false`.
- `SafetyGateDecision` — `ALLOW | REJECTED | NO_OP` outcome,
  optional failure code (required for `REJECTED`), reason,
  applied-count snapshot, and an `appliedCommand` reference
  recorded on `ALLOW` so `recordApplied(...)` can update the
  per-instance history.
- `RuntimeAdjustmentSafetyGate` — interface with `evaluate(...)`
  and `recordApplied(...)`. The gate never mutates the executor.
- `DefaultRuntimeAdjustmentSafetyGate` — enforces the spec
  scenario order: (1) input validation, (2) `NOT_READY` reject,
  (3) `READY_WITH_RISK` reject unless `allowReadyWithRisk`, (4)
  cooldown reject (decrements the remaining counter), (5)
  per-run limit reject, (6) opposite-direction reject when
  `blockImmediateOppositeDirection` is enabled and the
  candidate's `currentPoolSize` matches the last applied
  target, (7) no-op, (8) allow.

Tests: `RuntimeAdjustmentSafetyGateTest` (18 tests) covers
defaults, the six spec scenarios, the no-op case, the null
argument checks, that rejected decisions are not recorded, and
the configured per-run cap.

### Task 3 — Adapter and In-Memory Probe

- `ExecutorAdjustmentAdapter` — interface with
  `currentState()` and `apply(ScaleAdjustmentCommand)`. The
  Javadoc explicitly notes that the first bounded change MUST
  NOT integrate a production standard-library thread pool.
- `InMemoryAdjustableExecutorProbe` — owns the controlled state
  (`corePoolSize`, fixed `maximumPoolSize`, fixed
  `queueCapacity`) and an `appliedAdjustmentCount`. The
  `apply(...)` method:
  - returns `REJECTED` + `INVALID_COMMAND` when the target is
    below 1 or above `maximumPoolSize`;
  - returns `NO_OP` when the target matches the current state;
  - delegates the mutation to a `doSetCorePoolSize(...)` hook
    that the test suite can override to inject failures;
  - converts any thrown `RuntimeException` into a `FAILED`
    result with `PROBE_FAILURE` and preserves the original
    `beforeState`;
  - returns `APPLIED` on success and increments
    `appliedAdjustmentCount`.

Tests: `ExecutorAdjustmentAdapterTest` (15 tests) covers
state exposure, sizing validation, `APPLIED`, `NO_OP`,
`REJECTED` (target < 1, target > max), `FAILED` (forced
exception), full gate+probe pipeline wiring, queue-capacity
immutability, and adjustment counting.

### Task 4 — Evidence and Boundary Verification

- `AdjustmentEvidence` is the only producer of `runtime_adjustment`
  evidence. The `evidenceType` constant is exposed and is
  asserted in tests.
- `AdjustmentEvidenceTest` (6 tests) covers applied, rejected,
  failed evidence construction, evidence-type hygiene (no
  `offline_replay` tag, no `replay` substring), and the full
  gate + probe + evidence pipeline.
- `AdjustmentBoundaryIsolationTest` (5 tests) is a source-level
  boundary test that walks every Java file in
  `experiment.adjustment` and asserts the absence of
  `ThreadPoolExecutor`, `ScheduledExecutorService`,
  `QueueCapacityController`, persistence / REST / DB substrings.
  It also walks the `policy`, `analysis`, and `scenario`
  packages and asserts the absence of any reference to
  `experiment.adjustment`, `ScaleAdjustmentCommand`,
  `ExecutorAdjustmentAdapter`, or `AdjustmentEvidence`.
- `pom.xml` is unchanged. The full Maven test suite (314 tests,
  see `verify.md`) passes.

## Deviations from the Design

- The `AdjustmentResult.reason` and `AdjustmentEvidence.reason`
  fields always carry the **command's reason** in every
  status, so the source-decision intent is preserved through
  both applied and rejected / failed adjustments. The probe's
  internal validation messages live on the failure code and on
  the log path; the result's reason is reserved for the source
  decision. This is asserted by
  `ExecutorAdjustmentAdapterTest.applyShouldRejectWhenTargetExceedsMaximum`,
  `applyShouldRejectWhenTargetBelowOne`,
  `applyShouldReturnFailedWhenProbeThrows`, and
  `AdjustmentEvidenceTest.shouldBuildEvidenceFromRejectedResult`.

- The `AdjustmentFailureCode` enum has eight values to cover both
  the safety-gate rejection causes (`NOT_READY`,
  `RISK_NOT_ACCEPTED`, `COOLDOWN_ACTIVE`, `OPPOSITE_DIRECTION`,
  `RUN_LIMIT_EXCEEDED`) and the adapter-side causes
  (`INVALID_COMMAND`, `PROBE_FAILURE`, `UNSUPPORTED`). The
  `UNSUPPORTED` value is reserved for `DEFERRED`; the
  `ExecutorAdjustmentAdapter` returns `DEFERRED` semantics by
  returning `REJECTED` + `UNSUPPORTED` if it ever needs to
  signal that a specific adjustment type is not supported. The
  first probe does not produce `DEFERRED` results, but the
  failure code is exposed for forward compatibility.

- The `ScaleAdjustmentCommand` package-private raw constructor is
  retained so the `ExecutorAdjustmentAdapter` can be tested
  with intentionally invalid commands (e.g. `targetPoolSize=0`).
  The public API still requires callers to use `create(...)` or
  `noOp(...)`, which enforce the full validation contract.

## Verification Status

- `openspec.cmd validate --all --json` passes (`passed: 5,
  failed: 0`).
- `.\mvnw.cmd test` runs 314 tests, all green. (See `verify.md`
  for the per-suite breakdown.)
- `pom.xml` was not modified; no new dependencies were added.

## Files Touched

### Production sources (added)

- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/ScaleAdjustmentCommand.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/ExecutorStateSnapshot.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/AdjustmentStatus.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/AdjustmentFailureCode.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/AdjustmentResult.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/AdjustmentEvidence.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/SafetyGateConfig.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/SafetyGateDecision.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/RuntimeAdjustmentSafetyGate.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/DefaultRuntimeAdjustmentSafetyGate.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/ExecutorAdjustmentAdapter.java`
- `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/InMemoryAdjustableExecutorProbe.java`

### Tests (added)

- `src/test/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/AdjustmentContractsTest.java`
- `src/test/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/RuntimeAdjustmentSafetyGateTest.java`
- `src/test/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/ExecutorAdjustmentAdapterTest.java`
- `src/test/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/AdjustmentEvidenceTest.java`
- `src/test/java/com/zhiwu/dynamicthreadpollermanager/experiment/adjustment/AdjustmentBoundaryIsolationTest.java`

### Other (added)

- `openspec/changes/executor-adapter-and-adjustment-evidence/apply.md`
- `openspec/changes/executor-adapter-and-adjustment-evidence/verify.md`
- updated `openspec/changes/executor-adapter-and-adjustment-evidence/tasks.md`

### Other (not modified)

- `pom.xml` (no new dependencies)
- All other `experiment/**` packages, `application/**`, `domain/**`,
  `infrastructure/**` source and test code.
- All existing archived capability specs and the
  `metrics-snapshot-and-recording` change.
