package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ResizeEvidenceTest {

    private ExecutorStateSnapshot snapshot(int queueCapacity) {
        return ExecutorStateSnapshot.builder(Instant.now())
                .corePoolSize(2)
                .maximumPoolSize(4)
                .queueCapacity(queueCapacity)
                .build();
    }

    @Test
    void fromRebuildResultSuccess() {
        ExecutorStateSnapshot before = snapshot(10);
        ExecutorStateSnapshot after = snapshot(20);

        RebuildResult rebuildResult = new RebuildResult(
                true, before, after, 150L, 3, 0,
                QueueResizeCommand.Direction.EXPAND, 10, 20, null);

        ResizeEvidence evidence = ResizeEvidence.from(rebuildResult);

        assertTrue(evidence.success());
        assertSame(before, evidence.beforeState());
        assertSame(after, evidence.afterState());
        assertEquals(150L, evidence.rebuildDurationMs());
        assertEquals(3, evidence.drainedTaskCount());
        assertEquals(0, evidence.rejectedTaskCount());
        assertEquals("EXPAND", evidence.direction());
        assertEquals(10, evidence.oldQueueCapacity());
        assertEquals(20, evidence.newQueueCapacity());
        assertNull(evidence.errorMessage());
    }

    @Test
    void fromRebuildResultFailure() {
        ExecutorStateSnapshot before = snapshot(10);

        RebuildResult rebuildResult = new RebuildResult(
                false, before, null, 50L, 2, 2,
                QueueResizeCommand.Direction.EXPAND, 10, 20,
                "Commission failed: something");

        ResizeEvidence evidence = ResizeEvidence.from(rebuildResult);

        assertFalse(evidence.success());
        assertSame(before, evidence.beforeState());
        assertNull(evidence.afterState());
        assertEquals(50L, evidence.rebuildDurationMs());
        assertEquals(2, evidence.drainedTaskCount());
        assertEquals(2, evidence.rejectedTaskCount());
        assertEquals("EXPAND", evidence.direction());
        assertEquals(10, evidence.oldQueueCapacity());
        assertEquals(20, evidence.newQueueCapacity());
        assertEquals("Commission failed: something", evidence.errorMessage());
    }

    @Test
    void shrinkDirection() {
        ExecutorStateSnapshot before = snapshot(20);
        ExecutorStateSnapshot after = snapshot(5);

        RebuildResult rebuildResult = new RebuildResult(
                true, before, after, 100L, 3, 0,
                QueueResizeCommand.Direction.SHRINK, 20, 5, null);

        ResizeEvidence evidence = ResizeEvidence.from(rebuildResult);
        assertEquals("SHRINK", evidence.direction());
        assertEquals(20, evidence.oldQueueCapacity());
        assertEquals(5, evidence.newQueueCapacity());
    }
}
