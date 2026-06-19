# v0.10.0 Rejection Policy Runtime Replacement

## Header

- Version name: `v0.10.0`
- Authoring date: `2026-06-13`
- Status: `IMPLEMENTED`
- Current phase: `ARCHIVED`
- Requirement theme: runtime rejection-policy replacement, rebuild policy preservation, safety coverage

## Purpose

v0.10.0 addresses the last remaining static configuration dimension: **runtime rejection-policy replacement**. Unlike queue capacity resizing (v0.9.0), `ThreadPoolExecutor.setRejectedExecutionHandler()` is a public mutator — no executor rebuild is required. However, the current codebase has two gaps: `ManagedExecutor` stores the policy as `private final` with no setter, and `ExecutorRebuildStrategy` hardcodes `AbortPolicy()` during rebuild, silently discarding the original policy. v0.10.0 closes both gaps and delivers the final dynamic configuration capability.

## Scope Summary

| # | Change (candidate) | Scope |
|---|---|---|
| 1/? | `rejection-policy-command-and-adapter` | RejectionPolicyCommand, ManagedExecutor.setRejectionPolicy(), RejectionPolicyAdjustmentAdapter, RejectionPolicySafetyGate, PolicyReplacementEvidence |
| 2/? | `rejection-policy-end-to-end-verification` | End-to-end policy switch + scenario re-run, executor rebuild policy preservation, full safety coverage |

## Verification Target

- `mvn test`: all existing 476 tests pass (zero regression)
- New tests: command validation, safety gate evaluation, policy switch + scenario verification, rebuild policy preservation

## Key Decisions

See `decision-log.md`.

- D1: Direct setter vs. adapter-only mutation
- D2: Safety gate criteria for rejection policy replacement
- D3: Evidence recording for policy replacement
- D4: ExecutorRebuildStrategy policy preservation fix
- D5: Change decomposition strategy

## Predecessor

- v0.9.0 queue capacity resizing (IMPLEMENTED)
- v0.7.0 ManagedExecutor domain (IMPLEMENTED)

## Document Set

- `README.md`
- `00-objectives-and-scope.md`
- `decision-log.md`
- `10-ir.md` — requirements analysis (7 IR entries + 1 added during review)
- `11-ir-review.md` — independent IR review (7 findings)
- `12-ir-review-disposition.md` — disposition (3 FIX + 3 DEFER_TO_SR + 1 CLOSED)
- `13-ir-closure-verification.md` — IR closure verified
- `20-sr.md` — functional design (5 new components + 3 modifications)
- `21-sr-review.md` — independent SR review (5 findings, API spot-check 3/3 passed)
- `22-sr-review-disposition.md` — disposition (3 FIX + 1 ACCEPT + 1 DEFER)
- `23-sr-closure-verification.md` — SR closure verified
- `30-implementation-record.md` — Implementation record with spec scenario coverage and test results

## Final Status

- **534 tests, 0 failures, 0 errors, 0 skipped** — BUILD SUCCESS
- Both changes archived on 2026-06-13:
  - `rejection-policy-command-and-adapter` (5 new + 3 modified production files)
  - `rejection-policy-end-to-end-verification` (8 end-to-end scenarios)
- Delivered: runtime rejection-policy replacement via `ThreadPoolExecutor.setRejectedExecutionHandler()`, executor rebuild policy preservation, safety gate with resize-in-progress concurrency protection
