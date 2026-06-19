# Retrospective — `offline-replay-and-readiness-gate`

> Recommended (non-blocking) companion to `finalize.md`. Six sections,
> evidence first, opinion second. Recorded before the receipt is
> written so the post-authorization fixes are captured honestly.

## Wins

- **Spec / design / code alignment held.** The proposal, design, spec
  requirements, plan, implementation, apply notes, and verify notes
  all read consistently. Every spec scenario in
  `specs/offline-replay-and-readiness-gate/spec.md` is covered by at
  least one test (see the traceability table in `verify.md`).
- **Boundary isolation was cheap to enforce.** The
  `AnalysisBoundaryIsolationTest` test scans every Java file under
  `experiment/analysis` and fails the build on forbidden
  references. It is small, fast, and unambiguous; the change
  shipped without ever needing a fix-up to remove a forbidden
  import.
- **Post-authorization fix landed in-place.** Two semantic defects
  in the initial validator (incomplete pressure-field check,
  self-contradictory invalid counter) were caught and fixed without
  scope expansion, without new dependencies, and without
  disturbing any neighboring capability. The fix added 10 tests
  and 1 new helper, and bumped the total from 226 → 236 tests,
  all green.

## Misses

- **Initial `MISSING_PRESSURE_FIELDS` check only inspected two
  fields.** The design called for four
  (`activeThreads, poolSize, queueSize, completedTaskCount`); the
  first implementation only checked `activeThreads` and
  `queueSize`. Caught after the change was marked complete.
- **Initial invalid counter returned `accepted == rejected ==
  snapshots.size()`.** Self-contradictory and caught after the
  change was marked complete. The fix switched to a run-level
  blocking semantic and documented the rule in the class Javadoc.
- **`observed(...)` test helper left `completedTaskCount` absent.**
  Once the validator started checking all four fields, the helper
  itself became invalid. Fixed in the same pass that fixed the
  validator.

## Plan deviations

- **Contracts were partially pre-implemented.** When the change
  became `EXECUTION_AUTHORIZED`, the analysis package already
  contained 11 of the 17 final source files. The plan's Task 1
  was therefore a verification task rather than a greenfield
  implementation. Documented in `apply.md` under "Task 1 —
  Analysis Contracts (already present at authorization)".
- **`MISSING_RUN_ID` / `MISSING_SCENARIO_ID` /
  `MISSING_SCENARIO_PROFILE` failure codes are unreachable from
  the validator.** `ReplayRunInput`'s constructor rejects blank
  `runId` and `scenarioId` and null `scenarioProfile` before the
  validator is invoked. The codes remain in the enum to keep the
  surface area stable. The validator exercises every other
  failure code.
- **No worktree was used.** The implementation was done directly
  on the framework branch (`claude_master`) because the project's
  governance model treats `claude_master` as the authoritative
  branch and skips the feature-branch worktree pattern for
  bounded change implementations. The git-side closeout's
  worktree-based steps therefore did not apply; finalize follows
  the escape-hatch path documented in the finalize artifact
  instructions.
- **`ReplayReportWriter` always resolves to
  `outputs/reports/v0.4.0/`.** The plan's "controlled output
  directory" rule is enforced by construction; a test that tried
  to assert a custom subdir is rejected was relaxed to assert
  the canonical resolution target. Documented in `apply.md`.

## Skill / workflow compliance

- **Mandatory reading list was followed.** `docs/README.md`,
  `docs/00-project/current-state.md`, `docs/02-harness/...`,
  `docs/03-openspec/...`, and `docs/04-development/versions/README.md`
  were read at the start of the implementation. The current
  version design (`v0.4.0`) and the change's `proposal.md`,
  `design.md`, `spec.md`, `tasks.md`, and `plan.md` were
  consumed before any code was written.
- **No new Maven dependencies.** `pom.xml` is unchanged from the
  authorized state. The analysis layer uses only types already
  on the classpath.
- **Hard constraints respected.** `decisionTimestamp ==
  snapshotTimestamp` is enforced in the
  `ReplayDecisionEvidence` constructor. No `Instant.now()` in the
  analysis package. No `AdjustmentEvent`,
  `ThreadPoolExecutor`, `ScheduledExecutorService`,
  `ExecutorAdapter`, `QueueCapacityController`, or external IO
  references.
- **Tests run before checking tasks.** Each task's tests were
  written and confirmed failing before implementation, then
  re-run and confirmed passing. `tasks.md` checkboxes were only
  flipped after the corresponding tests were green.
- **Post-authorization fixes re-ran the full test suite and
  `openspec validate`.** Both passed.

## Surprises

- **`ReplayRunInput`'s constructor is stricter than the design
  implies.** The design lists `MISSING_RUN_ID` /
  `MISSING_SCENARIO_ID` / `MISSING_SCENARIO_PROFILE` as failure
  codes the validator can emit; the constructor blocks these
  values upstream. Either the design expected a more permissive
  constructor (and the validator would have been the only line
  of defense) or the design expected the validator to share the
  defensive responsibility. The current implementation chose
  defensive-by-construction; the codes are still in the enum.
- **`openspec validate` is fast and trustworthy.** The
  `offline-replay-and-readiness-gate` change artifact validates
  cleanly with no warnings; the same was true for the
  post-authorization fix.

## Promote candidates

- **Run-level blocking counter is a useful general pattern.**
  When a validator operates at run / collection scope (not
  per-element), the "accepted=0, rejected=N" pair is more
  honest than the per-element "accepted=N, rejected=N"
  contradiction. Worth promoting as a contract for any future
  collection-scope validator in the project.
- **`requiredPressureFields()` accessor.** The validator now
  exposes the canonical required-field list as a static helper
  so downstream readers (e.g. report writers) and future tests
  have a stable contract. The same pattern could be applied to
  any future validator that needs to publish a required-field
  list.
- **The boundary isolation test pattern.** The simple
  `Files.walk + String.contains` test is small, fast, and
  effective. It would be worth promoting into a shared harness
  helper so every new bounded change can opt in with one test
  method.
