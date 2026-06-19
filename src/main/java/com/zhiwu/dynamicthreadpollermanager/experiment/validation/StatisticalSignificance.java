package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

import java.util.Objects;

public record StatisticalSignificance(
        String metricName,
        double pValue,
        double ciLow,
        double ciHigh,
        double effectSize,
        boolean isSignificant,
        int sampleSize
) {
    public StatisticalSignificance {
        Objects.requireNonNull(metricName, "metricName must not be null");
        if (sampleSize < 0) {
            throw new IllegalArgumentException("sampleSize must be >= 0, was " + sampleSize);
        }
    }
}
