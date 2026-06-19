# Verify Notes — `offline-replay-and-readiness-gate`

## Build

- `.\mvnw.cmd test` — `BUILD SUCCESS`, 236 tests, 0 failures, 0 errors,
  0 skipped. (See per-suite breakdown below.)
- `openspec.cmd validate --all --json` — `passed: 4, failed: 0`. The
  `offline-replay-and-readiness-gate` change artifact is valid along
  with all archived capability specs.

## `mvn test` per-suite breakdown

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| `DynamicThreadPollerManagerApplicationTests` | 1 | 0 | 0 | 0 |
| `experiment.analysis.AnalysisBoundaryIsolationTest` | 2 | 0 | 0 | 0 |
| `experiment.analysis.AnalysisContractsTest` | 10 | 0 | 0 | 0 |
| `experiment.analysis.MutationReadinessGateTest` | 10 | 0 | 0 | 0 |
| `experiment.analysis.OfflinePolicyReplayServiceTest` | 12 | 0 | 0 | 0 |
| `experiment.analysis.ReplayEvidenceValidatorTest` | 19 | 0 | 0 | 0 |
| `experiment.analysis.ReplayReportWriterTest` | 11 | 0 | 0 | 0 |
| `experiment.analysis.ReplaySummaryBuilderTest` | 15 | 0 | 0 | 0 |
| `experiment.analysis.ThresholdSensitivityAnalyzerTest` | 6 | 0 | 0 | 0 |
| `experiment.coordinator.ExperimentCoordinatorTest` | 9 | 0 | 0 | 0 |
| `experiment.metrics.DefaultEvidenceSummaryBuilderTest` | 4 | 0 | 0 | 0 |
| `experiment.metrics.DefaultSnapshotAssemblerTest` | 6 | 0 | 0 | 0 |
| `experiment.metrics.InMemoryEvidenceRecorderTest` | 7 | 0 | 0 | 0 |
| `experiment.metrics.ManualPressureSamplerTest` | 4 | 0 | 0 | 0 |
| `experiment.metrics.MetricsBoundaryIsolationTest` | 2 | 0 | 0 | 0 |
| `experiment.metrics.MetricValueTest` | 3 | 0 | 0 | 0 |
| `experiment.metrics.RuntimeObservationTest` | 4 | 0 | 0 | 0 |
| `experiment.model.ExperimentRunTest` | 5 | 0 | 0 | 0 |
| `experiment.model.FoundationModelsTest` | 8 | 0 | 0 | 0 |
| `experiment.policy.DefaultControlGateTest` | 8 | 0 | 0 | 0 |
| `experiment.policy.PolicyBoundaryIsolationTest` | 1 | 0 | 0 | 0 |
| `experiment.policy.PolicyDecisionTest` | 16 | 0 | 0 | 0 |
| `experiment.policy.PolicyEnumsTest` | 2 | 0 | 0 | 0 |
| `experiment.policy.PolicyEvaluationInputTest` | 4 | 0 | 0 | 0 |
| `experiment.policy.ThresholdPolicyConfigTest` | 9 | 0 | 0 | 0 |
| `experiment.policy.ThresholdPolicyEvaluatorTest` | 18 | 0 | 0 | 0 |
| `experiment.scenario.BaselineExecutorPresetTest` | 9 | 0 | 0 | 0 |
| `experiment.scenario.BaselineWorkloadExecutorTest` | 4 | 0 | 0 | 0 |
| `experiment.scenario.DeterministicScenarioPlannerTest` | 5 | 0 | 0 | 0 |
| `experiment.scenario.ScenarioBoundaryIsolationTest` | 1 | 0 | 0 | 0 |
| `experiment.scenario.ScenarioDefinitionTest` | 7 | 0 | 0 | 0 |
| `experiment.scenario.ScenarioExperimentRunnerTest` | 5 | 0 | 0 | 0 |
| `experiment.scenario.ScenarioPlanTest` | 4 | 0 | 0 | 0 |
| `experiment.scenario.ScenarioStepTest` | 5 | 0 | 0 | 0 |
| **Total** | **236** | **0** | **0** | **0** |

## Spec → Test Traceability

