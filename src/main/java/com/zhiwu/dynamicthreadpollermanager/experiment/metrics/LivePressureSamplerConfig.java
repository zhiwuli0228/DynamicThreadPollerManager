package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import java.util.Objects;

/**
 * Configuration for {@link LivePressureSampler}.
 */
public record LivePressureSamplerConfig(
        long pollIntervalMs,
        boolean autoStart,
        String sessionId
) {
    public LivePressureSamplerConfig {
        if (pollIntervalMs < 100) {
            throw new IllegalArgumentException(
                    "pollIntervalMs must be >= 100, was " + pollIntervalMs);
        }
        Objects.requireNonNull(sessionId, "sessionId must not be null");
    }

    public static LivePressureSamplerConfig defaults(String sessionId) {
        return new LivePressureSamplerConfig(1000, false, sessionId);
    }
}
