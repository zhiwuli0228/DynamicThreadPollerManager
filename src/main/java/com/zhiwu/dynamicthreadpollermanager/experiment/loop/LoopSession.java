package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable record of a complete loop session from start to end.
 */
public record LoopSession(
        String sessionId,
        LoopConfig loopConfig,
        Instant startTime,
        Optional<Instant> endTime,
        int adjustmentCount,
        int iterationCount,
        LoopState finalState,
        String summary
) {
    public LoopSession {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        if (sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        Objects.requireNonNull(loopConfig, "loopConfig must not be null");
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
        Objects.requireNonNull(finalState, "finalState must not be null");
        if (adjustmentCount < 0) {
            throw new IllegalArgumentException("adjustmentCount must be >= 0, was " + adjustmentCount);
        }
        if (iterationCount < 0) {
            throw new IllegalArgumentException("iterationCount must be >= 0, was " + iterationCount);
        }
        if (iterationCount < adjustmentCount) {
            throw new IllegalArgumentException(
                    "iterationCount must be >= adjustmentCount, was iterationCount="
                            + iterationCount + ", adjustmentCount=" + adjustmentCount);
        }
        Objects.requireNonNull(summary, "summary must not be null");
    }

    public static LoopSession started(LoopConfig config) {
        return new LoopSession(
                UUID.randomUUID().toString(),
                config,
                Instant.now(),
                Optional.empty(),
                0, 0,
                LoopState.RUNNING,
                "session started");
    }

    public LoopSession ended(LoopState finalState, int adjustments, int iterations, String summary) {
        return new LoopSession(
                this.sessionId, this.loopConfig, this.startTime,
                Optional.of(Instant.now()), adjustments, iterations, finalState, summary);
    }
}
