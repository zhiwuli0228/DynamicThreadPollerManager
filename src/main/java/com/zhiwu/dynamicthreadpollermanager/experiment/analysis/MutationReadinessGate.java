package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Mutation readiness gate. Inspects a list of {@link ReplayRunSummary}
 * for the {@code default} sensitivity config and emits a
 * {@link ReadinessAssessment} verdict.
 *
 * <p>Decision rules (in order):
 * <ol>
 *   <li>Missing any of {@code STEADY}, {@code RAMP}, {@code BURST} →
 *       {@link ReadinessStatus#NOT_READY}.</li>
 *   <li>Any run with {@code evidenceCount < 3} → NOT_READY.</li>
 *   <li>Any summary with {@code skippedCount > 0} → NOT_READY.</li>
 *   <li>Any capped / hold / flip / streak ratio above the READY
 *       threshold → candidate for READY_WITH_RISK.</li>
 *   <li>Any such ratio above the RISK threshold → NOT_READY.</li>
 *   <li>Otherwise → READY.</li>
 * </ol>
 */
public final class MutationReadinessGate {

    private static final List<ScenarioProfile> REQUIRED_PROFILES =
            List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST);

    private final ReadinessThresholds thresholds;

    public MutationReadinessGate() {
        this(ReadinessThresholds.DEFAULTS);
    }

    public MutationReadinessGate(ReadinessThresholds thresholds) {
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds must not be null");
    }

    public ReadinessAssessment assess(List<ReplayRunSummary> runSummaries) {
        Objects.requireNonNull(runSummaries, "runSummaries must not be null");

        List<ReplayRunSummary> defaultRuns = new ArrayList<>();
        for (ReplayRunSummary run : runSummaries) {
            if (SensitivityConfigSet.DEFAULT_LABEL.equals(run.policyConfigLabel())) {
                defaultRuns.add(run);
            }
        }

        List<ScenarioProfile> evaluated = new ArrayList<>();
        for (ReplayRunSummary run : defaultRuns) {
            if (!evaluated.contains(run.scenarioProfile())) {
                evaluated.add(run.scenarioProfile());
            }
        }

        List<ScenarioProfile> missing = new ArrayList<>();
        for (ScenarioProfile required : REQUIRED_PROFILES) {
            if (!evaluated.contains(required)) {
                missing.add(required);
            }
        }

        List<String> blockingReasons = new ArrayList<>();
        List<String> riskReasons = new ArrayList<>();
        List<String> inputRunIds = new ArrayList<>();

        for (ScenarioProfile profile : missing) {
            blockingReasons.add("missing required scenarioProfile " + profile);
        }

        for (ReplayRunSummary run : defaultRuns) {
            inputRunIds.add(run.runId());
            if (run.evidenceCount() < 3) {
                blockingReasons.add("run " + run.runId() + " evidenceCount="
                        + run.evidenceCount() + " is below minimum 3");
            }
            if (run.skippedCount() > 0) {
                blockingReasons.add("run " + run.runId() + " skippedCount="
                        + run.skippedCount() + " is > 0");
            }
        }

        ReadinessStatus status;
        if (!blockingReasons.isEmpty()) {
            status = ReadinessStatus.NOT_READY;
        } else {
            Set<String> readyViolations = new LinkedHashSet<>();
            Set<String> riskViolations = new LinkedHashSet<>();

            for (ReplayRunSummary run : defaultRuns) {
                String runTag = "run " + run.runId();
                checkCappedRatio(run, runTag, readyViolations, riskViolations);
                checkHoldRatio(run, runTag, readyViolations, riskViolations);
                checkDirectionFlips(run, runTag, readyViolations, riskViolations);
                checkAlternatingStreak(run, runTag, readyViolations, riskViolations);
            }

            if (!riskViolations.isEmpty()) {
                status = ReadinessStatus.NOT_READY;
                blockingReasons.addAll(riskViolations);
            } else if (!readyViolations.isEmpty()) {
                status = ReadinessStatus.READY_WITH_RISK;
                riskReasons.addAll(readyViolations);
            } else {
                status = ReadinessStatus.READY;
            }
        }

        return new ReadinessAssessment(
                status,
                evaluated,
                missing,
                blockingReasons,
                riskReasons,
                SensitivityConfigSet.DEFAULT_LABEL,
                inputRunIds
        );
    }

    private void checkCappedRatio(ReplayRunSummary run,
                                  String runTag,
                                  Set<String> readyViolations,
                                  Set<String> riskViolations) {
        if (run.cappedRatio() > thresholds.maxCappedRatioForRisk()) {
            riskViolations.add(runTag + " cappedRatio=" + run.cappedRatio()
                    + " exceeds risk ceiling " + thresholds.maxCappedRatioForRisk());
        } else if (run.cappedRatio() > thresholds.maxCappedRatioForReady()) {
            readyViolations.add(runTag + " cappedRatio=" + run.cappedRatio()
                    + " exceeds ready ceiling " + thresholds.maxCappedRatioForReady());
        }
    }

    private void checkHoldRatio(ReplayRunSummary run,
                                String runTag,
                                Set<String> readyViolations,
                                Set<String> riskViolations) {
        if (run.holdRatio() > thresholds.maxHoldRatioForRisk()) {
            riskViolations.add(runTag + " holdRatio=" + run.holdRatio()
                    + " exceeds risk ceiling " + thresholds.maxHoldRatioForRisk());
        } else if (run.holdRatio() > thresholds.maxHoldRatioForReady()) {
            readyViolations.add(runTag + " holdRatio=" + run.holdRatio()
                    + " exceeds ready ceiling " + thresholds.maxHoldRatioForReady());
        }
    }

    private void checkDirectionFlips(ReplayRunSummary run,
                                     String runTag,
                                     Set<String> readyViolations,
                                     Set<String> riskViolations) {
        if (run.directionFlipCount() > thresholds.maxDirectionFlipCountForRisk()) {
            riskViolations.add(runTag + " directionFlipCount=" + run.directionFlipCount()
                    + " exceeds risk ceiling " + thresholds.maxDirectionFlipCountForRisk());
        } else if (run.directionFlipCount() > thresholds.maxDirectionFlipCountForReady()) {
            readyViolations.add(runTag + " directionFlipCount=" + run.directionFlipCount()
                    + " exceeds ready ceiling " + thresholds.maxDirectionFlipCountForReady());
        }
    }

    private void checkAlternatingStreak(ReplayRunSummary run,
                                        String runTag,
                                        Set<String> readyViolations,
                                        Set<String> riskViolations) {
        if (run.alternatingStreakMax() > thresholds.maxAlternatingStreakForRisk()) {
            riskViolations.add(runTag + " alternatingStreakMax=" + run.alternatingStreakMax()
                    + " exceeds risk ceiling " + thresholds.maxAlternatingStreakForRisk());
        } else if (run.alternatingStreakMax() > thresholds.maxAlternatingStreakForReady()) {
            readyViolations.add(runTag + " alternatingStreakMax=" + run.alternatingStreakMax()
                    + " exceeds ready ceiling " + thresholds.maxAlternatingStreakForReady());
        }
    }
}
