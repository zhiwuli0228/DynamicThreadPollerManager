package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import com.zhiwu.dynamicthreadpollermanager.experiment.coordinator.ExperimentCoordinator;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.DefaultSnapshotAssembler;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.InMemoryEvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ManualPressureSampler;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class ComparableScenarioRunner {

    private final BaselineExecutorCatalog catalog;
    private final ScenarioPlanner planner;
    private final Supplier<Instant> clock;

    public ComparableScenarioRunner(
            BaselineExecutorCatalog catalog,
            ScenarioPlanner planner,
            Supplier<Instant> clock) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.planner = Objects.requireNonNull(planner, "planner must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ComparisonResult compare(
            ScenarioDefinition scenario,
            String baselinePresetId,
            ManagedExecutorConfig managedConfig) {

        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(baselinePresetId, "baselinePresetId must not be null");
        Objects.requireNonNull(managedConfig, "managedConfig must not be null");

        CommonExecutorPreset preset = catalog.get(baselinePresetId);
        BaselineExecutorPreset bPreset = preset.toBaselinePreset();

        // Baseline run
        ExperimentCoordinator bCoord = new ExperimentCoordinator();
        InMemoryEvidenceRecorder bRecorder = new InMemoryEvidenceRecorder();
        ManualPressureSampler bSampler = new ManualPressureSampler(new DefaultSnapshotAssembler());
        BaselineWorkloadExecutor bExecutor = new BaselineWorkloadExecutor(bPreset);
        ScenarioExperimentRunner baselineRunner = new ScenarioExperimentRunner(
                bCoord, planner, bExecutor, bSampler, bRecorder, clock);

        long baselineStartMs = clock.get().toEpochMilli();
        ScenarioRunOutcome bOutcome = baselineRunner.run(scenario, bPreset);
        long baselineEndMs = clock.get().toEpochMilli();

        List<ObservedSnapshot> bSnapshots = bRecorder.snapshots(bOutcome.runId());
        NormalizedComparisonMetrics bMetrics = NormalizedComparisonMetrics.fromSnapshots(
                bSnapshots, baselineEndMs - baselineStartMs, bPreset.corePoolSize());

        // Managed run
        ExperimentCoordinator mCoord = new ExperimentCoordinator();
        InMemoryEvidenceRecorder mRecorder = new InMemoryEvidenceRecorder();
        ManualPressureSampler mSampler = new ManualPressureSampler(new DefaultSnapshotAssembler());
        ManagedExecutorScenarioRunner managedRunner = new ManagedExecutorScenarioRunner(
                mCoord, planner, mSampler, mRecorder, clock);

        long managedStartMs = clock.get().toEpochMilli();
        ScenarioRunOutcome mOutcome = managedRunner.run(scenario, managedConfig);
        long managedEndMs = clock.get().toEpochMilli();

        List<ObservedSnapshot> mSnapshots = mRecorder.snapshots(mOutcome.runId());
        long mRejected = mOutcome.rejectedTaskCount();
        NormalizedComparisonMetrics mMetrics = NormalizedComparisonMetrics.fromSnapshots(
                mSnapshots, managedEndMs - managedStartMs, managedConfig.corePoolSize())
                .withRejectedTaskCount(mRejected);

        // Compute deltas
        Map<String, MetricDelta> deltas = new LinkedHashMap<>();
        deltas.put("completedTaskCount", MetricDelta.compute(
                "completedTaskCount", bMetrics.completedTaskCount(),
                mMetrics.completedTaskCount(), true));
        deltas.put("rejectedTaskCount", MetricDelta.compute(
                "rejectedTaskCount", bMetrics.rejectedTaskCount(),
                mMetrics.rejectedTaskCount(), false));
        deltas.put("avgQueueDepth", MetricDelta.compute(
                "avgQueueDepth", bMetrics.avgQueueDepth(),
                mMetrics.avgQueueDepth(), false));
        deltas.put("maxQueueDepth", MetricDelta.compute(
                "maxQueueDepth", bMetrics.maxQueueDepth(),
                mMetrics.maxQueueDepth(), false));
        deltas.put("totalDurationMs", MetricDelta.compute(
                "totalDurationMs", bMetrics.totalDurationMs(),
                mMetrics.totalDurationMs(), false));
        deltas.put("throughputPerSecond", MetricDelta.compute(
                "throughputPerSecond", bMetrics.throughputPerSecond(),
                mMetrics.throughputPerSecond(), true));
        deltas.put("avgActiveThreads", MetricDelta.compute(
                "avgActiveThreads", bMetrics.avgActiveThreads(),
                mMetrics.avgActiveThreads(), true));
        deltas.put("maxPoolSize", MetricDelta.compute(
                "maxPoolSize", bMetrics.maxPoolSize(),
                mMetrics.maxPoolSize(), true));
        deltas.put("snapshotCount", MetricDelta.compute(
                "snapshotCount", bMetrics.snapshotCount(),
                mMetrics.snapshotCount(), true));

        String comparisonId = UUID.randomUUID().toString();
        return new ComparisonResult(
                comparisonId, scenario.scenarioId(),
                baselinePresetId, managedConfig.toPresetSummary().policyId(),
                bOutcome, mOutcome, bMetrics, mMetrics, deltas, clock.get());
    }
}
