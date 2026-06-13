package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ManagedExecutorVirtualThreadTest {

    private ManagedExecutor executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    void shouldCreateExecutorWithVirtualThreadMode() {
        executor = ManagedExecutor.virtual(4, 10, 60,
                TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy());
        assertEquals(ThreadMode.VIRTUAL, executor.getThreadMode());
    }

    @Test
    void shouldExecuteCallableOnVirtualThread() throws Exception {
        executor = ManagedExecutor.virtual(4, 10, 60,
                TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy());
        Callable<Boolean> task = () -> Thread.currentThread().isVirtual();
        assertTrue(executor.submit(task).get());
    }

    @Test
    void shouldExecuteMultipleTasksAndCompleteAll() throws Exception {
        executor = ManagedExecutor.virtual(4, 10, 60,
                TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy());
        AtomicInteger count = new AtomicInteger(0);
        int taskCount = 10;
        for (int i = 0; i < taskCount; i++) {
            executor.submit((Runnable) count::incrementAndGet);
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(taskCount, count.get());
    }

    @Test
    void shouldLimitConcurrencyWithSemaphore() throws Exception {
        executor = ManagedExecutor.virtual(2, 10, 60,
                TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy());
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger currentConcurrent = new AtomicInteger(0);
        int taskCount = 10;
        CountDownLatch allDone = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                try {
                    int current = currentConcurrent.incrementAndGet();
                    maxConcurrent.updateAndGet(prev -> Math.max(prev, current));
                    Thread.sleep(50);
                    currentConcurrent.decrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    allDone.countDown();
                }
            });
        }

        assertTrue(allDone.await(10, TimeUnit.SECONDS), "all tasks should complete");
        assertEquals(2, maxConcurrent.get(), "max concurrency should be bounded by semaphore");
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldRejectWhenQueueFull() throws Exception {
        executor = ManagedExecutor.virtual(1, 2, 60,
                TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy());
        executor.pauseDrainer();

        // Fill the queue (capacity 2)
        executor.submit(() -> {});
        executor.submit(() -> {});

        // Queue full — next submit should throw
        assertThrows(RejectedExecutionException.class, () -> {
            executor.submit(() -> {});
        });

        executor.resumeDrainer();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldShutdownGracefully() throws Exception {
        executor = ManagedExecutor.virtual(4, 10, 60,
                TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy());
        executor.submit(() -> {});
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(executor.isTerminated());
    }

    @Test
    void shouldProduceSnapshotWithVirtualThreadMode() {
        executor = ManagedExecutor.virtual(4, 10, 60,
                TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy());
        var snapshot = executor.toSnapshot();
        assertEquals(4, snapshot.corePoolSize());
        assertEquals(4, snapshot.maximumPoolSize());
        assertEquals(10, snapshot.queueCapacity());
    }

    @Test
    void shouldAdjustConcurrencyDynamically() throws Exception {
        executor = ManagedExecutor.virtual(2, 100, 60,
                TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy());
        assertEquals(2, executor.getCorePoolSize());

        executor.setCorePoolSize(5);
        assertEquals(5, executor.getCorePoolSize());

        // Submit tasks and verify they can run with new concurrency limit
        AtomicInteger count = new AtomicInteger(0);
        for (int i = 0; i < 20; i++) {
            executor.submit((Runnable) count::incrementAndGet);
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(20, count.get());
    }

    @Test
    void platformThreadDefaultShouldNotBeVirtual() throws Exception {
        executor = new ManagedExecutor(2, 4, 60,
                TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>(10));
        assertEquals(ThreadMode.PLATFORM, executor.getThreadMode());
        Callable<Boolean> task = () -> Thread.currentThread().isVirtual();
        assertFalse(executor.submit(task).get(), "default executor should use platform threads");
    }

    @Test
    void shouldGetQueueSizeDuringExecution() throws Exception {
        executor = ManagedExecutor.virtual(1, 10, 60,
                TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy());
        executor.pauseDrainer();

        // Tasks queue up in pendingQueue; drainer paused so nothing gets submitted
        executor.submit(() -> {});
        executor.submit(() -> {});

        assertEquals(2, executor.getQueueSize(), "pending tasks should be queued");

        executor.resumeDrainer();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldUnwrapReturnNullForVirtual() {
        executor = ManagedExecutor.virtual(4, 10, 60,
                TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy());
        assertNull(executor.unwrap());
    }
}
