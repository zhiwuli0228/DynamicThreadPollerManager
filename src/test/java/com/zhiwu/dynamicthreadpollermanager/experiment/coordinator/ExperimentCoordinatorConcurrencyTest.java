package com.zhiwu.dynamicthreadpollermanager.experiment.coordinator;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.ExperimentRun;
import com.zhiwu.dynamicthreadpollermanager.experiment.state.RunState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentCoordinatorConcurrencyTest {

    @Test
    void concurrentStartRunShouldNotCorruptState() throws Exception {
        ExperimentCoordinator coordinator = new ExperimentCoordinator();
        int threadCount = 20;
        int runsPerThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            futures.add(pool.submit(() -> {
                for (int i = 0; i < runsPerThread; i++) {
                    try {
                        ExperimentRun run = coordinator.createRun("s", "p");
                        coordinator.startRun(run.runId());
                        ExperimentRun started = coordinator.getRun(run.runId());
                        assertEquals(RunState.RUNNING, started.state(),
                                "run " + run.runId() + " should be RUNNING");
                    } catch (Throwable e) {
                        errors.add(e);
                    }
                }
            }));
        }

        for (Future<?> f : futures) { f.get(); }
        pool.shutdown();
        assertTrue(errors.isEmpty(),
                "Expected no errors but got: " + errors.stream().map(Throwable::getMessage).toList());
    }

    @Test
    void concurrentTransitionOnSameRunShouldSerialize() throws Exception {
        ExperimentCoordinator coordinator = new ExperimentCoordinator();
        ExperimentRun run = coordinator.createRun("s", "p");
        coordinator.startRun(run.runId());

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger illegalCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        // All threads try to stop the same run concurrently
        for (int t = 0; t < threadCount; t++) {
            futures.add(pool.submit(() -> {
                try {
                    startLatch.await();
                    coordinator.stopRun(run.runId());
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    // Expected: some threads will see already-stopped state
                    illegalCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        startLatch.countDown(); // release all threads simultaneously
        for (Future<?> f : futures) { f.get(); }
        pool.shutdown();

        // Exactly one thread should succeed; the rest should get IllegalStateException
        assertEquals(1, successCount.get(), "exactly one thread should succeed");
        assertEquals(threadCount - 1, illegalCount.get(), "remaining threads should see illegal transition");

        // Final state should be STOPPED
        ExperimentRun finalRun = coordinator.getRun(run.runId());
        assertEquals(RunState.STOPPED, finalRun.state());
    }

    @Test
    void concurrentFullLifecycleShouldNotLoseState() throws Exception {
        ExperimentCoordinator coordinator = new ExperimentCoordinator();
        int runCount = 100;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < runCount; i++) {
            futures.add(pool.submit(() -> {
                try {
                    startLatch.await();
                    ExperimentRun run = coordinator.createRun("s", "p");
                    coordinator.startRun(run.runId());
                    coordinator.stopRun(run.runId());
                    coordinator.finalizeRun(run.runId());
                    ExperimentRun finalized = coordinator.getRun(run.runId());
                    assertEquals(RunState.FINALIZED, finalized.state());
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
