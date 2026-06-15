package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import java.util.Optional;

/**
 * Stub oscillation detector for Change 1. Always returns false (no oscillation).
 * Full implementation provided in Change 2.
 */
public final class OscillationDetector {

    private final int windowSize;
    private final int patternThreshold;

    public OscillationDetector(int windowSize, int patternThreshold) {
        if (windowSize < 4) {
            throw new IllegalArgumentException("windowSize must be >= 4, was " + windowSize);
        }
        if (patternThreshold < 1) {
            throw new IllegalArgumentException("patternThreshold must be >= 1, was " + patternThreshold);
        }
        this.windowSize = windowSize;
        this.patternThreshold = patternThreshold;
    }

    public OscillationDetector() {
        this(6, 2);
    }

    /** Stub: always returns false. */
    public boolean wouldOscillate(AdjustmentDecision pending, AdjustmentHistory history) {
        return false;
    }

    /** Stub: always returns empty. */
    public Optional<String> detectedPattern(AdjustmentHistory history) {
        return Optional.empty();
    }
}
