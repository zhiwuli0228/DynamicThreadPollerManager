# v0.6.0 补救采集执行任务

## 00 元信息

- Package name: `v0.6.0-remediation`
- Task name: `real-data-remediation-campaign`
- Authoring date: `2026-06-07`
- Current stage reference: `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
- Current authorized work type reference: `NONE`
- Authoritative branch: `claude_master`
- Source of truth: `docs/00-project/current-state.md`
- Related design package: `docs/04-development/versions/v0.6.0/remediation/`
- Related output root: `outputs/reports/v0.6.0-remediation/`
- Current conclusion: this is a dispatch-ready task definition, not execution authorization

## 01 Task Intent

This task is for a worker agent that will collect real experimental data for the v0.6.0 remediation campaign and materialize the evidence on disk.

The task does not revise the original `v0.6.0` narrative. It exists to close the evidence gap identified by the remediation package.

## 02 Dispatch Gate

Before any execution, the worker agent MUST confirm the current authorization state from `docs/00-project/current-state.md`.

If the current stage is still `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED` with `Current authorized work type: NONE`, the worker agent MUST stop and report a blocked authorization state.

This task becomes executable only after an external authorization update explicitly allows the remediation campaign.

## 03 Scope

### In scope

- Collect real experimental data for the remediation campaign.
- Use the remediation package output contract.
- Produce run manifests, pressure summaries, readiness summaries, evidence indexes, campaign report, and raw snapshot evidence.
- Validate the dataset against the remediation data-quality gates.

### Out of scope

- Modifying the original `v0.6.0` IR / SR / plan documents.
- Creating a new OpenSpec change.
- Java source changes or test changes.
- Queue resizing.
- Production `ThreadPoolExecutor` integration.
- Closed-loop controller work.
- New dependencies.

## 04 Required Inputs

The worker agent MUST use the remediation package as the execution reference:

- [preflight-note.md](./preflight-note.md)
- [00-objectives-and-scope.md](./00-objectives-and-scope.md)
- [10-ir.md](./10-ir.md)
- [20-sr.md](./20-sr.md)
- [23-sr-closure-verification.md](./23-sr-closure-verification.md)
- [decision-log.md](./decision-log.md)

The worker agent MUST also inspect the current-state authority document before any execution.

## 05 Required Output Set

All outputs MUST be written under:

```text
outputs/reports/v0.6.0-remediation/
```

Required artifacts:

- `run-manifest-<batch>.json`
- `pressure-summary-<batch>.json`
- `readiness-summary-<batch>.md`
- `evidence-index-<batch>.json`
- `campaign-report-<batch>.md`
- `raw-snapshots-<runId>.jsonl`

The worker agent MUST use one consistent batch identifier across all versioned outputs.

## 06 Execution Requirements

- Cover the three required profiles: `STEADY`, `RAMP`, and `BURST`.
- Collect at least 3 valid runs per profile.
- Record fixed seed, scenario, step count, baseline preset, command line, and environment summary for each run.
- Ensure each run has at least 3 snapshots.
- Keep raw evidence append-only and traceable to the corresponding run.
- Preserve traceability from manifest to summary to raw snapshots to readiness result.

## 07 Data Quality Gates

The worker agent MUST treat these gates as blocking:

- All three profiles exist.
- Each profile has at least 3 valid runs.
- Each run has at least 3 snapshots.
- Snapshot timestamps are non-decreasing.
- All snapshots in a run share the same `runId`.
- Run metadata is complete.
- Evidence index can be traced back to each run.
- Raw evidence is not treated as versioned deliverable by default.

If any blocking gate fails, the worker agent MUST stop and report the exact failure.

## 08 Output Semantics

- `RunManifest` records the input signature and environment fingerprint.
- `PressureSummary` records pressure observations for each run.
- `ReadinessSummary` records a bounded verdict only.
- `EvidenceIndex` maps each run to its produced artifacts.
- `CampaignReport` summarizes the remediation campaign and handoff state.

`ReadinessSummary` MUST NOT imply runtime mutation authorization.

## 09 Stop Conditions

The worker agent MUST stop if any of the following occurs:

- Current authorization is still `NONE`.
- The worker would need to change code, tests, dependencies, or scope.
- The required output directory cannot be created or written.
- The available execution entry point cannot produce real raw snapshot evidence.
- Any blocking data-quality gate fails and cannot be corrected without code changes.
- The task is drifting back into the original v0.6.0 mainline instead of the remediation path.

## 10 Handoff Note

Use the following as the worker-facing handoff:

> Execute the `v0.6.0-remediation` real-data campaign only after confirming the repository authorization state. If current-state still says `Current authorized work type: NONE`, stop and report blocked authorization. If execution is authorized, collect real pressure data for `STEADY`, `RAMP`, and `BURST`, with at least 3 valid runs per profile and at least 3 snapshots per run. Write all outputs under `outputs/reports/v0.6.0-remediation/` using one consistent batch identifier, and produce `run-manifest`, `pressure-summary`, `readiness-summary`, `evidence-index`, `campaign-report`, and `raw-snapshots` artifacts. Do not change code, tests, dependencies, or scope. Stop immediately on any blocking data-quality gate or if real raw evidence cannot be produced.
