package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Autonomous pressure sampler that polls a live {@link ManagedExecutor}
 * at a configurable fixed delay using a daemon single-thread scheduler.
 *
 * <p>Implements {@link PressureSampler} — the {@link #sample} method
 * remains available for manual sampling alongside autonomous scheduling.
 */
public final class LivePressureSampler implements PressureSampler {

    private static final int MAX_CONSECUTIVE_FAILURES = 10;

    private final ManagedExecutor executor;
    private final EvidenceRecorder recorder;
    private final SnapshotAssembler assembler;
    private final LivePressureSamplerConfig config;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    public LivePressureSampler(
            ManagedExecutor executor,
            EvidenceRecorder recorder,
            SnapshotAssembler assembler,
            LivePressureSamplerConfig config) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.recorder = Objects.requireNonNull(recorder, "recorder must not be null");
        this.assembler = Objects.requireNonNull(assembler, "assembler must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "live-pressure-sampler-" + config.sessionId());
            t.setDaemon(true);
            return t;
        });
    }

    public LivePressureSampler(
            ManagedExecutor executor,
            EvidenceRecorder recorder,
            LivePressureSamplerConfig config) {
        this(executor, recorder, new DefaultSnapshotAssembler(), config);
    }

    public void start(String runId) {
        Objects.requireNonNull(runId, "runId must not be null");
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("sampler is already running");
        }
        scheduler.scheduleWithFixedDelay(() -> {
            if (!running.get()) return;
            try {
                Instant now = Instant.now();
                RuntimeObservation obs = RuntimeObservation.fromExecutor(executor, now);
                ObservedSnapshot snapshot = assembler.assemble(runId, obs);
                recorder.record(snapshot);
                consecutiveFailures.set(0);
            } catch (RuntimeException e) {
                int failures = consecutiveFailures.incrementAndGet();
                if (failures >= MAX_CONSECUTIVE_FAILURES) {
                    stop();
                }
            }
        }, 0, config.pollIntervalMs(), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    @Override
    public ObservedSnapshot sample(String runId, RuntimeObservation observation, Instant at) {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(observation, "observation must not be null");
        Objects.requireNonNull(at, "at must not be null");
        RuntimeObservation timestamped = observation.withTimestamp(at);
        ObservedSnapshot snapshot = assembler.assemble(runId, timestamped);
        recorder.record(snapshot);
        return snapshot;
    }
}
