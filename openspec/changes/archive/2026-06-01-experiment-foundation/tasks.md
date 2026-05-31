## 1. Foundation Model

- [x] 1.1 Define the minimal experiment runtime package structure for the shared contracts and lifecycle coordinator.
- [x] 1.2 Add immutable model objects for `ExperimentRun`, `LoadScenario`, `PressureSnapshot`, `ControlPolicy`, `ScaleDecision`, `AdjustmentEvent`, `ResultSeries`, and `AnalysisSummary`.
- [x] 1.3 Add a small lifecycle state model that can represent created, running, stopped, and finalized states.

## 2. Runtime Coordination

- [x] 2.1 Implement a minimal experiment coordinator that creates and tracks runs by scenario and policy identity.
- [x] 2.2 Implement start, stop, and finalize transitions without adding sampling or mutation responsibilities.
- [x] 2.3 Add summary generation for the foundation run metadata.

## 3. Verification and Boundary Checks

- [x] 3.1 Add unit tests that validate lifecycle transitions and deterministic run identity behavior.
- [x] 3.2 Add unit tests that ensure the foundation objects remain decoupled from sampling and executor mutation.
- [x] 3.3 Verify the new package boundary does not require ADR or architecture updates before later change work.
