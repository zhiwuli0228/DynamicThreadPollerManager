package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import java.util.Optional;

public record QueueResizeCommand(
        int targetQueueCapacity,
        String resizeReason,
        long timeoutMs) {

    public enum Direction { EXPAND, SHRINK, NO_CHANGE }

    public QueueResizeCommand {
        if (targetQueueCapacity <= 0) {
            throw new IllegalArgumentException(
                    "targetQueueCapacity must be > 0, got " + targetQueueCapacity);
        }
        if (timeoutMs < 0) {
            throw new IllegalArgumentException(
                    "timeoutMs must be >= 0, got " + timeoutMs);
        }
    }

    public QueueResizeCommand(int targetQueueCapacity, String resizeReason) {
        this(targetQueueCapacity, resizeReason, 30_000L);
    }

    public static Optional<QueueResizeCommand> fromCurrent(
            int currentCapacity, int newCapacity, String reason) {
        if (newCapacity == currentCapacity) {
            return Optional.empty();
        }
        return Optional.of(new QueueResizeCommand(newCapacity, reason));
    }

    public Direction direction(int currentCapacity) {
        if (targetQueueCapacity > currentCapacity) {
            return Direction.EXPAND;
        }
        if (targetQueueCapacity < currentCapacity) {
            return Direction.SHRINK;
        }
        return Direction.NO_CHANGE;
    }
}
