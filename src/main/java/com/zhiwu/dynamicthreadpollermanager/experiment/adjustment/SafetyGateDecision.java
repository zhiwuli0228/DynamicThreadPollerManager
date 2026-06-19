package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import java.util.Objects;

/**
 * Decision produced by a {@link RuntimeAdjustmentSafetyGate}. The
 * outcome separates "allow" (call adapter), "reject" (do not call
 * adapter) and "no-op" (do not call adapter; the target matches the
 * current state).
 */
public final class SafetyGateDecision {

    public enum Outcome {
        ALLOW,
        REJECTED,
        NO_OP
    }

    private final Outcome outcome;
    private final AdjustmentFailureCode failureCode;
    private final String reason;
    private final int appliedAdjustmentsForRun;
    private final int cooldownRemaining;
    private final ScaleAdjustmentCommand appliedCommand;

    public SafetyGateDecision(Outcome outcome,
                              AdjustmentFailureCode failureCode,
                              String reason,
                              int appliedAdjustmentsForRun,
                              int cooldownRemaining,
                              ScaleAdjustmentCommand appliedCommand) {
        this.outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        if (outcome == Outcome.REJECTED) {
            Objects.requireNonNull(failureCode, "failureCode must not be null for REJECTED");
            Objects.requireNonNull(reason, "reason must not be null for REJECTED");
        }
        if (reason != null && reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        this.failureCode = failureCode;
        this.reason = reason;
        this.appliedAdjustmentsForRun = appliedAdjustmentsForRun;
        this.cooldownRemaining = cooldownRemaining;
        this.appliedCommand = appliedCommand;
    }

    public static SafetyGateDecision allow(int appliedAdjustmentsForRun,
                                            ScaleAdjustmentCommand command) {
        return new SafetyGateDecision(Outcome.ALLOW, null, "allowed",
                appliedAdjustmentsForRun, 0, command);
    }

    public static SafetyGateDecision noOp(String reason) {
        return new SafetyGateDecision(Outcome.NO_OP, null, reason, 0, 0, null);
    }

    public static SafetyGateDecision rejected(AdjustmentFailureCode code, String reason) {
        return new SafetyGateDecision(Outcome.REJECTED, code, reason, 0, 0, null);
    }

    public Outcome outcome() {
        return outcome;
    }

    public AdjustmentFailureCode failureCode() {
        return failureCode;
    }

    public String reason() {
        return reason;
    }

    public int appliedAdjustmentsForRun() {
        return appliedAdjustmentsForRun;
    }

    public int cooldownRemaining() {
        return cooldownRemaining;
    }

    public ScaleAdjustmentCommand appliedCommand() {
        return appliedCommand;
    }

    public boolean isAllowed() {
        return outcome == Outcome.ALLOW;
    }
}
