## 1. Observation Contracts

- [x] 1.1 Define the metrics snapshot package structure under the experiment boundary.
- [x] 1.2 Add small contracts for pressure sampling, snapshot assembly, evidence recording, and summary building.
- [x] 1.3 Define how missing or unavailable metric values are represented without throwing away the snapshot.

## 2. Snapshot Collection and Normalization

- [x] 2.1 Implement a deterministic manual sampler that can capture a snapshot for a supplied run and clock input.
- [x] 2.2 Implement a snapshot assembler that maps available JVM and executor observations into `PressureSnapshot`.
- [x] 2.3 Add unit tests for timestamping, run association, available metric mapping, and unavailable metric handling.

## 3. Evidence Recording

- [x] 3.1 Implement an append-only recorder that stores snapshots by experiment run identity.
- [x] 3.2 Ensure the recorder preserves insertion order and keeps different runs isolated.
- [x] 3.3 Add unit tests for append behavior, run separation, and immutable read access.

## 4. Summary Generation

- [x] 4.1 Implement a summary builder that derives sample count and time bounds from recorded snapshots.
- [x] 4.2 Add summary behavior for empty evidence streams without fabricated metric values.
- [x] 4.3 Add unit tests for populated and empty summary generation.

## 5. Boundary and Verification

- [x] 5.1 Verify the metrics layer has no dependency on adaptive policy evaluation or executor mutation logic.
- [x] 5.2 Run targeted tests for the new metrics snapshot and recording package.
- [x] 5.3 Run the full Maven test suite before closing the change.
