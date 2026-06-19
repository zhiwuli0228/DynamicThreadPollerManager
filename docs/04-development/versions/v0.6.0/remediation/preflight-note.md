# v0.6.0-remediation Preflight Note

## Purpose

This note is the first thing a worker agent should read before attempting any remediation execution.

Its job is to answer one question:

> Is this repository snapshot authorized for remediation execution?

If the answer is no, stop here.

## Authorization Snapshot

Use this embedded snapshot as the authority check for this task package:

- Authoritative branch: `claude_master`
- Current stage: `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
- Current authorized work type: `NONE`
- Source of truth: `docs/00-project/current-state.md`

## Decision Rule

- If the worker can read `docs/00-project/current-state.md` and it matches the snapshot above, the worker MUST stop and report `BLOCKED: unauthorized`.
- If the worker cannot read `docs/00-project/current-state.md`, the worker MUST still treat the embedded snapshot above as the authority check for this package and stop with `BLOCKED: unauthorized`.
- The worker MUST NOT infer authorization from the presence of remediation design files alone.

## Remediation Package Inventory

The remediation package is expected to include:

- `README.md`
- `00-objectives-and-scope.md`
- `10-ir.md`
- `11-ir-review.md`
- `12-ir-review-disposition.md`
- `13-ir-closure-verification.md`
- `20-sr.md`
- `21-sr-review.md`
- `22-sr-review-disposition.md`
- `23-sr-closure-verification.md`
- `30-execution-task.md`
- `60-retrospective.md`
- `61-rectification-report.md`
- `decision-log.md`

If any of these files are missing in the worker's workspace, that is not an execution signal.
It is a workspace synchronization issue.

## What the Worker Should Do Next

1. Verify the authorization snapshot.
2. Stop if authorization is `NONE`.
3. If authorization is later updated, re-read `30-execution-task.md` and proceed only within the remediation scope.

## What the Worker Should Not Do

- Do not start collecting data.
- Do not create output directories.
- Do not fabricate missing evidence.
- Do not expand into mainline v0.6.0 execution.
- Do not treat missing files as permission to improvise new scope.

