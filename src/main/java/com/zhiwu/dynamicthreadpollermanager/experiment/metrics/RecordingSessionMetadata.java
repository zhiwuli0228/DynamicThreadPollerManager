package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import java.time.Instant;

/**
 * Immutable snapshot of a closed recording session.
 * Stores executor config as individual fields to avoid
 * importing executor types into the metrics package.
 */
public record RecordingSessionMetadata(
        String sessionId,
        String runId,
        int corePoolSize,
        int maximumPoolSize,
        int queueCapacity,
        long keepAliveTime,
        String keepAliveTimeUnit,
        Instant startedAt,
        Instant closedAt,
        int snapshotCount,
        SessionStatus status
) {}
