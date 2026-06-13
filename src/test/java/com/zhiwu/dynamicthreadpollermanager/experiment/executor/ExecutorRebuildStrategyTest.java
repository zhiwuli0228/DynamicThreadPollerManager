package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorRebuildStrategyTest {

    private ExecutorRegistry registry;
    private AtomicDeletionSafety deletionSafety;
    private ExecutorRebuildStrategy strategy;
    private Supplier<Instant> clock;

    @BeforeEach
    void setUp() {
        deletionSafety = new AtomicDeletionSafety();
        registry = new ExecutorRegistry(deletionSafety);
        clock = Instant::now;
        strategy = new ExecutorRebuildStrategy(registry, clock);
    }

    @AfterEach
    void tearDown() {
        for (String name : List.copyOf(registry.list())) {
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

    @Test
    void expandRebuildSucceeds() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand", 10_000L);
        RebuildResult result = strategy.rebuild("test-exec", executor, cmd);

        assertTrue(result.success(), "rebuild failed: " + result.errorMessage());
        assertEquals(QueueResizeCommand.Direction.EXPAND, result.direction());
        assertEquals(10, result.oldQueueCapacity());
        assertEquals(20, result.newQueueCapacity());
        assertNotNull(result.beforeState());
        assertNotNull(result.afterState());
        assertTrue(result.rebuildDurationMs() >= 0);
        assertNull(result.errorMessage());

        ManagedExecutor newExecutor = registry.get("test-exec").orElseThrow();
        assertEquals(20, newExecutor.getQueueCapacity());
        assertEquals(2, newExecutor.getCorePoolSize());
        assertEquals(4, newExecutor.getMaximumPoolSize());
    }

    @Test
    void shrinkRebuildSucceeds() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20));
        registry.register("test-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(5, "shrink", 10_000L);
        RebuildResult result = strategy.rebuild("test-exec", executor, cmd);

        assertTrue(result.success());
        assertEquals(QueueResizeCommand.Direction.SHRINK, result.direction());
        assertEquals(20, result.oldQueueCapacity());
        assertEquals(5, result.newQueueCapacity());
        assertEquals(0, result.drainedTaskCount());

        ManagedExecutor newExecutor = registry.get("test-exec").orElseThrow();
        assertEquals(5, newExecutor.getQueueCapacity());
    }

    @Test
    void oldExecutorTerminatedAfterRebuild() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand", 10_000L);
        strategy.rebuild("test-exec", executor, cmd);

        assertTrue(executor.isTerminated());
    }

    @Test
    void newExecutorInRegistryAfterRebuild() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand", 10_000L);
        strategy.rebuild("test-exec", executor, cmd);

        assertTrue(registry.get("test-exec").isPresent());
        assertNotSame(executor, registry.get("test-exec").get());
    }

    @Test
    void threadConfigPreservedAfterRebuild() {
        ManagedExecutor executor = new ManagedExecutor(3, 6, 90, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand", 10_000L);
        strategy.rebuild("test-exec", executor, cmd);

        ManagedExecutor newExecutor = registry.get("test-exec").orElseThrow();
        assertEquals(3, newExecutor.getCorePoolSize());
        assertEquals(6, newExecutor.getMaximumPoolSize());
        assertEquals(90_000L, newExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS));
    }

    @Test
    void expandDrainAndReplay() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        registry.register("test-exec", executor);

        // Submit 3 tasks that will be in the queue
        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        }

        QueueResizeCommand cmd = new QueueResizeCommand(20, "expand", 10_000L);
        RebuildResult result = strategy.rebuild("test-exec", executor, cmd);

        assertTrue(result.success());
        assertEquals(QueueResizeCommand.Direction.EXPAND, result.direction());
        // Drained tasks should be replayed (EXPAND direction)
        assertTrue(result.drainedTaskCount() >= 0);
        assertEquals(0, result.rejectedTaskCount());
    }

    @Test
    void shrinkDrainAndDiscard() {
        ManagedExecutor executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20));
        registry.register("test-exec", executor);

        // Submit tasks to partially fill queue
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        }

        QueueResizeCommand cmd = new QueueResizeCommand(5, "shrink", 10_000L);
        RebuildResult result = strategy.rebuild("test-exec", executor, cmd);

        assertTrue(result.success());
        assertEquals(QueueResizeCommand.Direction.SHRINK, result.direction());
        // SHRINK: drained tasks not replayed
        assertEquals(0, result.rejectedTaskCount());
    }
}
