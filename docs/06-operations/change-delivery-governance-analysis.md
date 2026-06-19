# Change Delivery Governance Analysis

## Purpose

This document captures why the `experiment-foundation` delivery produced an inconsistent state and defines the minimum governance actions required to prevent the same failure mode from recurring.

It is an operational note, not a code design document.

## 1. Failure Analysis

### 1.1 Authority state was not synchronized after archive

The repository ended up with a change archive, implementation code, and updated version design materials, but `docs/00-project/current-state.md` still described the repository as if it had not left documentation-framework construction.

Root cause:

- The archive result did not trigger a mandatory state-sync step.
- The current-state file was treated as informational instead of authoritative.
- The version design package and OpenSpec archive were updated independently.

Impact:

- Later agents can read conflicting authority signals.
- A change can be archived while the authoritative state still says no implementation is authorized.

### 1.2 Requirement wording and implementation semantics diverged

The design and verification text used the phrase "deterministic identity", but the implementation generated `runId` with a random UUID.

Root cause:

- The requirement used a stronger word than the implementation actually needed.
- The intended behavior was likely "traceable experiment identity", not literal deterministic identity.
- No explicit semantic review aligned the requirement wording with the runtime model before implementation started.

Impact:

- The implementation can pass tests while still violating the literal requirement wording.
- Later changes may build on a misleading semantic foundation.

### 1.3 Tests validated execution shape instead of requirement intent

The tests proved the coordinator could move through states and create summary objects, but they did not prove the identity semantics described by the requirement.

Root cause:

- Test names were closer to the requirement than the assertions were.
- The assertion set was too weak to distinguish "unique identity" from "deterministic identity".
- The verification report accepted the weaker proof as sufficient.

Impact:

- A false sense of requirement coverage was recorded.
- The implementation can appear complete while key semantics remain unresolved.

### 1.4 Summary generation was treated as complete too early

`generateSummary()` returned metadata and a fixed outcome, but it did not yet summarize actual experimental evidence.

Root cause:

- The first change intentionally stayed small.
- The verification path did not mark the summary as a placeholder capability.
- The evolution path for evidence-backed summary generation was not called out clearly enough.

Impact:

- Future metrics and scenario changes may assume summary semantics that do not yet exist.
- The foundation looks more complete than it really is.

## 2. Recurrence Prevention Principles

### 2.1 State synchronization is mandatory

Every archive or finalize event MUST be followed by a synchronized update of the authoritative current-state document.

### 2.2 Requirement language must match implementation intent

If a requirement says "deterministic", the implementation and tests must prove determinism. If the intent is only traceability, the requirement MUST say traceable instead.

### 2.3 Tests must prove the requirement, not the shape

Tests MUST be written against the semantic claim in the spec, not only against object creation or lifecycle shape.

### 2.4 Placeholder behavior must be explicit

If a method returns fixed or placeholder values, the design and verification notes MUST state that it is a baseline placeholder and not a completed analytic implementation.

### 2.5 Archive evidence and authority state must agree

The archived change record, version design package, and current-state authority record MUST tell the same story about what is allowed, what is implemented, and what remains pending.

## 3. Minimum Governance Checklist

Use this checklist for every future change:

### Before implementation

- Confirm the version design status in `docs/04-development/versions/<version>/`.
- Confirm the change is authorized by the current version design state.
- Confirm the requirement wording matches the intended runtime semantics.
- Confirm the change boundary is small enough for the current implementation model.

### During implementation

- Keep the implementation within the approved capability boundary.
- Keep tests aligned to requirement intent, not just code shape.
- Mark placeholder behavior explicitly in code comments or verification notes if the behavior is not yet final.
- Avoid expanding the scope without updating the proposal/design first.

### Before archive

- Verify that the implementation and verification evidence match the spec wording.
- Verify that the authoritative current-state document is updated.
- Verify that the version design package reflects the actual outcome of the change.
- Verify that archive receipts do not claim stronger semantics than the implementation provides.

### After archive

- Synchronize the current-state document.
- Synchronize any long-lived architecture facts if the change changed them.
- Record any semantic gaps that remain open for the next change.
- Do not start the next change until the handoff story is consistent.

## 4. Recommended Enforcement Rule

For future work, treat the following as a hard gate:

1. No archive without a current-state sync.
2. No requirement word like "deterministic" unless tests prove it.
3. No verification report may claim full semantic coverage if the assertions are weaker than the requirement.
4. No placeholder summary may be described as full analysis.

## 5. Outcome

The first change was useful as a foundation, but the delivery process exposed a governance gap:

- authority records were not kept in sync,
- requirement wording drifted from implementation intent,
- tests proved lifecycle shape more than semantic correctness.

The next changes should use the checklist above as a release gate.
