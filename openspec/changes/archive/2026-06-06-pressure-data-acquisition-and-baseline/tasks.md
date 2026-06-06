## 1. Acquisition Contracts

- [x] 1.1 Define the `RunManifest` shape for acquisition runs, including run identity, scenario inputs, baseline preset, environment summary, command line, and creation time.
- [x] 1.2 Define the `PressureSummary`, `ReplaySummary`, and `EvidenceIndex` shapes so every completed run can be traced back by `runId`.
- [x] 1.3 Define the versioned report output location and naming convention for v0.6.0 acquisition artifacts.

## 2. Data Quality and Readiness Rules

- [x] 2.1 Implement data quality validation rules for required profiles, repetition count, snapshot minimums, timestamp ordering, run identity consistency, and metadata completeness.
- [x] 2.2 Implement readiness classification rules that map valid datasets to `READY`, `READY_WITH_RISK`, or `NOT_READY`.
- [x] 2.3 Implement raw evidence hygiene rules so raw evidence is not versioned by default and any retained copy has an explicit retention record.

## 3. Test Coverage and Acceptance

- [x] 3.1 Add tests that prove valid acquisition runs produce all required manifest and summary artifacts.
- [x] 3.2 Add tests that prove invalid datasets are rejected when required profiles or quality gates are missing.
- [x] 3.3 Add tests that prove readiness outputs stay bounded and do not imply runtime mutation authorization.
- [x] 3.4 Add verification steps for change scope, current-state alignment, and report hygiene before implementation handoff.
