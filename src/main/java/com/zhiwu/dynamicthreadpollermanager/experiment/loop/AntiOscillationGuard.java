package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentFailureCode;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.SafetyGateDecision;

import java.util.Objects;

/**
 * Standalone guard that blocks non-emergency adjustments when sustained
 * oscillation is detected. Consults an {@link OscillationDetector} for
 * pattern recognition and maintains a consecutive-oscillation counter.
 *
 * <p>Emergency rollback commands always bypass the guard.
 *
 * <p>Thread-safe: {@code evaluate} and {@code reset} are synchronized.
 */
public final class AntiOscillationGuard {

    private final OscillationDetector detector;
    private final int blockThreshold;
    private int consecutiveOscillations;
    private boolean activated;

    public AntiOscillationGuard(OscillationDetector detector, int blockThreshold) {
        this.detector = Objects.requireNonNull(detector, "detector must not be null");
        if (blockThreshold < 1) {
            throw new IllegalArgumentException("blockThreshold must be >= 1, was " + blockThreshold);
        }
        this.blockThreshold = blockThreshold;
        this.consecutiveOscillations = 0;
        this.activated = false;
    }

    /**
     * Evaluate whether {@code pending} should be blocked due to sustained
     * oscillation. Emergency rollback commands always return ALLOW.
     *
     * @param pending              the decision about to be executed
     * @param history              the adjustment history
     * @param isEmergencyRollback  true if this is an emergency rollback
     * @return ALLOW if the adjustment may proceed, REJECTED with
     *         {@link AdjustmentFailureCode#ANTI_OSCILLATION_ACTIVE} if blocked
     */
    public synchronized SafetyGateDecision evaluate(AdjustmentDecision pending,
                                                     AdjustmentHistory history,
                                                     boolean isEmergencyRollback) {
        Objects.requireNonNull(pending, "pending must not be null");
        Objects.requireNonNull(history, "history must not be null");

        if (pending.isNoOp()) {
            return SafetyGateDecision.noOp("pending decision is a no-op");
        }

        if (isEmergencyRollback) {
            return SafetyGateDecision.allow(0, null);
        }

        boolean wouldOscillate = detector.wouldOscillate(pending, history);

        if (wouldOscillate) {
            consecutiveOscillations++;
        } else {
            consecutiveOscillations = 0;
            activated = false;
        }

        if (consecutiveOscillations >= blockThreshold) {
            activated = true;
            return SafetyGateDecision.rejected(AdjustmentFailureCode.ANTI_OSCILLATION_ACTIVE,
                    "anti-oscillation guard active: " + consecutiveOscillations
                            + " consecutive oscillations detected (threshold=" + blockThreshold + ")");
        }

        return SafetyGateDecision.allow(0, null);
    }

    public synchronized void reset() {
        consecutiveOscillations = 0;
        activated = false;
    }

    public synchronized boolean isActivated() {
        return activated;
    }

    public synchronized int consecutiveOscillations() {
        return consecutiveOscillations;
    }

    public int blockThreshold() {
        return blockThreshold;
    }
}
