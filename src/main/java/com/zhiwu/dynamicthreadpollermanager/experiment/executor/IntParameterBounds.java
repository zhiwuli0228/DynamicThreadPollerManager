package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import java.util.Objects;

public final class IntParameterBounds {

    private final int minValue;
    private final int maxValue;

    private IntParameterBounds(int minValue, int maxValue) {
        if (minValue > maxValue) {
            throw new IllegalArgumentException(
                    "minValue must be <= maxValue, was min=" + minValue + " max=" + maxValue);
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public static IntParameterBounds of(int minValue, int maxValue) {
        return new IntParameterBounds(minValue, maxValue);
    }

    public int minValue() {
        return minValue;
    }

    public int maxValue() {
        return maxValue;
    }

    public boolean within(int value) {
        return value >= minValue && value <= maxValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntParameterBounds that)) return false;
        return minValue == that.minValue && maxValue == that.maxValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minValue, maxValue);
    }

    @Override
    public String toString() {
        return "IntParameterBounds{min=" + minValue + ", max=" + maxValue + "}";
    }
}
