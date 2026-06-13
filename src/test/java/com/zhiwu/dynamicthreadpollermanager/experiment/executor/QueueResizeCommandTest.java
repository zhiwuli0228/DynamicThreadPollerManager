package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class QueueResizeCommandTest {

    @Test
    void validCommandCreation() {
        QueueResizeCommand cmd = new QueueResizeCommand(20, "test resize", 15_000L);
        assertEquals(20, cmd.targetQueueCapacity());
        assertEquals("test resize", cmd.resizeReason());
        assertEquals(15_000L, cmd.timeoutMs());
    }

    @Test
    void defaultTimeoutIs30Seconds() {
        QueueResizeCommand cmd = new QueueResizeCommand(10, "default timeout");
        assertEquals(30_000L, cmd.timeoutMs());
    }

    @Test
    void zeroCapacityThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new QueueResizeCommand(0, "zero", 10_000L));
    }

    @Test
    void negativeCapacityThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new QueueResizeCommand(-1, "negative", 10_000L));
    }

    @Test
    void negativeTimeoutThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new QueueResizeCommand(10, "bad timeout", -1L));
    }

    @Test
    void fromCurrentSameCapacityReturnsEmpty() {
        Optional<QueueResizeCommand> result = QueueResizeCommand.fromCurrent(10, 10, "no-op");
        assertTrue(result.isEmpty());
    }

    @Test
    void fromCurrentDifferentCapacityReturnsCommand() {
        Optional<QueueResizeCommand> result = QueueResizeCommand.fromCurrent(10, 20, "expand");
        assertTrue(result.isPresent());
        assertEquals(20, result.get().targetQueueCapacity());
        assertEquals("expand", result.get().resizeReason());
    }

    @Test
    void directionExpand() {
        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand");
        assertEquals(QueueResizeCommand.Direction.EXPAND, cmd.direction(10));
    }

    @Test
    void directionShrink() {
        QueueResizeCommand cmd = new QueueResizeCommand(5, "shrink");
        assertEquals(QueueResizeCommand.Direction.SHRINK, cmd.direction(20));
    }

    @Test
    void directionNoChange() {
        QueueResizeCommand cmd = new QueueResizeCommand(10, "same");
        assertEquals(QueueResizeCommand.Direction.NO_CHANGE, cmd.direction(10));
    }

    @Test
    void resizeReasonCanBeNull() {
        QueueResizeCommand cmd = new QueueResizeCommand(15, null);
        assertNull(cmd.resizeReason());
    }
}
