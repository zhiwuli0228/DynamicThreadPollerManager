package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import com.zhiwu.dynamicthreadpollermanager.experiment.coordinator.ExperimentCoordinator;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ExecutorRegistry;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.EvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.LivePressureSampler;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.LivePressureSamplerConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.PressureSampler;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.ExperimentRun;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class ManagedExecutorScenarioRunner {

    private static final java.util.logging.Logger LOG =
            java.util.logging.Logger.getLogger(ManagedExecutorScenarioRunner.class.getName());

    private final ExperimentCoordinator coordinator;
    private final ScenarioPlanner planner;
    private final PressureSampler sampler;
    private final EvidenceRecorder recorder;
    private final Supplier<Instant> clock;
    private final LivePressureSamplerConfig liveSamplerConfig;
    private LivePressureSampler liveSampler;

    public ManagedExecutorScenarioRunner(
            ExperimentCoordinator coordinator,
            ScenarioPlanner planner,
            PressureSampler sampler,
            EvidenceRecorder recorder,
            Supplier<Instant> clock) {
        this(coordinator, planner, sampler, recorder, clock, null);
    }

    public ManagedExecutorScenarioRunner(
            ExperimentCoordinator coordinator,
            ScenarioPlanner planner,
            PressureSampler sampler,
            EvidenceRecorder recorder,
            Supplier<Instant> clock,
            LivePressureSamplerConfig liveSamplerConfig) {
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator must not be null");
        this.planner = Objects.requireNonNull(planner, "planner must not be null");
        this.sampler = Objects.requireNonNull(sampler, "sampler must not be null");
        this.recorder = Objects.requireNonNull(recorder, "recorder must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.liveSamplerConfig = liveSamplerConfig;
    }

    public ScenarioRunOutcome run(ScenarioDefinition definition, ManagedExecutorConfig config) {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(config, "config must not be null");

        // Phase 1: create executor + register
        ManagedExecutor executor = config.toManagedExecutor();
        ExecutorRegistry registry = new ExecutorRegistry(null);
        registry.register(definition.scenarioId(), executor);

        // Phase 2: coordinate lifecycle
        ExperimentRun run = coordinator.createRun(
                definition.scenarioId(), config.toPresetSummary().policyId());
        coordinator.startRun(run.runId());

        ScenarioPlan plan = planner.plan(definition);

        if (liveSamplerConfig != null) {
            liveSampler = new LivePressureSampler(executor, recorder, liveSamplerConfig);
            liveSampler.start(run.runId());
        }

        try {
            for (ScenarioStep step : plan.steps()) {
                // Phase 3: submit + sync + sample + release + idle
                int taskCount = taskCountFor(definition.profile(), step.index(), config);
                CountDownLatch blocker = new CountDownLatch(1);
                CountDownLatch startedLatch = new CountDownLatch(taskCount);

                for (int i = 0; i < taskCount; i++) {
                    executor.submit(() -> {
                        startedLatch.countDown();
                        await(blocker);
                    });
                }

                boolean allStarted = false;
                try {
                    allStarted = startedLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (!allStarted) {
                    // timeout: record warning but continue sampling
                }

                if (liveSampler == null) {
                    RuntimeObservation observation = buildObservation(executor, clock.get());
                    ObservedSnapshot snapshot = sampler.sample(
                            run.runId(), observation, observation.timestamp());
                    recorder.record(snapshot);
                }

                blocker.countDown();
                waitForIdle(executor);
            }
        } catch (RuntimeException ex) {
            if (liveSampler != null) {
                liveSampler.stop();
            }
            tryStop(run.runId());
            shutdownAndTerminate(executor);
            throw ex;
        }

        // Phase 4: lifecycle closeout
        coordinator.stopRun(run.runId());
        ExperimentRun finalized = coordinator.finalizeRun(run.runId());

        // Phase 5: stop live sampler, shutdown + terminate
        if (liveSampler != null) {
            liveSampler.stop();
        }
        shutdownAndTerminate(executor);

        // Phase 6: remove from registry after confirming terminated
        if (!executor.isTerminated()) {
            throw new IllegalStateException(
                    "executor must be terminated before remove");
        }
        registry.remove(definition.scenarioId());

        // Phase 7: return outcome
        List<ObservedSnapshot> recorded = recorder.snapshots(run.runId());
        return new ScenarioRunOutcome(
                finalized.runId(),
                finalized.scenarioId(),
                finalized.policyId(),
                plan.steps().size(),
                plan.totalWorkUnits(),
                recorded.size(),
                finalized.state());
    }

    private int taskCountFor(ScenarioProfile profile, int stepIndex, ManagedExecutorConfig config) {
        int cap = config.queueCapacity() + config.maximumPoolSize();
        return switch (profile) {
            case STEADY -> 2;
            case RAMP -> Math.min(2 + stepIndex, cap);
            case BURST -> (stepIndex % 3 == 0) ? 6 : 2;
        };
    }

    private void waitForIdle(ManagedExecutor executor) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (executor.getQueueSize() == 0 && executor.getActiveCount() == 0) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void shutdownAndTerminate(ManagedExecutor executor) {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!executor.isTerminated()) {
            executor.shutdownNow();
        }
    }

    private RuntimeObservation buildObservation(ManagedExecutor executor, Instant now) {
        return RuntimeObservation.fromExecutor(executor, now);
    }

    private void tryStop(String runId) {
        try {
            coordinator.stopRun(runId);
        } catch (RuntimeException e) {
            LOG.fine("best-effort stopRun failed for " + runId + ": " + e.getMessage());
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
