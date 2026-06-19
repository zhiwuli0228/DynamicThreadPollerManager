# v0.11.0 Persistent Metrics Recording and Live Sampling

## Header

- Version name: `v0.11.0`
- Authoring date: `2026-06-13`
- Status: `DRAFT`
- Current phase: `CHANGE_DECOMPOSITION_COMPLETE` — 2 OpenSpec changes created, ready for EXECUTION_AUTHORIZED
- Requirement theme: persistent evidence recording, snapshot serialization, recording session lifecycle, live pressure sampling

## Purpose

v0.11.0 extends the v0.1.0 metrics-snapshot-and-recording foundation from in-memory-only to persistent, autonomous data collection. The three dynamic configuration dimensions (thread count, queue capacity, rejection policy) are now complete. The next gap is durability: `InMemoryEvidenceRecorder` cannot survive JVM restarts, `PressureSnapshot` has no serialization format, no `PressureSampler` can autonomously poll a live executor, and there is no recording session lifecycle. v0.11.0 closes these gaps.

## Scope Summary

| # | Change (candidate) | Scope |
|---|---|---|
| 1/2 | `persistent-evidence-recorder` | FileBackedEvidenceRecorder, ObservedSnapshot JSON serialization, RecordingSession lifecycle, RecordingSessionMetadata |
| 2/2 | `live-pressure-sampler-and-integration` | LivePressureSampler (scheduled polling), ManagedExecutorScenarioRunner integration, end-to-end persistent recording verification |

## Verification Target

- `mvn test`: all existing 535 tests pass (zero regression)
- New tests: JSON round-trip, file-backed recorder append/read, recording session lifecycle, live sampler scheduling and shutdown

## Key Decisions

See `decision-log.md`.

- D1: Serialization format (JSON via AcquisitionJsonWriter vs. new format)
- D2: File layout and naming conventions
- D3: LivePressureSampler scheduling model
- D4: RecordingSession lifecycle integration with EvidenceRecorder
- D5: Change decomposition strategy

## Predecessor

- v0.1.0 metrics-snapshot-and-recording (IMPLEMENTED) — in-memory foundation
- v0.10.0 rejection policy replacement (IMPLEMENTED) — completed dynamic config baseline

## Document Set

- `README.md`
- `00-objectives-and-scope.md`
- `decision-log.md`
- `10-ir.md` — requirements analysis (6 IR entries)
- `11-ir-review.md` — independent IR review (7 findings: 2 P0, 3 P1, 2 P2)
- `12-ir-review-disposition.md` — disposition (4 FIX, 2 DEFER_TO_SR, 1 CLOSED)
- `13-ir-closure-verification.md` — IR closure verified
- `20-sr.md` — SR functional design (10 component designs, SR reviewed)
- `21-sr-review.md` — independent SR review (7 findings: 2 P0, 3 P1, 2 P2)
- `22-sr-review-disposition.md` — disposition (5 FIX, 2 DEFER_TO_IMPLEMENTATION)
- `23-sr-closure-verification.md` — SR closure verified

## OpenSpec Changes

- `openspec/changes/persistent-evidence-recorder/` — change 1/2 (proposal, design, specs, tasks)
- `openspec/changes/live-pressure-sampler-and-integration/` — change 2/2 (proposal, design, specs, tasks)
