# Adaptive Policy and Control Gate Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development
> to implement this plan task-by-task.

**Goal:** Add deterministic threshold policy evaluation and explicit control gates that produce reasoned policy decisions without mutating executors or queues.

**Architecture:** Implement a small `experiment.policy` package. The package consumes `PressureSnapshot`, evaluates thresholds, applies min/max/no-op gates, and returns immutable `PolicyDecision` values. It must not import scenario runner or executor mutation classes.

**Tech Stack:** Java 21, existing Spring Boot project, JUnit 5, Maven. No new dependencies.

---

## Task 1: Policy Models

- [ ] **Step 1:** Create `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/policy/`.
- [ ] **Step 2:** Add `PolicyAction` enum with exactly `SCALE_UP`, `SCALE_DOWN`, and `HOLD`.
- [ ] **Step 3:** Add `GateStatus` enum with exactly `ACCEPTED`, `CAPPED`, `HOLD`, and `REJECTED`.
- [ ] **Step 4:** Write failing tests for valid `ThresholdPolicyConfig` construction.
- [ ] **Step 5:** Implement `ThresholdPolicyConfig` with fields `policyId`, `minPoolSize`, `maxPoolSize`, `scaleUpActiveThreadsThreshold`, `scaleUpQueueSizeThreshold`, `scaleDownActiveThreadsThreshold`, and `scaleStep`.
- [ ] **Step 6:** Write failing tests for blank policy id, min <= 0, max < min, negative thresholds, and scaleStep <= 0.
- [ ] **Step 7:** Implement config validation.
- [ ] **Step 8:** Write failing tests for valid `PolicyEvaluationInput` construction using a controlled `Instant` and `PressureSnapshot`.
- [ ] **Step 9:** Implement `PolicyEvaluationInput` with run id, snapshot, and evaluatedAt validation.
- [ ] **Step 10:** Run policy model tests and commit.

## Task 2: Policy Decision Output

- [ ] **Step 1:** Write failing tests for `PolicyDecision` exposing run id, policy id, timestamp, action, gate status, current pool size, proposed pool size, and reason.
- [ ] **Step 2:** Implement immutable `PolicyDecision`.
- [ ] **Step 3:** Write failing validation tests for blank ids, null timestamp, null action, null gate status, negative pool sizes, and blank reason.
- [ ] **Step 4:** Implement validation.
- [ ] **Step 5:** Decide whether to implement `toScaleDecision()`. If implemented, first write tests proving accepted and capped non-hold decisions convert correctly.
- [ ] **Step 6:** If conversion is implemented, write tests proving `HOLD` and `REJECTED` decisions throw on conversion.
- [ ] **Step 7:** Implement conversion only if tests from steps 5 and 6 exist. If not implementing conversion, document that downstream executor adapter will convert later.
- [ ] **Step 8:** Run decision tests and commit.

## Task 3: Control Gate

- [ ] **Step 1:** Add `ControlGate` interface. Recommended method: `PolicyDecision apply(PolicyEvaluationInput input, ThresholdPolicyConfig config, PolicyAction action, int proposedPoolSize, String reason)`.
- [ ] **Step 2:** Write failing test: explicit `HOLD` action returns `GateStatus.HOLD`, action `HOLD`, current pool size, and non-blank reason.
- [ ] **Step 3:** Implement hold handling.
- [ ] **Step 4:** Write failing test: safe scale-up proposal within bounds returns `GateStatus.ACCEPTED` and preserves proposed size.
- [ ] **Step 5:** Implement accepted handling.
- [ ] **Step 6:** Write failing test: proposal above max caps to max and returns `GateStatus.CAPPED` when max differs from current.
- [ ] **Step 7:** Implement max cap handling.
- [ ] **Step 8:** Write failing test: proposal below min caps to min and returns `GateStatus.CAPPED` when min differs from current.
- [ ] **Step 9:** Implement min cap handling.
- [ ] **Step 10:** Write failing test: proposed size equal to current returns `GateStatus.HOLD`.
- [ ] **Step 11:** Implement no-op hold handling.
- [ ] **Step 12:** Run gate tests and commit.

## Task 4: Threshold Policy Evaluator

- [ ] **Step 1:** Add `PolicyEvaluator` interface with `PolicyDecision evaluate(PolicyEvaluationInput input, ThresholdPolicyConfig config)`.
- [ ] **Step 2:** Add `ThresholdPolicyEvaluator` that depends on `ControlGate`.
- [ ] **Step 3:** Write failing test: active threads >= scale-up threshold returns `SCALE_UP` with proposed size `currentPoolSize + scaleStep`.
- [ ] **Step 4:** Implement active-thread scale-up path.
- [ ] **Step 5:** Write failing test: queue size >= scale-up queue threshold returns `SCALE_UP` with proposed size `currentPoolSize + scaleStep`.
- [ ] **Step 6:** Implement queue-size scale-up path.
- [ ] **Step 7:** Write failing test: active threads <= scale-down threshold and queue size == 0 returns `SCALE_DOWN` with proposed size `currentPoolSize - scaleStep`.
- [ ] **Step 8:** Implement scale-down path.
- [ ] **Step 9:** Write failing test: normal pressure returns `HOLD` with current pool size.
- [ ] **Step 10:** Implement normal-pressure hold path.
- [ ] **Step 11:** Write failing test: if scale-up and scale-down conditions both appear true, scale-up wins.
- [ ] **Step 12:** Ensure evaluator checks scale-up before scale-down.
- [ ] **Step 13:** Write failing test: decision timestamp equals input `evaluatedAt`.
- [ ] **Step 14:** Ensure evaluator and gate never call `Instant.now()`.
- [ ] **Step 15:** Add tests for max cap and min cap through evaluator.
- [ ] **Step 16:** Run evaluator tests and commit.

## Task 5: Boundary and Final Verification

- [ ] **Step 1:** Add `PolicyBoundaryIsolationTest` under `src/test/java/com/zhiwu/dynamicthreadpollermanager/experiment/policy/`.
- [ ] **Step 2:** Scan `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/policy/` for forbidden strings: `ScenarioExperimentRunner`, `BaselineWorkloadExecutor`, `.scenario.`, `ExecutorAdapter`, `QueueCapacityController`, `MutationValidator`, `AdjustmentEvent`, `ThreadPoolExecutor`, `ScheduledExecutorService`.
- [ ] **Step 3:** Confirm `pom.xml` has no new dependencies.
- [ ] **Step 4:** Run `openspec.cmd validate --all --json`.
- [ ] **Step 5:** Run `.\mvnw.cmd test`.
- [ ] **Step 6:** Update `tasks.md` checkboxes only after the corresponding implementation and tests pass.
- [ ] **Step 7:** Prepare `apply.md` only after implementation is complete, with commit range, task counts, and test results.

## Implementation Notes

- Use `PressureSnapshot.poolSize()` as the current pool size.
- Use `PressureSnapshot.activeThreads()` and `PressureSnapshot.queueSize()` for threshold decisions.
- Do not use `completedTaskCount()` for policy decisions in this version.
- Do not import or instantiate `ControlPolicy`; the policy id string in `ThresholdPolicyConfig` is enough.
- Do not create `AdjustmentEvent`; executor mutation is deferred.
- If any design detail is ambiguous, choose the smaller deterministic behavior and document it in tests.
