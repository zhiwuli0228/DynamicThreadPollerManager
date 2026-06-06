# pressure-data-acquisition-and-baseline Implementation Plan

> **For agentic workers:** Use a task-by-task approach and stop only at the next commit point when the current task is fully verified.

**Goal:** Implement a bounded pressure data acquisition capability that produces reproducible manifests, summaries, readiness outputs, and evidence indexes without crossing into runtime mutation or queue resizing.

**Architecture:** Add a new `experiment.acquisition` boundary that orchestrates existing `scenario`, `metrics`, `policy`, and `analysis` capabilities. The acquisition layer should only coordinate data capture and reporting; it must not own executor mutation, queue resizing, or production integration. Outputs are written under `outputs/reports/v0.6.0/` so acquisition evidence remains version-scoped and traceable.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, existing `com.zhiwu.dynamicthreadpollermanager.experiment.*` packages, OpenSpec `superspec`.

---

## Execution Rules

- Keep scope inside `pressure-data-acquisition-and-baseline` only.
- Re-read `docs/00-project/current-state.md`, `design.md`, `specs/pressure-data-acquisition-and-baseline/spec.md`, and `tasks.md` before implementation starts.
- After each task group, run the relevant tests and `git status --short` before committing.
- If a task exposes missing scope or a semantic mismatch, stop and fix the documents first instead of widening implementation scope.
- Continue to the next task only after the current task group is committed and the worktree state is understood.

## Task 1: Acquisition Contracts

- [ ] **Step 1:** Create `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/acquisition/RunManifest.java` as the canonical acquisition manifest model with run identity, scenario inputs, baseline preset, environment summary, command line, and creation time.
- [ ] **Step 2:** Create `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/acquisition/PressureSummary.java`, `ReplaySummary.java`, and `EvidenceIndex.java` as traceable run-level report models.
- [ ] **Step 3:** Add `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/acquisition/AcquisitionReportPaths.java` or equivalent to centralize `outputs/reports/v0.6.0/` naming and path rules.
- [ ] **Step 4:** Add focused unit tests under `src/test/java/com/zhiwu/dynamicthreadpollermanager/experiment/acquisition/` for required fields and traceability.
- [ ] **Step 5:** Run `.\mvnw.cmd test` and `git status --short`, then commit the contract layer before moving on.

## Task 2: Data Quality and Readiness Rules

- [ ] **Step 1:** Implement `AcquisitionDataQualityValidator` under `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/acquisition/` to enforce required profiles, repetition count, snapshot minimums, timestamp order, run identity consistency, and metadata completeness.
- [ ] **Step 2:** Implement `AcquisitionReadinessClassifier` or equivalent so valid datasets map only to `READY`, `READY_WITH_RISK`, or `NOT_READY`.
- [ ] **Step 3:** Encode raw evidence hygiene so raw evidence is not versioned by default and any retained copy must carry an explicit retention record.
- [ ] **Step 4:** Add tests covering the STEADY/RAMP/BURST gate, missing profile rejection, insufficient sample rejection, and readiness classification boundaries.
- [ ] **Step 5:** Run `.\mvnw.cmd test`, verify no new scope drift, and commit the validator and classifier work before continuing.

## Task 3: Report Outputs and Acceptance

- [ ] **Step 1:** Implement the report writer or orchestration service that emits the manifest, summaries, and evidence index into `outputs/reports/v0.6.0/`.
- [ ] **Step 2:** Wire the acquisition boundary to reuse existing `experiment.scenario`, `experiment.metrics`, `experiment.policy`, and `experiment.analysis` building blocks without introducing runtime mutation.
- [ ] **Step 3:** Add integration-style tests that prove a valid acquisition run produces all required outputs and an invalid dataset is blocked before downstream replay/readiness acceptance.
- [ ] **Step 4:** Add verification checks for scope alignment, `docs/00-project/current-state.md` alignment, and report hygiene before handoff.
- [ ] **Step 5:** Run `openspec validate --all --json`, `.\mvnw.cmd test`, and `git status --short`, then prepare the implementation record and finalize commit.
