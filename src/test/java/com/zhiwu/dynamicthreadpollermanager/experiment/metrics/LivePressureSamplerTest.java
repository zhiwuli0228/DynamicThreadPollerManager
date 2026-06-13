package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LivePressureSamplerTest {

    private final ManagedExecutor executor = ManagedExecutorConfig.defaultConfig().toManagedExecutor();
    private final InMemoryEvidenceRecorder recorder = new InMemoryEvidenceRecorder();
    private final LivePressureSamplerConfig config = new LivePressureSamplerConfig(100, false, "test-session");

    @Test
    void shouldRejectNullExecutor() {
        assertThrows(NullPointerException.class,
                () -> new LivePressureSampler(null, recorder, new DefaultSnapshotAssembler(), config));
    }

    @Test
    void shouldRejectNullRecorder() {
        assertThrows(NullPointerException.class,
                () -> new LivePressureSampler(executor, null, new DefaultSnapshotAssembler(), config));
    }

    @Test
    void shouldRejectNullAssembler() {
        assertThrows(NullPointerException.class,
                () -> new LivePressureSampler(executor, recorder, null, config));
    }

    @Test
    void shouldRejectNullConfig() {
        assertThrows(NullPointerException.class,
                () -> new LivePressureSampler(executor, recorder, new DefaultSnapshotAssembler(), null));
    }

    @Test
    void shouldCreateWithThreeArgConstructor() {
        LivePressureSampler sampler = new LivePressureSampler(executor, recorder, config);
        assertNotNull(sampler);
        assertFalse(sampler.isRunning());
    }

    @Test
    void shouldStartAndStop() {
        LivePressureSampler sampler = new LivePressureSampler(executor, recorder, config);

        assertFalse(sampler.isRunning());
        sampler.start("run-1");
        assertTrue(sampler.isRunning());
        sampler.stop();
        assertFalse(sampler.isRunning());
    }

    @Test
    void shouldRejectNullRunIdOnStart() {
        LivePressureSampler sampler = new LivePressureSampler(executor, recorder, config);
        assertThrows(NullPointerException.class, () -> sampler.start(null));
    }

    @Test
    void shouldThrowOnDoubleStart() {
        LivePressureSampler sampler = new LivePressureSampler(executor, recorder, config);
        sampler.start("run-1");
        try {
            assertThrows(IllegalStateException.class, () -> sampler.start("run-2"));
        } finally {
            sampler.stop();
        }
    }

    @Test
    void shouldStopIdempotent() {
        LivePressureSampler sampler = new LivePressureSampler(executor, recorder, config);

        sampler.start("run-1");
        sampler.stop();
        assertFalse(sampler.isRunning());

        // second stop is a no-op
        sampler.stop();
        assertFalse(sampler.isRunning());
    }

    @Test
    void shouldStopIdempotentWithoutStart() {
        LivePressureSampler sampler = new LivePressureSampler(executor, recorder, config);

        // stop before start is a no-op
        sampler.stop();
        assertFalse(sampler.isRunning());
    }

    @Test
    void shouldSupportManualSample() {
        LivePressureSampler sampler = new LivePressureSampler(executor, recorder, config);
        Instant at = Instant.parse("2026-06-13T10:00:00Z");
        RuntimeObservation observation = new RuntimeObservation(
                at,
                MetricValue.present(4),
                MetricValue.present(8),
                MetricValue.present(20),
                MetricValue.present(100L),
                MetricValue.present(0.5)
        );

        ObservedSnapshot snapshot = sampler.sample("run-1", observation, at);

        assertEquals("run-1", snapshot.runId());
        assertEquals(4, snapshot.snapshot().activeThreads());
        assertEquals(8, snapshot.snapshot().poolSize());
        assertEquals(20, snapshot.snapshot().queueSize());
        assertEquals(100L, snapshot.snapshot().completedTaskCount());
        assertEquals(0.5, snapshot.snapshot().cpuUtilization());

        assertEquals(1, recorder.snapshots("run-1").size());
    }

    @Test
    void shouldRejectNullArgumentsOnSample() {
        LivePressureSampler sampler = new LivePressureSampler(executor, recorder, config);
        Instant at = Instant.parse("2026-06-13T10:00:00Z");
        RuntimeObservation observation = new RuntimeObservation(
                at,
                MetricValue.absent(),
                MetricValue.absent(),
                MetricValue.absent()
        );

        assertThrows(NullPointerException.class,
                () -> sampler.sample(null, observation, at));
        assertThrows(NullPointerException.class,
                () -> sampler.sample("run-1", null, at));
        assertThrows(NullPointerException.class,
                () -> sampler.sample("run-1", observation, null));
    }

    @Test
    @Timeout(30)
    void autonomousSamplingShouldRecordSnapshots() throws Exception {
        LivePressureSampler sampler = new LivePressureSampler(executor, recorder, config);

        // Submit some tasks to create executor activity
        CountDownLatch blocker = new CountDownLatch(1);
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                try {
                    blocker.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        sampler.start("auto-run-1");

        // Wait for a few polling cycles
        Thread.sleep(500);

        sampler.stop();
        blocker.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertFalse(sampler.isRunning());
        assertTrue(recorder.snapshots("auto-run-1").size() >= 2,
                "expected at least 2 autonomous samples, got "
                        + recorder.snapshots("auto-run-1").size());
    }

    @Test
    void stopShouldBeCallableMultipleTimes() {
        LivePressureSampler sampler = new LivePressureSampler(executor, recorder, config);
        sampler.start("run-multi-stop");
        sampler.stop();
        sampler.stop();
        sampler.stop();
        assertFalse(sampler.isRunning());
    }

    @Test
    void shouldStartWithFourArgConstructor() {
        LivePressureSampler sampler = new LivePressureSampler(
                executor, recorder, new DefaultSnapshotAssembler(), config);

        assertFalse(sampler.isRunning());
        sampler.start("run-4arg");
        assertTrue(sampler.isRunning());
        sampler.stop();
    }
}
