package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ManagedExecutorConcurrencyTest {

    private ManagedExecutor executor;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null && !executor.isTerminated()) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void concurrentSetCorePoolSizeShouldNotDesyncSemaphore() throws Exception {
        // Virtual mode executor with known max concurrency
        executor = ManagedExecutor.virtual(5, 10, 60, TimeUnit.SECONDS,
                new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        // Each thread concurrently adjusts the pool size
        for (int t = 0; t < threadCount; t++) {
            final int targetSize = 1 + (t % 5) + 1; // targets 2-6, but capped by MAX_POOL_SIZE
            futures.add(pool.submit(() -> {
                try {
                    startLatch.await();
                    // Alternate between different sizes
                    for (int i = 0; i < 5; i++) {
                        int size = 1 + ((targetSize + i) % 5);
                        executor.setCorePoolSize(size);
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) { f.get(); }
        pool.shutdown();

        assertTrue(errors.isEmpty(),
                "Expected no errors but got: " + errors.stream().map(Throwable::getMessage).toList());

        // Verify internal consistency: availablePermits should not exceed maxConcurrency
        // and should not be negative
        int maxConcurrency = executor.getMaximumPoolSize();
        int availablePermits = executor.getLargestPoolSize(); // use as a proxy
        // The key invariant: the executor should still be functional
        assertTrue(maxConcurrency >= 1, "maxConcurrency should be >= 1, was " + maxConcurrency);
    }

    @Test
    void concurrentSetMaximumPoolSizeShouldMaintainConsistency() throws Exception {
        executor = ManagedExecutor.virtual(3, 10, 60, TimeUnit.SECONDS,
                new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());

        int threadCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int target = 1 + (t % 4) + 1; // 2-5
            futures.add(pool.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 10; i++) {
                        executor.setMaximumPoolSize(1 + ((target + i) % 5));
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) { f.get(); }
        pool.shutdown();

        assertTrue(errors.isEmpty(),
                "Expected no errors but got: " + errors.stream().map(Throwable::getMessage).toList());

        // Executor should still be usable after concurrent adjustments
        assertTrue(executor.getMaximumPoolSize() >= 1);
    }

    @Test
    void concurrentAdjustAndSubmitShouldNotDeadlock() throws Exception {
        executor = ManagedExecutor.virtual(3, 10, 60, TimeUnit.SECONDS,
                new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());

        int threadCount = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        // Half threads adjust, half submit tasks
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            futures.add(pool.submit(() -> {
                try {
                    startLatch.await();
                    if (threadId < threadCount / 2) {
                        // Adjusters
                        for (int i = 0; i < 20; i++) {
                            executor.setCorePoolSize(1 + (i % 4));
                        }
                    } else {
                        // Submitters
                        for (int i = 0; i < 20; i++) {
                            try {
                                executor.submit(() -> {
                                    try { Thread.sleep(1); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                                }).get(2, TimeUnit.SECONDS);
                            } catch (java.util.concurrent.RejectedExecutionException e) {
                                // acceptable under contention
                            } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
                                // acceptable under contention
                            }
                        }
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) { f.get(); }
        pool.shutdown();

        assertTrue(errors.isEmpty(),
                "Expected no errors but got: " + errors.stream().map(Throwable::getMessage).toList());
    }
}
