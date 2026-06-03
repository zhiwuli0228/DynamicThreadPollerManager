# v0.3.0 Testing and Acceptance Design

## Header

- Version name: `v0.3.0`
- Status: `READY_FOR_CHANGE_DECOMPOSITION`
- Change candidate: `adaptive-policy-and-control-gate`

## 1. Test Strategy

Tests must be deterministic and scenario-specific. They should construct `PressureSnapshot` inputs directly.

Do not run scenario workloads in policy tests. Scenario runner behavior is already covered by `v0.2.0`.

## 2. Required Test Groups

### 2.1 Configuration tests

Verify:

- valid config creation,
- blank policy id rejected,
- invalid min/max rejected,
- negative thresholds rejected,
- non-positive scale step rejected.

### 2.2 Gate tests

Verify:

- hold action remains hold,
- proposed size above max is capped or held,
- proposed size below min is capped or held,
- proposed size equal to current returns hold,
- accepted proposal passes through unchanged.

### 2.3 Evaluator tests

Verify:

- high active threads triggers scale up,
- high queue size triggers scale up,
- low active threads with empty queue triggers scale down,
- normal pressure holds,
- proposed size respects max,
- proposed size respects min,
- timestamp comes from input.

### 2.4 Decision tests

Verify:

- decision exposes all required fields,
- reason is non-blank,
- optional `ScaleDecision` conversion only exists for accepted/capped decisions if conversion is implemented.

### 2.5 Boundary tests

Scan policy package source for forbidden references:

- scenario package names,
- `ScenarioExperimentRunner`,
- `BaselineWorkloadExecutor`,
- `ExecutorAdapter`,
- `QueueCapacityController`,
- `MutationValidator`,
- `AdjustmentEvent`,
- `ThreadPoolExecutor`,
- `ScheduledExecutorService`.

## 3. Required Commands

After the future OpenSpec change implementation:

```powershell
openspec.cmd validate --all --json
.\mvnw.cmd test
git status --short
```

## 4. Acceptance Criteria

- Policy package compiles without new dependencies.
- All policy tests pass.
- Existing 93 tests continue to pass.
- No scenario or executor mutation dependency enters the policy package.
- OpenSpec validation passes.
- Worktree is clean before handoff.

## 5. Review Checklist

- Does evaluator call `Instant.now()`? If yes, reject.
- Does policy code mutate executor state? If yes, reject.
- Does policy code import scenario runner classes? If yes, reject.
- Are threshold formulas asserted in tests? If no, add tests.
- Are gate outcomes explicit? If no, add `GateStatus` or equivalent.
