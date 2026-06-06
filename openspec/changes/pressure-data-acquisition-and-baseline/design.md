# pressure-data-acquisition-and-baseline Design

## Header

- Change identifier: `pressure-data-acquisition-and-baseline`
- Design purpose: define a bounded, reproducible pressure data acquisition capability on top of existing baseline, metrics, policy, and analysis artifacts
- Authoritative inputs:
  - `docs/04-development/versions/v0.6.0/10-ir.md`
  - `docs/04-development/versions/v0.6.0/11-ir-review.md`
  - `docs/04-development/versions/v0.6.0/12-ir-review-disposition.md`
  - `docs/04-development/versions/v0.6.0/13-ir-closure-verification.md`
  - `docs/04-development/versions/v0.6.0/20-sr.md`
  - `docs/04-development/versions/v0.6.0/21-sr-review.md`
  - `docs/04-development/versions/v0.6.0/22-sr-review-disposition.md`
  - `docs/04-development/versions/v0.6.0/23-sr-closure-verification.md`
  - `docs/04-development/versions/v0.6.0/15-experiment-data-acquisition-plan.md`
  - `docs/04-development/versions/v0.6.0/decision-log.md`
  - `docs/00-project/current-state.md`

## 1. Scope

This change defines a controlled acquisition layer that collects baseline pressure evidence and produces stable report artifacts for later review. It does not implement executor mutation, queue resizing, or production executor integration.

In scope:

- acquisition manifest generation
- baseline pressure summary generation
- replay summary generation
- readiness summary generation
- evidence indexing
- report hygiene rules
- data quality validation rules

Out of scope:

- Java runtime mutation behavior
- queue resizing
- closed-loop scheduler/controller
- production `ThreadPoolExecutor` integration
- REST/API/UI
- persistence layer
- external dependency introduction
- throughput claims or optimization claims

## 2. Implementation Order

1. Define the acquisition manifest and evidence index contracts.
2. Define the report outputs and directory layout.
3. Define validation rules for required profiles, repetition, timestamps, and metadata completeness.
4. Define replay/readiness summary semantics.
5. Define verification scenarios and acceptance checks.
6. Keep all scope within the approved baseline pressure acquisition boundary.

## 3. Verification Requirements

- Verify that the change remains inside the approved pressure data acquisition boundary.
- Verify that required profiles exist for `STEADY`, `RAMP`, and `BURST`.
- Verify that manifest, summaries, and evidence index are all traceable by `runId`.
- Verify that raw evidence is not treated as a versioned default artifact.
- Verify that readiness output never implies runtime mutation authorization.

## 4. Evidence Requirements

- Preserve a stable mapping from run input to artifact output.
- Capture environment fingerprint data in the manifest.
- Record summary outputs in the report directory.
- Record residual risk if data is insufficient for downstream readiness.
- Do not record stronger semantic claims than the acquisition boundary supports.

## 5. Closeout Steps

- Produce the proposal, spec, tasks, and plan artifacts for the change.
- Keep the current-state record synchronized with the active authorization story.
- Ensure future implementation agents have an exact non-scope boundary.
- Do not authorize implementation until the change is fully decomposed and reviewed.

## 6. Delivery Checklist

- Use `docs/07-templates/change-delivery-checklist-template.md` as the required closeout checklist for this change.
- Do not mark the change as ready for archive until the checklist is complete.
- Ensure the archive receipt, verification report, and current-state document tell the same story before closeout.
