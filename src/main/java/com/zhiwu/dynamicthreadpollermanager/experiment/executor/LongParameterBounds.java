package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import java.util.Objects;

public final class LongParameterBounds {

    private final long minValue;
    private final long maxValue;

    private LongParameterBounds(long minValue, long maxValue) {
        if (minValue > maxValue) {
            throw new IllegalArgumentException(
                    "minValue must be <= maxValue, was min=" + minValue + " max=" + maxValue);
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public static LongParameterBounds of(long minValue, long maxValue) {
        return new LongParameterBounds(minValue, maxValue);
    }

    public long minValue() {
        return minValue;
    }

    public long maxValue() {
        return maxValue;
    }

    public boolean within(long value) {
        return value >= minValue && value <= maxValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LongParameterBounds that)) return false;
        return minValue == that.minValue && maxValue == that.maxValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minValue, maxValue);
    }

    @Override
    public String toString() {
        return "LongParameterBounds{min=" + minValue + ", max=" + maxValue + "}";
    }
}
