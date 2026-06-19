package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Classifies a validated acquisition dataset into one of
 * {@code READY}, {@code READY_WITH_RISK}, or {@code NOT_READY}
 * and produces a {@link ReadinessSummary} with a bounded
 * {@code recommendedNextStep}. The classifier MUST NOT output
 * any field that could be interpreted as runtime mutation
 * authorization.
 *
 * <p>Rules (applied in order):
 * <ol>
 *   <li>If the dataset is {@code INVALID} (data quality
 *       gates failed), the verdict is {@code NOT_READY}; the
 *       data-quality blocking reasons are copied into the
 *       readiness blocking reasons and the next step is
 *       {@code collect_additional_evidence}.</li>
 *   <li>If a required profile is missing, the verdict is
 *       {@code NOT_READY}.</li>
 *   <li>If a required profile has risk signals, the verdict
 *       is {@code READY_WITH_RISK}; risk signals are
 *       recorded; the next step is
 *       {@code proceed_with_acknowledged_risk}.</li>
 *   <li>Otherwise the verdict is {@code READY}; the next
 *       step is {@code proceed_to_replay_review}.</li>
 * </ol>
 */
public final class AcquisitionReadinessClassifier {

    /**
     * Read-only risk profile description. The classifier
     * never invents risk: it copies caller-supplied reasons
     * verbatim into the {@link ReadinessSummary}.
     */
    public record RiskSignal(ScenarioProfile profile, String reason) {
        public RiskSignal {
            Objects.requireNonNull(profile, "profile");
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("reason must not be blank");
            }
        }
    }

    public AcquisitionReadinessClassifier() {
    }

    public ReadinessSummary classify(String runId,
                                     AcquisitionDataQualityResult quality,
                                     List<RiskSignal> riskSignals) {
        Objects.requireNonNull(quality, "quality must not be null");
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        List<RiskSignal> signals = riskSignals == null ? List.of() : List.copyOf(riskSignals);
        signals.forEach(s -> Objects.requireNonNull(s, "riskSignals must not contain null"));

        List<ScenarioProfile> evaluated = quality.evaluatedScenarioProfiles();
        List<ScenarioProfile> missingList = quality.missingScenarioProfiles();
        Set<ScenarioProfile> missing = missingList.isEmpty()
                ? EnumSet.noneOf(ScenarioProfile.class)
                : EnumSet.copyOf(new java.util.HashSet<>(missingList));
        List<String> blocking = new ArrayList<>(quality.blockingReasons());
        List<String> riskReasons = new ArrayList<>();
        ReadinessStatus status;
        String nextStep;

        if (quality.status() == AcquisitionDataQualityResult.Status.INVALID) {
            status = ReadinessStatus.NOT_READY;
            nextStep = ReadinessSummary.NEXT_STEP_COLLECT_MORE;
        } else if (!missing.isEmpty()) {
            status = ReadinessStatus.NOT_READY;
            for (ScenarioProfile m : missing) {
                blocking.add("missing required profile " + m.name());
            }
            nextStep = ReadinessSummary.NEXT_STEP_COLLECT_MORE;
        } else if (!signals.isEmpty()) {
            status = ReadinessStatus.READY_WITH_RISK;
            for (RiskSignal s : signals) {
                riskReasons.add(s.profile().name() + ": " + s.reason());
            }
            nextStep = ReadinessSummary.NEXT_STEP_REPLAY_WITH_CAUTION;
        } else {
            status = ReadinessStatus.READY;
            nextStep = ReadinessSummary.NEXT_STEP_REPLAY;
        }

        return new ReadinessSummary(
                runId,
                status,
                evaluated,
                List.copyOf(missing),
                List.copyOf(blocking),
                List.copyOf(riskReasons),
                nextStep);
    }
}
