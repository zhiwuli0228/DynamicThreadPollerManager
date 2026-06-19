package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedEvidenceRecorderConcurrencyTest {

    private static final Instant T0 = Instant.parse("2026-06-05T10:00:00Z");

    private Path tempDir;
    private FileBackedEvidenceRecorder recorder;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("fber-concurrency-");
        recorder = new FileBackedEvidenceRecorder(tempDir, "concurrency-v1");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    });
        }
    }

    @Test
    void shouldNotLoseDataUnderConcurrentWrites() throws Exception {
        String runId = "concurrent-file-run";
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
                        Instant ts = T0.plusMillis(threadId * writesPerThread + i);
                        PressureSnapshot ps = new PressureSnapshot(
                                ts, threadId + 1, threadId + 1,
                                i, i * 10L, 0.5);
                        RuntimeObservation obs = new RuntimeObservation(
                                ts,
                                MetricValue.present(threadId + 1),
                                MetricValue.present(i),
                                MetricValue.present(0.5));
                        recorder.record(new ObservedSnapshot(runId, ps, obs));
                    }
                } catch (Exception e) {
                    synchronized (failures) {
                        failures.add(e);
                    }
                }
            }, "file-writer-" + threadId);
            thread.setDaemon(true);
            threads.add(thread);
        }

        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join(10_000);
        }

        assertTrue(failures.isEmpty(),
                "Concurrent file writes had failures: " + failures);

        List<ObservedSnapshot> snapshots = recorder.snapshots(runId);
        assertEquals(threadCount * writesPerThread, snapshots.size(),
                "Expected " + (threadCount * writesPerThread) + " snapshots, got " + snapshots.size());
    }
}
