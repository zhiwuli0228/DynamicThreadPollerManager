package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorRegistryTest {

    private ExecutorRegistry registry;
    private AtomicDeletionSafety deletionSafety;
    private ManagedExecutor executor;

    @BeforeEach
    void setUp() {
        deletionSafety = new AtomicDeletionSafety();
        registry = new ExecutorRegistry(deletionSafety);
        executor = new ManagedExecutor(2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (!executor.isTerminated()) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void registerAndGetShouldRoundTrip() {
        registry.register("test-executor", executor);
        assertTrue(registry.get("test-executor").isPresent());
        assertSame(executor, registry.get("test-executor").get());
    }

    @Test
    void getNonExistentShouldReturnEmpty() {
        assertTrue(registry.get("non-existent").isEmpty());
    }

    @Test
    void duplicateRegistrationShouldThrow() {
        registry.register("test-executor", executor);
        ManagedExecutor another = new ManagedExecutor(1, 2, 30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(5));
        assertThrows(IllegalArgumentException.class, () ->
                registry.register("test-executor", another));
    }

    @Test
    void listShouldReturnUnmodifiableSnapshot() {
        registry.register("test-executor", executor);
        List<String> names = registry.list();
        assertEquals(1, names.size());
        assertEquals("test-executor", names.get(0));
        assertThrows(UnsupportedOperationException.class, () -> names.add("another"));
    }

    @Test
    void sizeShouldReflectRegistryState() {
        assertEquals(0, registry.size());
        registry.register("test-executor", executor);
        assertEquals(1, registry.size());
    }

    @Test
    void removeShouldReturnFalseWhenDeletionSafetyBlocks() {
        registry.register("test-executor", executor);
        deletionSafety.acquire("test-executor");
        assertFalse(registry.remove("test-executor"));
        assertTrue(registry.get("test-executor").isPresent());
    }

    @Test
    void removeShouldSucceedWhenDeletionSafetyAllows() {
        registry.register("test-executor", executor);
        executor.shutdown();
        assertTrue(registry.remove("test-executor"));
        assertTrue(registry.get("test-executor").isEmpty());
    }

    @Test
    void removeDoesNotAutoShutdown() {
        registry.register("test-executor", executor);
        assertFalse(executor.isShutdown());
        assertFalse(registry.remove("test-executor")); // blocked — not terminated
        assertFalse(executor.isShutdown());
    }

    @Test
    void concurrentRegisterAndGetShouldBeSafe() throws Exception {
        int threads = 10;
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final String name = "executor-" + i;
            new Thread(() -> {
                ManagedExecutor exec = new ManagedExecutor(1, 2, 30, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(5));
                registry.register(name, exec);
                assertTrue(registry.get(name).isPresent());
                exec.shutdown();
                latch.countDown();
            }).start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(threads, registry.size());
    }
}
