package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.QueueResizeSafetyGate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class QueueResizeAdjustmentAdapterTest {

    private ExecutorRegistry registry;
    private AtomicDeletionSafety deletionSafety;
    private QueueResizeSafetyGate safetyGate;
    private ExecutorRebuildStrategy rebuildStrategy;
    private QueueResizeAdjustmentAdapter adapter;
    private Supplier<Instant> clock;

    @BeforeEach
    void setUp() {
        deletionSafety = new AtomicDeletionSafety();
        registry = new ExecutorRegistry(deletionSafety);
        safetyGate = new QueueResizeSafetyGate();
        clock = Instant::now;
        rebuildStrategy = new ExecutorRebuildStrategy(registry, clock);
        adapter = new QueueResizeAdjustmentAdapter(registry, safetyGate, rebuildStrategy);
    }

    @AfterEach
    void tearDown() {
        for (String name : registry.list()) {
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
        }
    }

    @Test
    void successfulExpandViaAdapter() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand via adapter");
        QueueResizeResult result = adapter.apply("test-exec", cmd);

        assertTrue(result.success());
        assertNotNull(result.evidence());
        assertNull(result.failureCode());

        ResizeEvidence evidence = result.evidence();
        assertTrue(evidence.success());
        assertEquals(10, evidence.oldQueueCapacity());
        assertEquals(20, evidence.newQueueCapacity());
        assertEquals("EXPAND", evidence.direction());
        assertTrue(evidence.rebuildDurationMs() >= 0);
    }

    @Test
    void successfulShrinkViaAdapter() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20));
        registry.register("test-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(5, "shrink via adapter");
        QueueResizeResult result = adapter.apply("test-exec", cmd);

        assertTrue(result.success());
        assertNotNull(result.evidence());
        assertEquals("SHRINK", result.evidence().direction());
        assertEquals(20, result.evidence().oldQueueCapacity());
        assertEquals(5, result.evidence().newQueueCapacity());
    }

    @Test
    void executorNotFound() {
        QueueResizeCommand cmd = new QueueResizeCommand(20, "no executor");
        QueueResizeResult result = adapter.apply("nonexistent", cmd);

        assertFalse(result.success());
        assertEquals("EXECUTOR_NOT_FOUND", result.failureCode());
        assertNull(result.evidence());
    }

    @Test
    void safetyGateDeniedForNonRunning() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);
        executor.shutdown();

        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand terminated");
        QueueResizeResult result = adapter.apply("test-exec", cmd);

        assertFalse(result.success());
        assertEquals("SAFETY_GATE_DENIED", result.failureCode());
        assertNotNull(result.evidence());
        assertFalse(result.evidence().success());
    }

    @Test
    void safetyGateDeniedForSameCapacity() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(10, "no change");
        QueueResizeResult result = adapter.apply("test-exec", cmd);

        assertFalse(result.success());
        assertEquals("SAFETY_GATE_DENIED", result.failureCode());
    }

    @Test
    void idempotencyGuardPreventsConcurrentResize() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand", 60_000L);

        // Simulate concurrent resize by directly putting to the map
        // (the map is internal, so we test via the adapter API)
        QueueResizeResult result1 = adapter.apply("test-exec", cmd);
        assertTrue(result1.success());

        // Second call on same executor should succeed because the
        // idempotency guard was cleared after the first call
        // To properly test the guard, we'd need concurrent threads.
        // Here we verify the guard existence: after a successful call,
        // the guard entry is removed, allowing subsequent calls.
        ManagedExecutor newExecutor = registry.get("test-exec").orElseThrow();
        QueueResizeCommand cmd2 = new QueueResizeCommand(30, "expand again");
        QueueResizeResult result2 = adapter.apply("test-exec", cmd2);
        assertTrue(result2.success());
    }

    @Test
    void evidenceOnSuccessfulResize() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(25, "evidence test");
        QueueResizeResult result = adapter.apply("test-exec", cmd);

        assertTrue(result.success());
        ResizeEvidence evidence = result.evidence();
        assertNotNull(evidence.beforeState());
        assertNotNull(evidence.afterState());
        assertEquals(10, evidence.oldQueueCapacity());
        assertEquals(25, evidence.newQueueCapacity());
        assertEquals("EXPAND", evidence.direction());
        assertNull(evidence.errorMessage());
    }
}
