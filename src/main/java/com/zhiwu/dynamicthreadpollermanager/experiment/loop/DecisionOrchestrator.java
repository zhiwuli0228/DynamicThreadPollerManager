package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.classification.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable orchestrator that executes the full decision pipeline:
 * classify → rank → find config → construct input → evaluate → assemble.
 *
 * <p>Immutable. Weight updates after calibration are handled by
 * {@code AdjustmentLoop} creating a new orchestrator instance.
 */
public final class DecisionOrchestrator {

    private final PressureClassifier classifier;
    private final PolicyRanker ranker;
    private final PolicyEvaluator evaluator;
    private final ClassifierConfig classifierConfig;

    public DecisionOrchestrator(
            PressureClassifier classifier,
            PolicyRanker ranker,
            PolicyEvaluator evaluator,
            ClassifierConfig classifierConfig) {
        this.classifier = Objects.requireNonNull(classifier, "classifier must not be null");
        this.ranker = Objects.requireNonNull(ranker, "ranker must not be null");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
        this.classifierConfig = Objects.requireNonNull(classifierConfig, "classifierConfig must not be null");
    }

    public AdjustmentDecision decide(
            List<ObservedSnapshot> snapshots,
            List<ThresholdPolicyConfig> candidates,
            ManagedExecutor executor,
            String runId) {

        if (snapshots.isEmpty()) {
            return createNoOpDecision("empty snapshots", runId);
        }

        // Step 1: compute total duration
        long firstTs = snapshots.get(0).snapshot().timestamp().toEpochMilli();
        long lastTs = snapshots.get(snapshots.size() - 1).snapshot().timestamp().toEpochMilli();
        long totalDurationMs = Math.max(0, lastTs - firstTs);

        // Step 2: classify
        long rejectedTaskCount = executor.getRejectedTaskCount();
        PressureClassification classification = classifier.classify(
                snapshots, classifierConfig, rejectedTaskCount, totalDurationMs);

        // Step 3: rank policies
        List<PolicyScore> ranked = ranker.rank(classification, candidates);
        if (ranked.isEmpty()) {
            return createNoOpDecision("no candidates scored", runId);
        }
        PolicyScore bestScore = ranked.get(0);

        // Step 4: find corresponding config
        ThresholdPolicyConfig selectedConfig = candidates.stream()
                .filter(c -> c.policyId().equals(bestScore.policyId()))
                .findFirst()
                .orElse(candidates.get(0));

        // Step 5: construct PolicyEvaluationInput from last snapshot timestamp
        PressureSnapshot lastSnapshot = snapshots.get(snapshots.size() - 1).snapshot();
        PolicyEvaluationInput input = new PolicyEvaluationInput(
                runId, lastSnapshot, lastSnapshot.timestamp());

        // Step 6: evaluate selected policy
        PolicyDecision decision = evaluator.evaluate(input, selectedConfig);

        // Step 7: assemble rationale
        String rationale = String.format(
                "[%s] Selected policy '%s' (composite=%.2f): %s",
                classification.state().name(),
                bestScore.policyId(),
                bestScore.compositeScore(),
                decision.reason());

        return new AdjustmentDecision(
                classification, bestScore, selectedConfig, decision, rationale, Instant.now());
    }

    PolicyRanker ranker() { return ranker; }

    private AdjustmentDecision createNoOpDecision(String reason, String runId) {
        return new AdjustmentDecision(
                new PressureClassification(
                        PressureState.NORMAL, 0.0, List.of(),
                        NormalizedPressureMetrics.fromSnapshots(List.of(), 0L, 0, 5),
                        Instant.now()),
                null,
                null,
                new PolicyDecision(runId, "no-op", Instant.now(), PolicyAction.HOLD,
                        GateStatus.HOLD, 0, 0, reason),
                reason,
                Instant.now());
    }
}
