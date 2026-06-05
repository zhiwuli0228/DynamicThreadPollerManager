package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyDecision;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyEvaluationInput;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyEvaluator;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Drives offline replay of a {@link ReplayRunInput} through a
 * fixed {@link SensitivityConfigSet} and produces a list of
 * {@link ReplayDecisionEvidence} — one per (snapshot, config) pair.
 *
 * <p>The replay service is strictly read-only: it never mutates the
 * input, never calls wall-clock APIs, and never touches the
 * executor. Each {@code decisionTimestamp} is the source snapshot
 * timestamp so the replay is fully deterministic.
 */
public final class OfflinePolicyReplayService {

    private final PolicyEvaluator evaluator;
    private final SensitivityConfigSet configSet;

    public OfflinePolicyReplayService(PolicyEvaluator evaluator) {
        this(evaluator, SensitivityConfigSet.defaults());
    }

    public OfflinePolicyReplayService(PolicyEvaluator evaluator, SensitivityConfigSet configSet) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
        this.configSet = Objects.requireNonNull(configSet, "configSet must not be null");
    }

    public SensitivityConfigSet configSet() {
        return configSet;
    }

    public List<ReplayDecisionEvidence> replay(ReplayRunInput input) {
        Objects.requireNonNull(input, "input must not be null");
        List<ReplayDecisionEvidence> evidence = new ArrayList<>(
                input.snapshots().size() * configSet.size());
        for (int i = 0; i < input.snapshots().size(); i++) {
            ObservedSnapshot snapshot = input.snapshots().get(i);
            for (ThresholdPolicyConfig config : configSet.configs()) {
                evidence.add(replayOne(input, snapshot, i, config));
            }
        }
        return evidence;
    }

    private ReplayDecisionEvidence replayOne(ReplayRunInput input,
                                             ObservedSnapshot snapshot,
                                             int snapshotIndex,
                                             ThresholdPolicyConfig config) {
        PressureSnapshot pressure = snapshot.snapshot();
        PolicyEvaluationInput evalInput = new PolicyEvaluationInput(
                input.runId(), pressure, pressure.timestamp());
        PolicyDecision decision = evaluator.evaluate(evalInput, config);
        return new ReplayDecisionEvidence(
                input.runId(),
                input.scenarioId(),
                input.scenarioProfile(),
                labelFor(config),
                config.policyId(),
                snapshotIndex,
                pressure.timestamp(),
                pressure.timestamp(),
                decision.action(),
                decision.gateStatus(),
                decision.currentPoolSize(),
                decision.proposedPoolSize(),
                decision.reason()
        );
    }

    private static String labelFor(ThresholdPolicyConfig config) {
        return switch (config.policyId()) {
            case SensitivityConfigSet.DEFAULT_POLICY_ID -> SensitivityConfigSet.DEFAULT_LABEL;
            case SensitivityConfigSet.CONSERVATIVE_POLICY_ID -> SensitivityConfigSet.CONSERVATIVE_LABEL;
            case SensitivityConfigSet.AGGRESSIVE_POLICY_ID -> SensitivityConfigSet.AGGRESSIVE_LABEL;
            default -> config.policyId();
        };
    }
}
