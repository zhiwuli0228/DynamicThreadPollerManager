package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import java.time.Instant;
import java.util.Objects;

/**
 * Manages the lifecycle of a recording session: ACTIVE → CLOSED.
 * Tracks snapshot count and captures executor config fields at session start.
 *
 * <p>Individual config fields are stored to avoid importing
 * executor types into the metrics package (respecting the metrics boundary).
 */
public final class RecordingSession {

    private final String sessionId;
    private final String runId;
    private final int corePoolSize;
    private final int maximumPoolSize;
    private final int queueCapacity;
    private final long keepAliveTime;
    private final String keepAliveTimeUnit;
    private final String threadMode;
    private final Instant startedAt;
    private volatile Instant closedAt;
    private volatile int snapshotCount;
    private volatile SessionStatus status;

    public RecordingSession(String sessionId, String runId,
                            int corePoolSize, int maximumPoolSize, int queueCapacity,
                            long keepAliveTime, String keepAliveTimeUnit,
                            String threadMode) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        this.runId = Objects.requireNonNull(runId, "runId must not be null");
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.queueCapacity = queueCapacity;
        this.keepAliveTime = keepAliveTime;
        this.keepAliveTimeUnit = Objects.requireNonNull(keepAliveTimeUnit, "keepAliveTimeUnit must not be null");
        this.threadMode = Objects.requireNonNull(threadMode, "threadMode must not be null");
        this.startedAt = Instant.now();
        this.snapshotCount = 0;
        this.status = SessionStatus.ACTIVE;
    }

    public void incrementSnapshotCount() {
        if (status != SessionStatus.ACTIVE) {
            throw new IllegalStateException("session is not ACTIVE");
        }
        snapshotCount++;
    }

    public RecordingSessionMetadata close() {
        if (status != SessionStatus.ACTIVE) {
            throw new IllegalStateException("session is already CLOSED");
        }
        this.closedAt = Instant.now();
        this.status = SessionStatus.CLOSED;
        return new RecordingSessionMetadata(
                sessionId, runId,
                corePoolSize, maximumPoolSize, queueCapacity,
                keepAliveTime, keepAliveTimeUnit, threadMode,
                startedAt, closedAt, snapshotCount, status);
    }

    public String sessionId() { return sessionId; }
    public String runId() { return runId; }
    public int corePoolSize() { return corePoolSize; }
    public int maximumPoolSize() { return maximumPoolSize; }
    public int queueCapacity() { return queueCapacity; }
    public long keepAliveTime() { return keepAliveTime; }
    public String keepAliveTimeUnit() { return keepAliveTimeUnit; }
    public String threadMode() { return threadMode; }
    public Instant startedAt() { return startedAt; }
    public Instant closedAt() { return closedAt; }
    public int snapshotCount() { return snapshotCount; }
    public SessionStatus status() { return status; }
}
