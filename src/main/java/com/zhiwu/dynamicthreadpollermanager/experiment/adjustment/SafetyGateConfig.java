package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import java.util.Objects;

/**
 * Immutable safety gate configuration. The default values are the
 * design-pinned defaults from
 * {@code docs/04-development/versions/v0.5.0/20-sr.md}.
 */
public final class SafetyGateConfig {

    private final int cooldownDecisionIntervals;
    private final int maxAdjustmentsPerRun;
    private final boolean blockImmediateOppositeDirection;
    private final boolean allowReadyWithRisk;

    public SafetyGateConfig(int cooldownDecisionIntervals,
                            int maxAdjustmentsPerRun,
                            boolean blockImmediateOppositeDirection,
                            boolean allowReadyWithRisk) {
        if (cooldownDecisionIntervals < 0) {
            throw new IllegalArgumentException(
                    "cooldownDecisionIntervals must be >= 0, was " + cooldownDecisionIntervals);
        }
        if (maxAdjustmentsPerRun <= 0) {
            throw new IllegalArgumentException(
                    "maxAdjustmentsPerRun must be > 0, was " + maxAdjustmentsPerRun);
        }
        this.cooldownDecisionIntervals = cooldownDecisionIntervals;
        this.maxAdjustmentsPerRun = maxAdjustmentsPerRun;
        this.blockImmediateOppositeDirection = blockImmediateOppositeDirection;
        this.allowReadyWithRisk = allowReadyWithRisk;
    }

    public static SafetyGateConfig defaults() {
        return new SafetyGateConfig(2, 5, true, false);
    }

    public int cooldownDecisionIntervals() {
        return cooldownDecisionIntervals;
    }

    public int maxAdjustmentsPerRun() {
        return maxAdjustmentsPerRun;
    }

    public boolean blockImmediateOppositeDirection() {
        return blockImmediateOppositeDirection;
    }

    public boolean allowReadyWithRisk() {
        return allowReadyWithRisk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof SafetyGateConfig that
                && cooldownDecisionIntervals == that.cooldownDecisionIntervals
                && maxAdjustmentsPerRun == that.maxAdjustmentsPerRun
                && blockImmediateOppositeDirection == that.blockImmediateOppositeDirection
                && allowReadyWithRisk == that.allowReadyWithRisk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cooldownDecisionIntervals, maxAdjustmentsPerRun,
                blockImmediateOppositeDirection, allowReadyWithRisk);
    }

    @Override
    public String toString() {
        return "SafetyGateConfig{cooldown=%d, maxPerRun=%d, blockOpposite=%s, allowReadyWithRisk=%s}"
                .formatted(cooldownDecisionIntervals, maxAdjustmentsPerRun,
                        blockImmediateOppositeDirection, allowReadyWithRisk);
    }
}
