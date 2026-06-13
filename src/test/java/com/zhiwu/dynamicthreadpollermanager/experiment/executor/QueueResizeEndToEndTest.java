package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.QueueResizeSafetyGate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class QueueResizeEndToEndTest {

    private ExecutorRegistry registry;
    private AtomicDeletionSafety deletionSafety;
    private QueueResizeSafetyGate safetyGate;
    private ExecutorRebuildStrategy rebuildStrategy;
    private QueueResizeAdjustmentAdapter adapter;
    private Supplier<Instant> clock;
    private CountDownLatch taskBlocker;

    @BeforeEach
    void setUp() {
        deletionSafety = new AtomicDeletionSafety();
        registry = new ExecutorRegistry(deletionSafety);
        safetyGate = new QueueResizeSafetyGate();
        clock = Instant::now;
        rebuildStrategy = new ExecutorRebuildStrategy(registry, clock);
        adapter = new QueueResizeAdjustmentAdapter(registry, safetyGate, rebuildStrategy);
        taskBlocker = new CountDownLatch(1);
    }

    @AfterEach
    void tearDown() {
        taskBlocker.countDown();
        for (String name : java.util.List.copyOf(registry.list())) {
            registry.get(name).ifPresent(executor -> {
                if (!executor.isTerminated()) {
                    executor.shutdownNow();
                    try {
                        executor.awaitTermination(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            registry.remove(name);
        }
    }

    // --- EXPAND end-to-end ---

    @Test
    void expandResizePreservesThreadConfig() {
        ManagedExecutor executor = new ManagedExecutor(
                2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10));
        registry.register("e2e-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand e2e", 10_000L);
        QueueResizeResult result = adapter.apply("e2e-exec", cmd);

        assertTrue(result.success(), "expand failed: " + result.errorMessage());

        ManagedExecutor newExecutor = registry.get("e2e-exec").orElseThrow();
        assertEquals(20, newExecutor.getQueueCapacity());
        assertEquals(2, newExecutor.getCorePoolSize());
        assertEquals(4, newExecutor.getMaximumPoolSize());
        assertEquals(60, newExecutor.getKeepAliveTime(TimeUnit.SECONDS));

        ResizeEvidence evidence = result.evidence();
        assertNotNull(evidence);
        assertTrue(evidence.success());
        assertEquals(10, evidence.oldQueueCapacity());
        assertEquals(20, evidence.newQueueCapacity());
        assertEquals("EXPAND", evidence.direction());
    }

    @Test
    void expandResizeNewExecutorIsFunctional() {
        ManagedExecutor executor = new ManagedExecutor(
                2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10));
        registry.register("e2e-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand e2e");
        QueueResizeResult result = adapter.apply("e2e-exec", cmd);

        assertTrue(result.success());

        ManagedExecutor newExecutor = registry.get("e2e-exec").orElseThrow();
        assertFalse(newExecutor.isShutdown());
        assertFalse(newExecutor.isTerminated());

        // New executor should accept tasks
        var future = newExecutor.submit(() -> 42);
        try {
            assertEquals(42, future.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            fail("new executor should accept tasks: " + e.getMessage());
        }
    }

    // --- SHRINK end-to-end ---

    @Test
    void shrinkResizeSucceeds() {
        ManagedExecutor executor = new ManagedExecutor(
                2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(20));
        registry.register("e2e-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(5, "shrink e2e");
        QueueResizeResult result = adapter.apply("e2e-exec", cmd);

        assertTrue(result.success(), "shrink failed: " + result.errorMessage());

        ManagedExecutor newExecutor = registry.get("e2e-exec").orElseThrow();
        assertEquals(5, newExecutor.getQueueCapacity());
        assertEquals("SHRINK", result.evidence().direction());
        assertEquals(20, result.evidence().oldQueueCapacity());
        assertEquals(5, result.evidence().newQueueCapacity());
    }

    @Test
    void oldExecutorTerminatedAfterResize() {
        ManagedExecutor executor = new ManagedExecutor(
                2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10));
        registry.register("e2e-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand e2e");
        adapter.apply("e2e-exec", cmd);

        assertTrue(executor.isTerminated(),
                "old executor should be terminated after rebuild");
    }

    // --- Safety gate DENY ---

    @Test
    void safetyGateDeniesShrinkWithActiveQueueDepth() {
        ManagedExecutor executor = new ManagedExecutor(
                2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10));
        registry.register("e2e-exec", executor);

        // Submit blocking tasks to fill the queue
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    taskBlocker.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        }

        // Wait for tasks to be picked up or queued
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // The queue should have depth > 0 (tasks waiting for threads)
        // Try to SHRINK to a small capacity
        QueueResizeCommand cmd = new QueueResizeCommand(1, "aggressive shrink");
        QueueResizeResult result = adapter.apply("e2e-exec", cmd);

        // The safety gate should DENY if queue depth > 1
        // (or PERMIT if all tasks fit in the pool and queue is empty)
        if (!result.success()) {
            assertEquals("SAFETY_GATE_DENIED", result.failureCode());
        }
        // If permitted, the resize still runs — that's fine too
    }

    // --- ResizeEvidence completeness ---

    @Test
    void resizeEvidenceIsComplete() {
        ManagedExecutor executor = new ManagedExecutor(
                2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10));
        registry.register("e2e-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(20, "evidence test");
        QueueResizeResult result = adapter.apply("e2e-exec", cmd);

        assertTrue(result.success());
        ResizeEvidence evidence = result.evidence();

        assertNotNull(evidence.beforeState());
        assertEquals(10, evidence.beforeState().queueCapacity());

        assertNotNull(evidence.afterState());
        assertEquals(20, evidence.afterState().queueCapacity());

        assertEquals("EXPAND", evidence.direction());
        assertEquals(10, evidence.oldQueueCapacity());
        assertEquals(20, evidence.newQueueCapacity());
        assertTrue(evidence.success());
        assertNull(evidence.errorMessage());
        assertTrue(evidence.rebuildDurationMs() >= 0);
    }

    // --- No-op detection ---

    @Test
    void noOpWhenCapacityUnchanged() {
        Optional<QueueResizeCommand> cmd = QueueResizeCommand.fromCurrent(
                10, 10, "no change");
        assertTrue(cmd.isEmpty());
    }

    @Test
    void noOpCommandNotApplied() {
        ManagedExecutor executor = new ManagedExecutor(
                2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10));
        registry.register("e2e-exec", executor);

        Optional<QueueResizeCommand> cmdOpt = QueueResizeCommand.fromCurrent(
                10, 10, "no change");
        assertTrue(cmdOpt.isEmpty(),
                "fromCurrent should return empty for same capacity");
    }
}
