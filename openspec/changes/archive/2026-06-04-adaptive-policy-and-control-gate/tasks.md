## 1. Policy Models

- [x] 1.1 Create the `experiment/policy` package.
- [x] 1.2 Add `PolicyAction` with `SCALE_UP`, `SCALE_DOWN`, and `HOLD`.
- [x] 1.3 Add `GateStatus` with `ACCEPTED`, `CAPPED`, `HOLD`, and `REJECTED`.
- [x] 1.4 Add immutable `ThresholdPolicyConfig` with validation for policy id, pool bounds, thresholds, and scale step.
- [x] 1.5 Add immutable `PolicyEvaluationInput` with run id, pressure snapshot, and evaluation timestamp.

## 2. Policy Decision Output

- [x] 2.1 Add immutable `PolicyDecision` with run id, policy id, timestamp, action, gate status, current pool size, proposed pool size, and reason.
- [x] 2.2 Validate non-blank ids, non-null enum/timestamp values, non-negative pool sizes, and non-blank reason.
- [x] 2.3 If `ScaleDecision` conversion is implemented, allow conversion only for accepted or capped non-hold decisions.
- [x] 2.4 Add tests for decision field exposure, validation, reason preservation, and conversion behavior.

## 3. Control Gate

- [x] 3.1 Add `ControlGate` interface.
- [x] 3.2 Implement `DefaultControlGate` hold behavior for explicit hold actions.
- [x] 3.3 Implement accepted behavior for safe proposals within bounds.
- [x] 3.4 Implement max-bound capping behavior.
- [x] 3.5 Implement min-bound capping behavior.
- [x] 3.6 Implement no-op hold behavior when proposed size equals current size.
- [x] 3.7 Add tests for all gate outcomes and exact proposed pool sizes.

## 4. Threshold Policy Evaluator

- [x] 4.1 Add `PolicyEvaluator` interface.
- [x] 4.2 Implement `ThresholdPolicyEvaluator` with scale-up on high active threads.
- [x] 4.3 Implement scale-up on high queue size.
- [x] 4.4 Implement scale-down on low active threads with empty queue.
- [x] 4.5 Implement hold for normal pressure.
- [x] 4.6 Ensure scale-up wins if scale-up and scale-down conditions both appear true.
- [x] 4.7 Ensure decision timestamp comes from `PolicyEvaluationInput`.
- [x] 4.8 Add evaluator tests for each threshold path and min/max bound interaction.

## 5. Boundary and Verification

- [x] 5.1 Add a policy boundary isolation test that scans the policy package for scenario, executor mutation, thread-pool, scheduler, and adjustment-event references.
- [x] 5.2 Confirm no new dependencies are added to `pom.xml`.
- [x] 5.3 Run `openspec.cmd validate --all --json`.
- [x] 5.4 Run `.\mvnw.cmd test`.
- [x] 5.5 Confirm `git status --short` before handoff.