| Spec requirement | Test(s) |
| --- | --- |
| Replay evidence validation (VALID/INVALID, failure codes) | `AnalysisContractsTest`, `ReplayEvidenceValidatorTest` |
| `MISSING_PRESSURE_FIELDS` covers all four required pressure fields | `ReplayEvidenceValidatorTest` (`shouldMarkInputWithMissingPressureFieldsAsInvalid`, `shouldMarkInputWithMissingActiveThreadsAsInvalid`, `shouldMarkInputWithMissingPoolSizeAsInvalid`, `shouldMarkInputWithMissingQueueSizeAsInvalid`, `shouldMarkInputWithMissingCompletedTaskCountAsInvalid`, `shouldNameAllMissingFieldsWhenMultipleAreAbsent`) |
| Failure reason names the missing fields and the offending snapshot | `ReplayEvidenceValidatorTest` (`shouldNameAllMissingFieldsWhenMultipleAreAbsent`, `shouldMarkInputWithMissingPoolSizeAsInvalid`, `shouldMarkInputWithMissingCompletedTaskCountAsInvalid`) |
| Run-level blocking counter (INVALID ⇒ accepted=0, rejected=snapshots.size()) | `ReplayEvidenceValidatorTest` (`shouldProduceZeroAcceptedAndFullRejectedCountForInvalidInput`, `shouldProduceZeroAcceptedAndFullRejectedCountForEmptySnapshots`, `shouldProduceZeroAcceptedAndFullRejectedCountForInsufficientSnapshots`, `shouldProduceZeroAcceptedAndFullRejectedCountForUnorderedTimestamps`, `shouldProduceZeroAcceptedAndFullRejectedCountForRunIdMismatch`) |
| Offline policy replay evidence (`replayMode=offline_replay`, full field set) | `AnalysisContractsTest` (`replayDecisionEvidenceShouldEnforceTimestampEquality`, `replayDecisionEvidenceShouldExposeFieldsAndReplayMode`), `OfflinePolicyReplayServiceTest` |
| Decision timestamp equals source snapshot timestamp | `AnalysisContractsTest` (`replayDecisionEvidenceShouldEnforceTimestampEquality`), `OfflinePolicyReplayServiceTest` (`replayShouldSetDecisionTimestampEqualToSnapshotTimestamp`, `replayShouldUseSnapshotTimestampAsPolicyEvaluationInput`, `replayShouldNotProduceWallClockTimeOnReplay`) |
| Forbidden runtime-mutation references in analysis package | `AnalysisBoundaryIsolationTest` (both tests) |
| Replay summary and oscillation signals | `AnalysisContractsTest` (`replayRunSummaryShouldEnforceCountConservation`), `ReplaySummaryBuilderTest` |
| Direction flips and alternating length (non-HOLD only) | `ReplaySummaryBuilderTest` (`shouldCountDirectionFlipsOnlyOnNonHoldSequence`, `shouldIgnoreHoldsWhenCountingDirectionFlips`, `shouldComputeAlternatingStreakMaxFromUpDownSequence`, `shouldIgnoreHoldsWhenCountingAlternatingStreak`, `shouldResetAlternatingStreakOnRepeatedDirection`) |
| `decisionCount + skippedCount == evidenceCount` | `AnalysisContractsTest` (`replayRunSummaryShouldEnforceCountConservation`), `ReplaySummaryBuilderTest` (`shouldEnforceCountConservation`, `shouldHandleZeroEvidence`) |
| Threshold sensitivity comparison (3 fixed configs + deltas vs default) | `AnalysisContractsTest`, `ThresholdSensitivityAnalyzerTest` |
| Mutation readiness assessment (READY / READY_WITH_RISK / NOT_READY) | `MutationReadinessGateTest` |
| Missing profile → NOT_READY | `MutationReadinessGateTest` (`shouldReturnNotReadyWhenAnyScenarioProfileIsMissing`) |
| Insufficient evidence → NOT_READY | `MutationReadinessGateTest` (`shouldReturnNotReadyWhenAnyRunHasInsufficientEvidence`) |
| Skipped evidence → NOT_READY | `MutationReadinessGateTest` (`shouldReturnNotReadyWhenAnySummaryHasSkippedEvidence`) |
| Risk-threshold breach → NOT_READY | `MutationReadinessGateTest` (`shouldReturnNotReadyWhenAnyMetricExceedsRiskThreshold`) |
| Ready-threshold breach, no risk breach → READY_WITH_RISK | `MutationReadinessGateTest` (`shouldReturnReadyWithRiskWhenMetricsExceedReadyButStayBelowRisk`) |
| All inside ready thresholds → READY | `MutationReadinessGateTest` (`shouldReturnReadyWhenAllMetricsInsideReadyThresholds`) |
| Default config label is the only readiness input | `MutationReadinessGateTest` (`shouldFilterToDefaultLabelOnly`) |
| Controlled replay report artifacts in `outputs/reports/v0.4.0/` | `ReplayReportWriterTest` (`shouldForceOutputDirectoryToV040`, `shouldAlwaysResolveToV040Subdirectory`) |
| Canonical file names | `ReplayReportWriterTest` (`shouldUseCanonicalRunSummaryFileName`, `shouldUseCanonicalScenarioSummaryFileName`, `shouldUseCanonicalSensitivityReportFileName`, `shouldUseCanonicalReadinessAssessmentFileName`, `shouldWriteCompositeMarkdownReport`) |
| Evidence hygiene (no raw snapshot evidence in report) | `ReplayReportWriterTest` (`shouldNotCopyRawSnapshotEvidence`) |
| Minimal fields in JSON | `ReplayReportWriterTest` (`shouldWriteMinimalFieldsInJsonArtifact`) |

