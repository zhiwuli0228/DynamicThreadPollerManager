package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.EvidenceSummary;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.util.List;
import java.util.Objects;

/**
 * Immutable input handed to {@link ReplayEvidenceValidator} and
 * {@link OfflinePolicyReplayService}. It captures the minimum
 * metadata required to audit a deterministic baseline run during
 * offline replay.
 */
public final class ReplayRunInput {

    private final String runId;
    private final String scenarioId;
    private final ScenarioProfile scenarioProfile;
    private final String baselinePolicyId;
    private final List<ObservedSnapshot> snapshots;
    private final EvidenceSummary evidenceSummary;
    private final int completedStepCount;
    private final int totalWorkUnits;

    public ReplayRunInput(String runId,
                          String scenarioId,
                          ScenarioProfile scenarioProfile,
                          String baselinePolicyId,
                          List<ObservedSnapshot> snapshots,
                          EvidenceSummary evidenceSummary,
                          int completedStepCount,
                          int totalWorkUnits) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId must not be blank");
        }
        this.scenarioProfile = Objects.requireNonNull(scenarioProfile, "scenarioProfile must not be null");
        if (baselinePolicyId == null || baselinePolicyId.isBlank()) {
            throw new IllegalArgumentException("baselinePolicyId must not be blank");
        }
        this.snapshots = List.copyOf(Objects.requireNonNull(snapshots, "snapshots must not be null"));
        this.evidenceSummary = Objects.requireNonNull(evidenceSummary, "evidenceSummary must not be null");
        if (completedStepCount < 0) {
            throw new IllegalArgumentException("completedStepCount must be >= 0");
        }
        if (totalWorkUnits < 0) {
            throw new IllegalArgumentException("totalWorkUnits must be >= 0");
        }
        this.runId = runId;
        this.scenarioId = scenarioId;
        this.baselinePolicyId = baselinePolicyId;
        this.completedStepCount = completedStepCount;
        this.totalWorkUnits = totalWorkUnits;
    }

    public String runId() {
        return runId;
    }

    public String scenarioId() {
        return scenarioId;
    }

    public ScenarioProfile scenarioProfile() {
        return scenarioProfile;
    }

    public String baselinePolicyId() {
        return baselinePolicyId;
    }

    public List<ObservedSnapshot> snapshots() {
        return snapshots;
    }

    public EvidenceSummary evidenceSummary() {
        return evidenceSummary;
    }

    public int completedStepCount() {
        return completedStepCount;
    }

    public int totalWorkUnits() {
        return totalWorkUnits;
    }
}
