package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.GateStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyAction;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable record of a single offline policy decision produced
 * during replay. The {@code decisionTimestamp} MUST equal the source
 * snapshot timestamp so replay remains deterministic and never
 * consults wall-clock time.
 *
 * <p>{@code replayMode} is fixed to {@code offline_replay} so this
 * evidence can never be confused with a runtime adjustment.
 */
public final class ReplayDecisionEvidence {

    public static final String REPLAY_MODE = "offline_replay";

    private final String runId;
    private final String scenarioId;
    private final ScenarioProfile scenarioProfile;
    private final String policyConfigLabel;
    private final String policyId;
    private final int snapshotIndex;
    private final Instant snapshotTimestamp;
    private final Instant decisionTimestamp;
    private final PolicyAction action;
    private final GateStatus gateStatus;
    private final int currentPoolSize;
    private final int proposedPoolSize;
    private final String reason;
    private final String replayMode;

    public ReplayDecisionEvidence(String runId,
                                  String scenarioId,
                                  ScenarioProfile scenarioProfile,
                                  String policyConfigLabel,
                                  String policyId,
                                  int snapshotIndex,
                                  Instant snapshotTimestamp,
                                  Instant decisionTimestamp,
                                  PolicyAction action,
                                  GateStatus gateStatus,
                                  int currentPoolSize,
                                  int proposedPoolSize,
                                  String reason) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId must not be blank");
        }
        this.scenarioProfile = Objects.requireNonNull(scenarioProfile, "scenarioProfile must not be null");
        if (policyConfigLabel == null || policyConfigLabel.isBlank()) {
            throw new IllegalArgumentException("policyConfigLabel must not be blank");
        }
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId must not be blank");
        }
        if (snapshotIndex < 0) {
            throw new IllegalArgumentException("snapshotIndex must be >= 0");
        }
        this.snapshotTimestamp = Objects.requireNonNull(snapshotTimestamp, "snapshotTimestamp must not be null");
        this.decisionTimestamp = Objects.requireNonNull(decisionTimestamp, "decisionTimestamp must not be null");
        if (!snapshotTimestamp.equals(decisionTimestamp)) {
            throw new IllegalArgumentException(
                    "decisionTimestamp must equal snapshotTimestamp for deterministic offline replay");
        }
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.gateStatus = Objects.requireNonNull(gateStatus, "gateStatus must not be null");
        if (currentPoolSize < 0) {
            throw new IllegalArgumentException("currentPoolSize must be >= 0");
        }
        if (proposedPoolSize < 0) {
            throw new IllegalArgumentException("proposedPoolSize must be >= 0");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        this.runId = runId;
        this.scenarioId = scenarioId;
        this.policyConfigLabel = policyConfigLabel;
        this.policyId = policyId;
        this.snapshotIndex = snapshotIndex;
        this.currentPoolSize = currentPoolSize;
        this.proposedPoolSize = proposedPoolSize;
        this.reason = reason;
        this.replayMode = REPLAY_MODE;
    }

    public String runId() { return runId; }
    public String scenarioId() { return scenarioId; }
    public ScenarioProfile scenarioProfile() { return scenarioProfile; }
    public String policyConfigLabel() { return policyConfigLabel; }
    public String policyId() { return policyId; }
    public int snapshotIndex() { return snapshotIndex; }
    public Instant snapshotTimestamp() { return snapshotTimestamp; }
    public Instant decisionTimestamp() { return decisionTimestamp; }
    public PolicyAction action() { return action; }
    public GateStatus gateStatus() { return gateStatus; }
    public int currentPoolSize() { return currentPoolSize; }
    public int proposedPoolSize() { return proposedPoolSize; }
    public String reason() { return reason; }
    public String replayMode() { return replayMode; }
}
