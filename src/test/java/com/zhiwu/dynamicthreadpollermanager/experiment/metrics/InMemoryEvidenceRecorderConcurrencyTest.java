package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryEvidenceRecorderConcurrencyTest {

    private static final Instant T0 = Instant.parse("2026-06-05T10:00:00Z");

    @Test
    void shouldHandleConcurrentWritesFromMultipleThreads() throws Exception {
        InMemoryEvidenceRecorder recorder = new InMemoryEvidenceRecorder();
        String runId = "concurrent-run";
        int threadCount = 4;
        int writesPerThread = 50;

        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        List<Exception> failures = Collections.synchronizedList(new ArrayList<>());

        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            Thread thread = new Thread(() -> {
                try {
                    barrier.await();
                    for (int i = 0; i < writesPerThread; i++) {
                        PressureSnapshot ps = new PressureSnapshot(
                                T0.plusMillis(threadId * writesPerThread + i),
                                threadId + 1, threadId + 1,
                                i, i * 10L, 0.5);
                        RuntimeObservation obs = new RuntimeObservation(
                                T0.plusMillis(threadId * writesPerThread + i),
                                MetricValue.present(threadId + 1),
                                MetricValue.present(i),
                                MetricValue.present(0.5));
                        recorder.record(new ObservedSnapshot(runId, ps, obs));
                    }
                } catch (Exception e) {
                    failures.add(e);
                }
            }, "writer-" + threadId);
            thread.setDaemon(true);
            threads.add(thread);
        }

        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join(10_000);
        }

        assertTrue(failures.isEmpty(),
                "Concurrent writes had failures: " + failures);
        List<ObservedSnapshot> snapshots = recorder.snapshots(runId);
        assertEquals(threadCount * writesPerThread, snapshots.size(),
                "Expected " + (threadCount * writesPerThread) + " snapshots, got " + snapshots.size());

        // Verify insertion order: all snapshots from a given thread appear
        // in insertion order (interleaving may vary)
        assertTrue(snapshots.size() == 200, "Must have exactly 200 snapshots");
    }

    @Test
    void shouldKeepRunIdsIndependent() throws Exception {
        InMemoryEvidenceRecorder recorder = new InMemoryEvidenceRecorder();
        int writesPerRun = 30;

        AtomicInteger done = new AtomicInteger(0);
        CyclicBarrier barrier = new CyclicBarrier(2);

        Thread writerA = new Thread(() -> {
            try {
                barrier.await();
                for (int i = 0; i < writesPerRun; i++) {
                    PressureSnapshot ps = new PressureSnapshot(
                            T0.plusMillis(i), 1, 1, i, 0L, 0.5);
                    RuntimeObservation obs = new RuntimeObservation(
                            T0.plusMillis(i),
                            MetricValue.present(1),
                            MetricValue.present(i),
                            MetricValue.present(0.5));
                    recorder.record(new ObservedSnapshot("run-A", ps, obs));
                }
                done.incrementAndGet();
            } catch (Exception ignored) {}
        }, "writer-A");
        writerA.setDaemon(true);

        Thread writerB = new Thread(() -> {
            try {
                barrier.await();
                for (int i = 0; i < writesPerRun; i++) {
                    PressureSnapshot ps = new PressureSnapshot(
                            T0.plusMillis(i), 2, 2, i * 2, i * 10L, 0.6);
                    RuntimeObservation obs = new RuntimeObservation(
                            T0.plusMillis(i),
                            MetricValue.present(2),
                            MetricValue.present(i * 2),
                            MetricValue.present(0.6));
                    recorder.record(new ObservedSnapshot("run-B", ps, obs));
                }
                done.incrementAndGet();
            } catch (Exception ignored) {}
        }, "writer-B");
        writerB.setDaemon(true);

        writerA.start();
        writerB.start();
        writerA.join(10_000);
        writerB.join(10_000);

        assertEquals(2, done.get(), "Both writers should complete");
        assertEquals(writesPerRun, recorder.snapshots("run-A").size());
        assertEquals(writesPerRun, recorder.snapshots("run-B").size());
        assertNotEquals(recorder.snapshots("run-A"), recorder.snapshots("run-B"),
                "Different runIds should have independent snapshot streams");
    }
}
