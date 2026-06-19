# acquisition-paths-and-quality-gates Plan

## Header

- Change identifier: `acquisition-paths-and-quality-gates`
- Plan purpose: implementation and verification plan

## 1. Implementation Order

1. `AcquisitionReportPaths.forVersion()` + dual-arg `AcquisitionReportWriter` constructor
2. `RunSnapshot` extension (extendedFieldPresence, threadLeakFree)
3. `AcquisitionDataQualityValidator` G7-G9 gates
4. `AcquisitionReportBridge` class
5. 9-run data acquisition test

## 2. Verification Commands

- `mvn test -pl .` — full test suite
- `mvn test -Dtest=AcquisitionReportPathsTest` — paths tests
- `mvn test -Dtest=AcquisitionDataQualityValidatorTest` — gate tests
- `mvn test -Dtest=AcquisitionReportBridgeTest` — bridge tests
- `mvn test -Dtest=FullDataAcquisitionTest` — 9-run end-to-end test

## 3. Dependency Check

Before implementation:
- Verify `real-executor-data-acquisition` change is implemented (blocking dependency).
- Verify `ManagedExecutorConfig`, `ManagedExecutorScenarioRunner`, `SnapshotAssembler.fromExecutorState()` are available on classpath.

## 4. Scope Check

Before implementation:
- Verify `docs/00-project/current-state.md` says `EXECUTION_AUTHORIZED`
- Verify this change is listed as an active authorized change

## 5. Autonomous Continuation Rule

After each task completes, proceed directly to the next task. Stop only for:
- Test failure that cannot be trivially fixed
- BLOCKED condition documented in task output
- Scope expansion beyond this change's boundary
