package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import java.util.Objects;

/**
 * Result returned by {@link QueueResizeAdjustmentAdapter} after
 * attempting a queue resize operation. Carries success/failure status,
 * a failure code string, and {@link ResizeEvidence} when available.
 */
public final class QueueResizeResult {

    private final boolean success;
    private final ResizeEvidence evidence;
    private final String failureCode;
    private final String errorMessage;

    private QueueResizeResult(boolean success, ResizeEvidence evidence,
                               String failureCode, String errorMessage) {
        this.success = success;
        this.evidence = evidence;
        this.failureCode = failureCode;
        this.errorMessage = errorMessage;
    }

    public static QueueResizeResult success(ResizeEvidence evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        return new QueueResizeResult(true, evidence, null, null);
    }

    public static QueueResizeResult failed(String failureCode, String errorMessage) {
        Objects.requireNonNull(failureCode, "failureCode must not be null");
        return new QueueResizeResult(false, null, failureCode, errorMessage);
    }

    public static QueueResizeResult failed(String failureCode, String errorMessage,
                                            ResizeEvidence evidence) {
        Objects.requireNonNull(failureCode, "failureCode must not be null");
        return new QueueResizeResult(false, evidence, failureCode, errorMessage);
    }

    public boolean success() {
        return success;
    }

    public ResizeEvidence evidence() {
        return evidence;
    }

    public String failureCode() {
        return failureCode;
    }

    public String errorMessage() {
        return errorMessage;
    }
}
