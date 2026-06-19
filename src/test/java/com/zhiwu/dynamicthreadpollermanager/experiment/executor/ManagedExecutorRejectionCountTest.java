package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ManagedExecutorRejectionCountTest {

    @Test
    void shouldCountRejectedTasksWhenCapacityExceeded() throws Exception {
        ManagedExecutor executor = new ManagedExecutor(
                1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1));

        CountDownLatch blocker = new CountDownLatch(1);

        executor.submit(() -> {
            try {
                blocker.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        executor.submit(() -> {});

        // Queue is now full (1 running + 1 queued). Submit more to trigger rejection.
        for (int i = 0; i < 5; i++) {
            try {
                executor.submit(() -> {});
            } catch (Exception e) {
                // expected — AbortPolicy throws RejectedExecutionException
            }
        }

        assertTrue(executor.getRejectedTaskCount() > 0,
                "Expected rejected tasks but got " + executor.getRejectedTaskCount());

        blocker.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldReturnZeroWhenNoRejections() throws Exception {
        ManagedExecutor executor = new ManagedExecutor(
                2, 4, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));

        executor.submit(() -> {});
        executor.submit(() -> {});

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(0L, executor.getRejectedTaskCount());
    }

    @Test
    void getRejectionPolicyShouldReturnOriginalPolicyType() {
        ManagedExecutor executor = new ManagedExecutor(
                1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1));

        assertNotNull(executor.getRejectionPolicy());
        assertTrue(executor.getRejectionPolicy() instanceof java.util.concurrent.ThreadPoolExecutor.AbortPolicy);

        executor.shutdown();
    }

    @Test
    void rejectionCountShouldBeThreadSafe() throws Exception {
        ManagedExecutor executor = new ManagedExecutor(
                1, 1, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1));

        CountDownLatch blocker = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                blocker.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        executor.submit(() -> {});

        // Queue is full. Multiple threads concurrently trigger rejection.
        Thread[] threads = new Thread[4];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    try {
                        executor.submit(() -> {});
                    } catch (Exception e) {
                        // expected rejection
                    }
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertTrue(executor.getRejectedTaskCount() >= 1);

        blocker.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