## Hard-Constraint Compliance

- **`decisionTimestamp == snapshotTimestamp`** — enforced in
  `ReplayDecisionEvidence` constructor and exercised in
  `AnalysisContractsTest.replayDecisionEvidenceShouldEnforceTimestampEquality`
  and `OfflinePolicyReplayServiceTest.replayShouldSetDecisionTimestampEqualToSnapshotTimestamp`.
  `shouldNotProduceWallClockTimeOnReplay` confirms the replay service
  never reads wall-clock time by feeding a far-future timestamp and
  asserting the decision timestamp matches it.
- **No `Instant.now()` in analysis package** — confirmed by
  `AnalysisBoundaryIsolationTest.shouldNotReferenceForbiddenTypes`,
  which fails the build on any `Instant.now(` substring under
  `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/analysis`.
- **No `AdjustmentEvent`, `ThreadPoolExecutor`,
  `ScheduledExecutorService`, `ExecutorAdapter`,
  `QueueCapacityController`, `MutationValidator`, external IO
  types** — confirmed by the same boundary isolation test.
- **No `ScenarioExperimentRunner` / `BaselineWorkloadExecutor`** —
  confirmed by
  `AnalysisBoundaryIsolationTest.shouldNotImportScenarioRunnerTypes`.
- **No new Maven dependencies** — `pom.xml` is unchanged.
- **No new mutation, queue resizing, scheduler, persistence, REST,
  or external API types** — analysis package only depends on
  `experiment.metrics`, `experiment.scenario` (profile enum only),
  `experiment.policy`, and `experiment.model`. No `ThreadPoolExecutor`,
  no `ScheduledExecutorService`, no `AdjustmentEvent`, no
  `ExecutorAdapter`, no `QueueCapacityController`.

## Validator Counter Semantics (post-fix)

The `ReplayEvidenceValidator` uses a run-level blocking semantic.
The two counters are mutually exclusive by construction:

| `ReplayValidationStatus` | `acceptedSnapshotCount` | `rejectedSnapshotCount` |
| --- | ---: | ---: |
| `VALID` | `snapshots.size()` | `0` |
| `INVALID` (with snapshots) | `0` | `snapshots.size()` |
| `INVALID` (`EMPTY_SNAPSHOTS`) | `0` | `0` |

The counter pair is the only public surface that exposes this
semantic, and it is verified by five dedicated tests in
`ReplayEvidenceValidatorTest`. Callers that need to know whether
the run was accepted should consult `result.isValid()` and the
counter pair, never the counters alone.

## Final Verdict

- All implementation and test tasks are complete (`tasks.md` is
  fully checked).
- `apply.md` and `verify.md` are produced and reflect the
  post-authorization fixes to the validator's pressure-field check
  and counter semantics.
- The change is ready for an independent review pass; no archive
  is performed in this session, per the user's instruction.
