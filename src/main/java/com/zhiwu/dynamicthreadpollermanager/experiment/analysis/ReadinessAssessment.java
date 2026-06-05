package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.util.List;
import java.util.Objects;

/**
 * Verdict of the mutation readiness gate. {@code selectedConfigLabel}
 * is fixed to {@code default} in this version; the other two
 * sensitivity configs are reference-only.
 */
public final class ReadinessAssessment {

    public static final String DEFAULT_CONFIG_LABEL = "default";

    private final ReadinessStatus status;
    private final List<ScenarioProfile> evaluatedScenarioProfiles;
    private final List<ScenarioProfile> missingScenarioProfiles;
    private final List<String> blockingReasons;
    private final List<String> riskReasons;
    private final String selectedConfigLabel;
    private final List<String> inputRunIds;

    public ReadinessAssessment(ReadinessStatus status,
                               List<ScenarioProfile> evaluatedScenarioProfiles,
                               List<ScenarioProfile> missingScenarioProfiles,
                               List<String> blockingReasons,
                               List<String> riskReasons,
                               String selectedConfigLabel,
                               List<String> inputRunIds) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.evaluatedScenarioProfiles = List.copyOf(Objects.requireNonNull(evaluatedScenarioProfiles, "evaluatedScenarioProfiles must not be null"));
        this.missingScenarioProfiles = List.copyOf(Objects.requireNonNull(missingScenarioProfiles, "missingScenarioProfiles must not be null"));
        this.blockingReasons = List.copyOf(Objects.requireNonNull(blockingReasons, "blockingReasons must not be null"));
        this.riskReasons = List.copyOf(Objects.requireNonNull(riskReasons, "riskReasons must not be null"));
        if (selectedConfigLabel == null || selectedConfigLabel.isBlank()) {
            throw new IllegalArgumentException("selectedConfigLabel must not be blank");
        }
        this.selectedConfigLabel = selectedConfigLabel;
        this.inputRunIds = List.copyOf(Objects.requireNonNull(inputRunIds, "inputRunIds must not be null"));
    }

    public ReadinessStatus status() { return status; }
    public List<ScenarioProfile> evaluatedScenarioProfiles() { return evaluatedScenarioProfiles; }
    public List<ScenarioProfile> missingScenarioProfiles() { return missingScenarioProfiles; }
    public List<String> blockingReasons() { return blockingReasons; }
    public List<String> riskReasons() { return riskReasons; }
    public String selectedConfigLabel() { return selectedConfigLabel; }
    public List<String> inputRunIds() { return inputRunIds; }
}
