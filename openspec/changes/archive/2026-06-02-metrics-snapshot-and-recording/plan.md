# Metrics Snapshot and Recording Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development
> to implement this plan task-by-task.

**Goal:** Add a read-only observation layer that records normalized pressure snapshots and produces minimal evidence-derived summaries for experiment runs.

**Architecture:** The change introduces a small metrics package inside the existing experiment boundary. Manual sampling, snapshot assembly, append-only recording, and summary generation stay separate so later policy and executor changes can consume evidence without coupling to collection internals.

**Tech Stack:** Java 21, Spring Boot project structure, JUnit 5, Maven

---

## Task 1: Observation Contracts

- [ ] **Step 1:** Create `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/metrics/`.
- [ ] **Step 2:** Add interfaces or small final classes for `PressureSampler`, `SnapshotAssembler`, `EvidenceRecorder`, and `EvidenceSummaryBuilder`.
- [ ] **Step 3:** Add a value object for raw observation input if the existing `PressureSnapshot` cannot directly model unavailable metrics.
- [ ] **Step 4:** Add tests under `src/test/java/com/zhiwu/dynamicthreadpollermanager/experiment/metrics/` that compile against the contracts and document missing-value semantics.
- [ ] **Step 5:** Commit after the contracts and compilation tests pass.

## Task 2: Snapshot Collection and Normalization

- [ ] **Step 1:** Write a failing test for manually sampling a run with a controlled timestamp.
- [ ] **Step 2:** Implement the manual sampler so it delegates normalization to the snapshot assembler.
- [ ] **Step 3:** Write a failing test for mapping available executor-style values into a `PressureSnapshot`.
- [ ] **Step 4:** Implement the assembler mapping for available values.
- [ ] **Step 5:** Write a failing test for unavailable metric values.
- [ ] **Step 6:** Implement explicit missing-value handling without returning null snapshots.
- [ ] **Step 7:** Run the metrics package tests and commit the sampler and assembler.

## Task 3: Evidence Recording

- [ ] **Step 1:** Write a failing test that appends two snapshots for the same run and expects insertion order to be preserved.
- [ ] **Step 2:** Implement an in-memory append-only evidence recorder keyed by run identity.
- [ ] **Step 3:** Write a failing test that records snapshots for two runs and expects independent streams.
- [ ] **Step 4:** Implement per-run isolation and immutable read access.
- [ ] **Step 5:** Run the metrics package tests and commit the recorder.

## Task 4: Summary Generation

- [ ] **Step 1:** Write a failing test that summarizes a recorded run with multiple snapshots.
- [ ] **Step 2:** Implement sample count, first timestamp, and last timestamp summary fields.
- [ ] **Step 3:** Write a failing test for a run with no snapshots.
- [ ] **Step 4:** Implement zero-sample summary behavior without fabricated pressure values.
- [ ] **Step 5:** Run the metrics package tests and commit the summary builder.

## Task 5: Boundary and Verification

- [ ] **Step 1:** Add or update a boundary test confirming the metrics package does not depend on policy evaluation implementations or executor mutation adapters.
- [ ] **Step 2:** Run `.\mvnw.cmd test`.
- [ ] **Step 3:** Review `tasks.md` and mark completed tasks only after the corresponding tests pass.
- [ ] **Step 4:** Prepare `apply.md` with the worktree path, branch, commit range, task counts, and iteration value after implementation completes.
