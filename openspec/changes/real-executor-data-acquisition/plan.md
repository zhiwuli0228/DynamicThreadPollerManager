# real-executor-data-acquisition Plan

## Header

- Change identifier: `real-executor-data-acquisition`
- Plan purpose: implementation and verification plan

## 1. Implementation Order

1. `ManagedExecutorConfig` record + unit test
2. `SnapshotAssembler.fromExecutorState()` default method + unit test
3. `ManualPressureSampler.sampleFromExecutorState()` overload
4. `ManagedExecutorScenarioRunner` class
5. Runner integration tests (3 profiles, cleanup, exception path)

## 2. Verification Commands

- `mvn test -pl .` — full test suite
- `mvn test -Dtest=ManagedExecutorConfigTest` — config unit tests
- `mvn test -Dtest=ManagedExecutorScenarioRunnerTest` — runner integration tests
- `mvn test -Dtest=SnapshotAssemblerTest` — assembler unit tests

## 3. Scope Check

Before implementation:
- Verify `docs/00-project/current-state.md` says `EXECUTION_AUTHORIZED`
- Verify this change is listed as an active authorized change
- Verify `mvn test` passes with 412 tests before any changes

## 4. Autonomous Continuation Rule

After each task completes, proceed directly to the next task. Stop only for:
- Test failure that cannot be trivially fixed
- BLOCKED condition documented in task output
- Scope expansion beyond this change's boundary
