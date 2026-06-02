package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import com.zhiwu.dynamicthreadpollermanager.experiment.coordinator.ExperimentCoordinator;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.EvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.PressureSampler;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.ExperimentRun;
import com.zhiwu.dynamicthreadpollermanager.experiment.state.RunState;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Drives a deterministic scenario through the experiment lifecycle
 * and records one observed snapshot per executed step. The runner
 * never evaluates policy, never resizes the baseline executor, and
 * never mutates queues.
 */
public final class ScenarioExperimentRunner {

    private final ExperimentCoordinator coordinator;
    private final ScenarioPlanner planner;
    private final BaselineWorkloadExecutor baselineExecutor;
    private final PressureSampler sampler;
    private final EvidenceRecorder recorder;
    private final Supplier<Instant> clock;

    public ScenarioExperimentRunner(ExperimentCoordinator coordinator,
                                    ScenarioPlanner planner,
                                    BaselineWorkloadExecutor baselineExecutor,
                                    PressureSampler sampler,
                                    EvidenceRecorder recorder,
                                    Supplier<Instant> clock) {
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator must not be null");
        this.planner = Objects.requireNonNull(planner, "planner must not be null");
        this.baselineExecutor = Objects.requireNonNull(baselineExecutor, "baselineExecutor must not be null");
        this.sampler = Objects.requireNonNull(sampler, "sampler must not be null");
        this.recorder = Objects.requireNonNull(recorder, "recorder must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ScenarioRunOutcome run(ScenarioDefinition definition, BaselineExecutorPreset preset) {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(preset, "preset must not be null");

        ScenarioPlan plan = planner.plan(definition);
        ExperimentRun run = coordinator.createRun(definition.scenarioId(), preset.policyId());
        coordinator.startRun(run.runId());

        try {
            for (ScenarioStep step : plan.steps()) {
                baselineExecutor.executeStep(step);
                RuntimeObservation observation = buildObservation(preset);
                ObservedSnapshot snapshot = sampler.sample(run.runId(), observation, observation.timestamp());
                recorder.record(snapshot);
            }
        } catch (RuntimeException ex) {
            // Best-effort cleanup: if anything fails mid-run, attempt to stop
            // the run so the coordinator does not hold a RUNNING record.
            tryStop(run.runId());
            throw ex;
        }

        coordinator.stopRun(run.runId());
        ExperimentRun finalized = coordinator.finalizeRun(run.runId());

        List<ObservedSnapshot> recorded = recorder.snapshots(run.runId());
        return new ScenarioRunOutcome(
                finalized.runId(),
                finalized.scenarioId(),
                finalized.policyId(),
                baselineExecutor.completedStepCount(),
                baselineExecutor.completedWorkUnits(),
                recorded.size(),
                finalized.state()
        );
    }

    private RuntimeObservation buildObservation(BaselineExecutorPreset preset) {
        Instant now = clock.get();
        return new RuntimeObservation(
                now,
                MetricValue.present(baselineExecutor.activeThreads()),
                MetricValue.present(preset.corePoolSize()),
                MetricValue.present(baselineExecutor.queueSize()),
                MetricValue.present(baselineExecutor.completedTaskCount()),
                MetricValue.absent()
        );
    }

    private void tryStop(String runId) {
        try {
            coordinator.stopRun(runId);
        } catch (RuntimeException ignored) {
            // Run may already be in a terminal state or missing; nothing to do.
        }
    }

    /**
     * Convenience accessor used by tests and other callers that need
     * to confirm the runner finalized the run.
     */
    public RunState stateOf(String runId) {
        return coordinator.getRun(runId).state();
    }
}
