# v0.12.0 Baseline Comparison Experiment Framework

## Header

- Version name: `v0.12.0`
- Authoring date: `2026-06-14`
- Status: `DRAFT`
- Current phase: `ARCHIVED` — v0.12.0 both changes archived (2026-06-14)
- Authoritative branch: `claude_master`
- Requirement theme: baseline catalog, comparable scenario runner, normalized result model, comparison report artifact

## Purpose

v0.12.0 delivers the baseline comparison experiment framework that enables the project to answer its core question with recorded evidence: under the same workload and safety constraints, when does the managed executor produce better stability, latency, rejection, recovery, or operability outcomes than common thread-pool baselines?

The three dynamic configuration dimensions (thread count, queue capacity, rejection policy) plus persistent evidence recording and live pressure sampling are now complete (v0.7.0–v0.11.0). The system can dynamically adjust and persist data for managed executors, and the `ScenarioExperimentRunner` can run workloads against a `BaselineWorkloadExecutor`. But there is no framework to run the SAME workload against MULTIPLE executor types, normalize metrics across them, and produce a side-by-side comparison artifact. v0.12.0 closes this gap.

## Scope Summary

| # | Change (candidate) | Scope |
|---|---|---|
| 1/2 | `baseline-catalog-and-comparison-runner` | BaselineExecutorCatalog (preset registry), CommonExecutorPreset, ComparableScenarioRunner, ComparisonResult, NormalizedComparisonMetrics |
| 2/2 | `comparison-report-and-end-to-end-verification` | ComparisonReportArtifact, ComparisonJsonWriter, AcquisitionReportPaths extension, end-to-end baseline-vs-managed comparison verification |

## Verification Target

- `mvn test`: all existing 646 tests pass (zero regression)
- New tests: baseline catalog registration/lookup, ComparableScenarioRunner dual-executor run, normalized result model round-trip, comparison report JSON serialization, ManagedExecutor rejection counting, end-to-end baseline-vs-managed comparison

## Key Decisions

See `decision-log.md`.

- D1: Executor baseline catalog scope and presets (6 defaults)
- D2: ComparableScenarioRunner design (sequential execution)
- D3: Normalized result model and metric mapping (9 metrics)
- D4: Comparison report artifact format (single JSON file)
- D5: Change decomposition strategy (2 changes)
- D6: ScenarioDefinition reuse (no new WorkloadDefinition)

## Predecessor

- v0.11.0 persistent evidence recording and live sampling (ARCHIVED) — durability and autonomous sampling
- v0.10.0 rejection policy replacement (ARCHIVED) — dynamic config baseline complete
- v0.7.0 ManagedExecutor domain (IMPLEMENTED) — real executor infrastructure
- v0.1.0 metrics-snapshot-and-recording (IMPLEMENTED) — in-memory foundation

## Document Set

- `README.md`
- `00-objectives-and-scope.md`
- `decision-log.md`
- `10-ir.md` — requirements analysis (9 IR entries, post-disposition)
- `11-ir-review.md` — independent IR review (8 findings: 3 P0, 3 P1, 2 P2)
- `12-ir-review-disposition.md` — disposition (6 FIX, 2 DEFER_TO_SR)
- `13-ir-closure-verification.md` — IR closure verified, all P0/P1 closed
- `20-sr.md` — SR functional design (11 component designs, post-disposition)
- `21-sr-review.md` — independent SR review (7 findings: 2 P0, 3 P1, 2 P2)
- `22-sr-review-disposition.md` — disposition (5 FIX, 2 DEFER)
- `23-sr-closure-verification.md` — SR closure verified, all P0/P1 closed

## OpenSpec Changes

- `openspec/changes/baseline-catalog-and-comparison-runner/` — change 1/2 (proposal, design, specs, tasks, plan)
- `openspec/changes/comparison-report-and-end-to-end-verification/` — change 2/2 (proposal, design, specs, tasks, plan)
