package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.util.List;
import java.util.Objects;

/**
 * Aggregate of {@link ReplayRunSummary} entries for a single
 * (scenarioProfile, policyConfigLabel) pair.
 */
public final class ReplayScenarioSummary {

    private final ScenarioProfile scenarioProfile;
    private final String policyConfigLabel;
    private final List<String> runIds;
    private final List<ReplayRunSummary> runSummaries;
    private final int totalEvidenceCount;
    private final int totalDecisionCount;
    private final int totalSkippedCount;
    private final int aggregateDirectionFlipCount;
    private final int aggregateAlternatingStreakMax;
    private final double aggregateHoldRatio;
    private final double aggregateCappedRatio;

    public ReplayScenarioSummary(ScenarioProfile scenarioProfile,
                                 String policyConfigLabel,
                                 List<String> runIds,
                                 List<ReplayRunSummary> runSummaries,
                                 int totalEvidenceCount,
                                 int totalDecisionCount,
                                 int totalSkippedCount,
                                 int aggregateDirectionFlipCount,
                                 int aggregateAlternatingStreakMax,
                                 double aggregateHoldRatio,
                                 double aggregateCappedRatio) {
        this.scenarioProfile = Objects.requireNonNull(scenarioProfile, "scenarioProfile must not be null");
        if (policyConfigLabel == null || policyConfigLabel.isBlank()) {
            throw new IllegalArgumentException("policyConfigLabel must not be blank");
        }
        this.runIds = List.copyOf(Objects.requireNonNull(runIds, "runIds must not be null"));
        this.runSummaries = List.copyOf(Objects.requireNonNull(runSummaries, "runSummaries must not be null"));
        if (totalEvidenceCount < 0 || totalDecisionCount < 0 || totalSkippedCount < 0) {
            throw new IllegalArgumentException("totals must be >= 0");
        }
        if (totalDecisionCount + totalSkippedCount != totalEvidenceCount) {
            throw new IllegalArgumentException("totalDecisionCount + totalSkippedCount must equal totalEvidenceCount");
        }
        if (aggregateHoldRatio < 0.0 || aggregateHoldRatio > 1.0
                || aggregateCappedRatio < 0.0 || aggregateCappedRatio > 1.0) {
            throw new IllegalArgumentException("aggregate ratios must be in [0.0, 1.0]");
        }
        this.policyConfigLabel = policyConfigLabel;
        this.totalEvidenceCount = totalEvidenceCount;
        this.totalDecisionCount = totalDecisionCount;
        this.totalSkippedCount = totalSkippedCount;
        this.aggregateDirectionFlipCount = aggregateDirectionFlipCount;
        this.aggregateAlternatingStreakMax = aggregateAlternatingStreakMax;
        this.aggregateHoldRatio = aggregateHoldRatio;
        this.aggregateCappedRatio = aggregateCappedRatio;
    }

    public ScenarioProfile scenarioProfile() { return scenarioProfile; }
    public String policyConfigLabel() { return policyConfigLabel; }
    public List<String> runIds() { return runIds; }
    public List<ReplayRunSummary> runSummaries() { return runSummaries; }
    public int totalEvidenceCount() { return totalEvidenceCount; }
    public int totalDecisionCount() { return totalDecisionCount; }
    public int totalSkippedCount() { return totalSkippedCount; }
    public int aggregateDirectionFlipCount() { return aggregateDirectionFlipCount; }
    public int aggregateAlternatingStreakMax() { return aggregateAlternatingStreakMax; }
    public double aggregateHoldRatio() { return aggregateHoldRatio; }
    public double aggregateCappedRatio() { return aggregateCappedRatio; }
}
