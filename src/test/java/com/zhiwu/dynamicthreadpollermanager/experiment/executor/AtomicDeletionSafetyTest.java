package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AtomicDeletionSafetyTest {

    private AtomicDeletionSafety deletionSafety;
    private ExecutorRegistry registry;
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
    void acquireShouldIncrementRefCount() {
        deletionSafety.acquire("executor");
        assertEquals(1, deletionSafety.referenceCount("executor"));
    }

    @Test
    void releaseShouldDecrementRefCount() {
        deletionSafety.acquire("executor");
        deletionSafety.acquire("executor");
        assertEquals(2, deletionSafety.referenceCount("executor"));
        deletionSafety.release("executor");
        assertEquals(1, deletionSafety.referenceCount("executor"));
    }

    @Test
    void releaseWhenZeroShouldThrow() {
        deletionSafety.acquire("executor");
        deletionSafety.release("executor");
        assertEquals(0, deletionSafety.referenceCount("executor"));
        assertThrows(IllegalStateException.class, () -> deletionSafety.release("executor"));
    }

    @Test
    void releaseNeverAcquiredShouldThrow() {
        assertThrows(IllegalStateException.class, () -> deletionSafety.release("never-acquired"));
    }

    @Test
    void canRemoveShouldReturnFalseWhenRefCountPositive() {
        registry.register("executor", executor);
        deletionSafety.acquire("executor");
        assertFalse(deletionSafety.canRemove("executor", registry));
    }

    @Test
    void canRemoveShouldReturnFalseWhenNotTerminated() {
        registry.register("executor", executor);
        // refCount == 0 but executor is still running
        assertFalse(deletionSafety.canRemove("executor", registry));
    }

    @Test
    void canRemoveShouldReturnTrueWhenRefCountZeroAndTerminated() {
        registry.register("executor", executor);
        executor.shutdown();
        assertTrue(deletionSafety.canRemove("executor", registry));
    }

    @Test
    void canRemoveShouldReturnTrueForUnregisteredName() {
        assertTrue(deletionSafety.canRemove("non-existent", registry));
    }

    @Test
    void referenceCountShouldReturnZeroForUnknownName() {
        assertEquals(0, deletionSafety.referenceCount("unknown"));
    }
}
