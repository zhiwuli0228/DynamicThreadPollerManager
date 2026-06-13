package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import java.util.Objects;

public final class PolicyReplacementResult {

    private final boolean success;
    private final PolicyReplacementEvidence evidence;
    private final String failureCode;
    private final String reason;

    private PolicyReplacementResult(boolean success,
                                     PolicyReplacementEvidence evidence,
                                     String failureCode,
                                     String reason) {
        this.success = success;
        this.evidence = evidence;
        this.failureCode = failureCode;
        this.reason = reason;
    }

    public static PolicyReplacementResult success(PolicyReplacementEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        return new PolicyReplacementResult(true, evidence, null, null);
    }

    public static PolicyReplacementResult denied(String failureCode, String reason,
                                                   PolicyReplacementEvidence evidence) {
        Objects.requireNonNull(failureCode, "failureCode must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");
        return new PolicyReplacementResult(false, evidence, failureCode, reason);
    }

    public static PolicyReplacementResult failed(String failureCode, String reason) {
        Objects.requireNonNull(failureCode, "failureCode must not be null");
        return new PolicyReplacementResult(false, null, failureCode, reason);
    }

    public static PolicyReplacementResult failed(String failureCode, String reason,
                                                   PolicyReplacementEvidence evidence) {
        Objects.requireNonNull(failureCode, "failureCode must not be null");
        return new PolicyReplacementResult(false, evidence, failureCode, reason);
    }

    public boolean success() {
        return success;
    }

    public PolicyReplacementEvidence evidence() {
        return evidence;
    }

    public String failureCode() {
        return failureCode;
    }

    public String reason() {
        return reason;
    }
}
