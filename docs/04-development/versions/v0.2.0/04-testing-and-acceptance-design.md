# v0.2.0 Testing and Acceptance Design

## Header

- Version name: `v0.2.0`
- Status: `EXECUTION_AUTHORIZED`
- Authorized change: `scenario-runner-and-baseline`

## 1. Test Strategy

The implementation must be validated mainly with deterministic unit tests. Avoid sleeping, timing assertions, or reliance on OS thread scheduling.

## 2. Required Test Groups

### 2.1 Scenario model tests

Verify:

- required fields,
- validation failures,
- equality or identity semantics if implemented,
- total work calculation.

### 2.2 Planner tests

Verify:

- same definition produces same plan,
- steady profile produces equal work units,
- ramp profile increases work units deterministically,
- burst profile produces deterministic spikes.

### 2.3 Baseline preset and executor tests

Verify:

- fixed configuration validation,
- no resizing after creation,
- completed work count is observable,
- no adaptive decision types are created.

### 2.4 Runner tests

Verify:

- run lifecycle reaches finalized state,
- completed step count equals planned step count,
- evidence count is greater than zero,
- recorded snapshots are associated with the run id,
- runner uses baseline policy id.

### 2.5 Boundary tests

Scan scenario package source for forbidden references:

- `ControlPolicy`,
- `ScaleDecision`,
- `AdjustmentEvent`,
- `.policy.`,
- `adaptive`,
- mutation adapter package names if introduced later.

## 3. Required Commands

After implementation:

```powershell
openspec.cmd validate --all --json
.\mvnw.cmd test
git status --short
```

## 4. Acceptance Criteria

- All `scenario-runner-and-baseline` tasks complete.
- All new specs have scenario-to-test coverage.
- No new dependencies are introduced.
- OpenSpec validation passes.
- Maven test suite passes.
- Worktree is clean before handoff.

## 5. Review Checklist for Downstream Agent

- Did the implementation add adaptive behavior? If yes, reject or split into a later change.
- Did any test rely on `Thread.sleep`? If yes, replace with deterministic control.
- Does `ScenarioRunOutcome` contain enough information for future comparison?
- Are evidence snapshots actually recorded, not just assembled?
- Is the baseline executor fixed and shut down safely if it owns threads?
