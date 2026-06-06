package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.util.List;
import java.util.Objects;

/**
 * Bounded readiness verdict for an acquisition dataset. The
 * verdict is one of {@code READY}, {@code READY_WITH_RISK},
 * or {@code NOT_READY}, with an explicit
 * {@code recommendedNextStep} that never implies runtime
 * mutation authorization.
 *
 * <p>This type is intentionally narrower than the analysis
 * layer's {@code ReadinessAssessment}: it carries only what
 * the acquisition boundary needs to record, and it
 * deliberately does not duplicate the sensitivity
 * configuration comparison done by the analysis layer.
 */
public final class ReadinessSummary {

    public static final String NEXT_STEP_REPLAY = "proceed_to_replay_review";
    public static final String NEXT_STEP_REPLAY_WITH_CAUTION = "proceed_with_acknowledged_risk";
    public static final String NEXT_STEP_COLLECT_MORE = "collect_additional_evidence";

    private final String runId;
    private final ReadinessStatus status;
    private final List<ScenarioProfile> evaluatedScenarioProfiles;
    private final List<ScenarioProfile> missingScenarioProfiles;
    private final List<String> blockingReasons;
    private final List<String> riskReasons;
    private final String recommendedNextStep;

    public ReadinessSummary(String runId,
                            ReadinessStatus status,
                            List<ScenarioProfile> evaluatedScenarioProfiles,
                            List<ScenarioProfile> missingScenarioProfiles,
                            List<String> blockingReasons,
                            List<String> riskReasons,
                            String recommendedNextStep) {
        this.runId = requireNonBlank(runId, "runId");
        this.status = Objects.requireNonNull(status, "status");
        this.evaluatedScenarioProfiles = List.copyOf(Objects.requireNonNull(
                evaluatedScenarioProfiles, "evaluatedScenarioProfiles"));
        this.missingScenarioProfiles = List.copyOf(Objects.requireNonNull(
                missingScenarioProfiles, "missingScenarioProfiles"));
        this.blockingReasons = List.copyOf(Objects.requireNonNull(
                blockingReasons, "blockingReasons"));
        this.riskReasons = List.copyOf(Objects.requireNonNull(
                riskReasons, "riskReasons"));
        this.recommendedNextStep = requireNonBlank(
                recommendedNextStep, "recommendedNextStep");
        if (!isAllowedNextStep(recommendedNextStep)) {
            throw new IllegalArgumentException(
                    "recommendedNextStep must be one of the bounded set, was " + recommendedNextStep);
        }
        if (status == ReadinessStatus.NOT_READY && blockingReasons.isEmpty()) {
            throw new IllegalArgumentException(
                    "NOT_READY verdict must carry at least one blockingReason");
        }
        if (status == ReadinessStatus.READY && !riskReasons.isEmpty()) {
            throw new IllegalArgumentException(
                    "READY verdict must not carry riskReasons");
        }
        if (status == ReadinessStatus.READY_WITH_RISK && riskReasons.isEmpty()) {
            throw new IllegalArgumentException(
                    "READY_WITH_RISK verdict must carry at least one riskReason");
        }
    }

    public String runId() { return runId; }
    public ReadinessStatus status() { return status; }
    public List<ScenarioProfile> evaluatedScenarioProfiles() { return evaluatedScenarioProfiles; }
    public List<ScenarioProfile> missingScenarioProfiles() { return missingScenarioProfiles; }
    public List<String> blockingReasons() { return blockingReasons; }
    public List<String> riskReasons() { return riskReasons; }
    public String recommendedNextStep() { return recommendedNextStep; }

    /**
     * The acquisition readiness verdict is descriptive only.
     * It MUST NOT carry any field that could be interpreted as
     * authorizing runtime mutation. If a future revision needs
     * to add such a field, the readiness type must be
     * redesigned and the spec re-validated.
     */
    public boolean isMutationAuthorizing() {
        return false;
    }

    private static boolean isAllowedNextStep(String step) {
        return NEXT_STEP_REPLAY.equals(step)
                || NEXT_STEP_REPLAY_WITH_CAUTION.equals(step)
                || NEXT_STEP_COLLECT_MORE.equals(step);
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
