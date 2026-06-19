# Pressure Data Acquisition and Baseline Specification

## Purpose

The pressure data acquisition and baseline capability defines a controlled acquisition layer that collects baseline pressure evidence and produces stable report artifacts for later review. It creates a `RunManifest` for every completed acquisition run recording run identity, scenario inputs, baseline preset, environment summary, command line, and creation time so that runs can be reproduced and audited. It produces `PressureSummary`, `ReplaySummary`, and `EvidenceIndex` artifacts traceable by `runId` under the versioned report output path `outputs/reports/v0.6.0/`. It enforces data quality gates requiring the `STEADY`, `RAMP`, and `BURST` profiles with at least three valid runs per profile, at least three snapshots per run, non-decreasing snapshot timestamps, consistent `runId` values, and complete run metadata. It classifies acquisition datasets as `READY`, `READY_WITH_RISK`, or `NOT_READY` with bounded `recommendedNextStep` values that never imply runtime mutation authorization. Raw evidence defaults to non-versioned storage; if retained, the retention location, responsible owner, and cleanup plan must be explicitly recorded. The capability does not implement executor mutation, queue resizing, production `ThreadPoolExecutor` integration, closed-loop scheduler/controller, persistence, REST/API/UI, or external dependencies.

## Requirements

### Requirement: Acquisition run manifest
The system MUST create a `RunManifest` for every completed pressure data acquisition run. The manifest MUST record `runId`, `scenarioId`, `scenarioProfile`, `seed`, `stepCount`, `baselinePolicyId`, `baselineExecutorPreset` details, `environmentSummary`, `commandLine`, and `createdAt` so that the run can be reproduced and audited.

#### Scenario: Manifest is written for a valid run
- **WHEN** a pressure data acquisition run completes successfully with a valid scenario profile and baseline preset
- **THEN** the system writes a `RunManifest` containing the required run identity, baseline, environment, and invocation fields

---

### Requirement: Pressure and replay summaries
The system MUST produce a `PressureSummary`, `ReplaySummary`, and `EvidenceIndex` for each completed acquisition run. These artifacts MUST be written to the versioned report output path for v0.6.0, and the output MUST preserve traceability from `runId` to the generated artifacts.

#### Scenario: Summaries and evidence index are emitted
- **WHEN** a valid acquisition run has finished and the collected evidence passes the minimum data checks
- **THEN** the system writes the pressure summary, replay summary, and evidence index under `outputs/reports/v0.6.0/` and keeps them traceable by `runId`

---

### Requirement: Data quality gates
The system MUST reject acquisition evidence that does not satisfy the required data quality gates. The gates MUST require the `STEADY`, `RAMP`, and `BURST` profiles, require at least three valid runs per profile, require at least three snapshots per run, require non-decreasing snapshot timestamps, require consistent `runId` values within a run, and require complete run metadata for scenario, seed, preset, and environment.

#### Scenario: Missing required profile is blocked
- **WHEN** the acquisition dataset is missing any required profile, including `STEADY`, `RAMP`, or `BURST`
- **THEN** the system rejects the dataset and reports that the data is not ready for downstream replay or readiness review

---

### Requirement: Readiness classification and raw evidence hygiene
The system MUST produce a `ReadinessSummary` that classifies the dataset as `READY`, `READY_WITH_RISK`, or `NOT_READY` and MUST provide a `recommendedNextStep` consistent with that classification. The system MUST NOT imply runtime mutation authorization in the readiness output. Raw evidence MUST default to non-versioned storage; if raw evidence is retained, the retention location and cleanup responsibility MUST be explicitly recorded.

#### Scenario: Readiness summary stays bounded
- **WHEN** the system evaluates a completed acquisition dataset
- **THEN** it emits a readiness summary with a bounded classification, a clear next step, and no implicit runtime mutation authorization, while keeping raw evidence out of version control by default
