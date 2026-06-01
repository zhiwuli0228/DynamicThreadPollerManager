### Requirement: Runtime pressure snapshot collection
The system MUST collect timestamped runtime pressure snapshots for an experiment run without evaluating policy decisions or mutating executor state.

#### Scenario: Capture a snapshot for a run
- **WHEN** an experiment run requests a pressure sample
- **THEN** the system MUST create a snapshot associated with that run and include the sample timestamp

#### Scenario: Preserve observation-only behavior
- **WHEN** the metrics layer captures runtime state
- **THEN** it MUST NOT create scale decisions, adjustment events, or executor mutations

---

### Requirement: Snapshot normalization
The system MUST normalize available runtime and executor observations into a stable pressure snapshot representation that later capabilities can consume.

#### Scenario: Normalize available executor state
- **WHEN** active count, pool size, queue size, or completed task count are available
- **THEN** the system MUST copy those values into the normalized snapshot without requiring callers to inspect executor internals

#### Scenario: Handle unavailable metrics
- **WHEN** a runtime metric is not safely available
- **THEN** the system MUST still produce a valid snapshot and represent the missing value explicitly rather than failing silently

---

### Requirement: Append-only evidence recording
The system MUST record pressure snapshots in append-only chronological order for each experiment run.

#### Scenario: Append multiple snapshots
- **WHEN** multiple snapshots are recorded for the same run
- **THEN** the result series MUST preserve their insertion order and expose the full recorded sequence

#### Scenario: Separate evidence by run
- **WHEN** snapshots from different experiment runs are recorded
- **THEN** the system MUST keep each run's evidence stream independently addressable

---

### Requirement: Observation-derived summary generation
The system MUST generate a minimal summary from recorded snapshots, including sample count and time bounds.

#### Scenario: Summarize a recorded run
- **WHEN** a run has one or more recorded snapshots
- **THEN** the summary MUST include the number of samples and the first and last sample timestamps

#### Scenario: Summarize a run with no snapshots
- **WHEN** a run has no recorded snapshots
- **THEN** the summary MUST report zero samples without inventing pressure values

---

### Requirement: Deterministic sampling for tests
The system MUST provide a deterministic sampling path that can be exercised without depending on wall-clock scheduling races.

#### Scenario: Manually trigger sampling
- **WHEN** a test triggers sampling with controlled inputs
- **THEN** the system MUST produce a predictable snapshot and recorder state

#### Scenario: Verify recorder ordering
- **WHEN** a test records snapshots with controlled timestamps
- **THEN** the system MUST expose the snapshots in the same order they were appended
