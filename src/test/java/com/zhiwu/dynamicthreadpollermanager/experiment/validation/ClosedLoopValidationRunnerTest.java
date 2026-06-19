package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.DefaultSnapshotAssembler;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.LoadScenario;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClosedLoopValidationRunnerTest {

    @Test
    void computeMetricsShouldReturnSevenEntries() {
        ManagedExecutor executor = new ManagedExecutor(
                2, 4, 60, java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(10));
        try {
            executor.submit(() -> { /* no-op */ });
            List<ObservedSnapshot> snapshots = new ArrayList<>();
            DefaultSnapshotAssembler assembler = new DefaultSnapshotAssembler();
            for (int i = 0; i < 5; i++) {
                RuntimeObservation obs = RuntimeObservation.fromExecutor(
                        executor, Instant.now());
                snapshots.add(assembler.assemble("test", obs));
            }

            Map<String, Double> metrics = ClosedLoopValidationRunner.computeMetrics(
                    snapshots, 5000, executor);

            assertEquals(7, metrics.size());
            assertTrue(metrics.containsKey("throughput"));
            assertTrue(metrics.containsKey("latency"));
            assertTrue(metrics.containsKey("p99"));
            assertTrue(metrics.containsKey("rejectionRate"));
            assertTrue(metrics.containsKey("queueDepth"));
            assertTrue(metrics.containsKey("stability"));
            assertTrue(metrics.containsKey("cpuEfficiency"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void computeComparisonsShouldProduceSevenComparisons() {
        ValidationRunResult cl = new ValidationRunResult(
                ValidationMode.CLOSED_LOOP, "r1", 30,
                Map.of("throughput", 200.0, "latency", 15.0, "p99", 50.0,
                        "rejectionRate", 0.01, "queueDepth", 3.0,
                        "stability", 0.9, "cpuEfficiency", 0.8),
                30_000, 3, "steady");
        ValidationRunResult sp = new ValidationRunResult(
                ValidationMode.STATIC_POLICY, "r2", 30,
                Map.of("throughput", 180.0, "latency", 20.0, "p99", 60.0,
                        "rejectionRate", 0.02, "queueDepth", 4.0,
                        "stability", 0.85, "cpuEfficiency", 0.75),
                30_000, 0, "steady");
        ValidationRunResult bl = new ValidationRunResult(
                ValidationMode.BASELINE, "r3", 30,
                Map.of("throughput", 150.0, "latency", 30.0, "p99", 80.0,
                        "rejectionRate", 0.05, "queueDepth", 5.0,
                        "stability", 0.7, "cpuEfficiency", 0.6),
                30_000, 0, "steady");

        List<MetricComparison> comparisons = ClosedLoopValidationRunner
                .computeComparisons(cl, sp, bl);

        assertEquals(7, comparisons.size());

        MetricComparison throughput = findMetric(comparisons, "throughput");
        assertTrue(throughput.closedLoopVsBaselineDelta() > 0,
                "Closed-loop should have higher throughput");
        assertTrue(throughput.closedLoopValue() > throughput.baselineValue());

        MetricComparison rejection = findMetric(comparisons, "rejectionRate");
        assertTrue(rejection.closedLoopVsBaselineDelta() > 0,
                "Closed-loop should have lower rejection rate (= positive delta)");
    }

    @Test
    void computeSignificanceShouldProduceFourteenTests() {
        ValidationRunResult cl = new ValidationRunResult(
                ValidationMode.CLOSED_LOOP, "r1", 30,
                Map.of("throughput", 200.0, "latency", 15.0, "p99", 50.0,
                        "rejectionRate", 0.01, "queueDepth", 3.0,
                        "stability", 0.9, "cpuEfficiency", 0.8),
                30_000, 3, "steady");
        ValidationRunResult sp = new ValidationRunResult(
                ValidationMode.STATIC_POLICY, "r2", 30,
                Map.of("throughput", 180.0, "latency", 20.0, "p99", 60.0,
                        "rejectionRate", 0.02, "queueDepth", 4.0,
                        "stability", 0.85, "cpuEfficiency", 0.75),
                30_000, 0, "steady");
        ValidationRunResult bl = new ValidationRunResult(
                ValidationMode.BASELINE, "r3", 30,
                Map.of("throughput", 150.0, "latency", 30.0, "p99", 80.0,
                        "rejectionRate", 0.05, "queueDepth", 5.0,
                        "stability", 0.7, "cpuEfficiency", 0.6),
                30_000, 0, "steady");

        List<StatisticalSignificance> tests = ClosedLoopValidationRunner
                .computeSignificance(cl, sp, bl);

        assertEquals(14, tests.size());
        long significant = tests.stream()
                .filter(StatisticalSignificance::isSignificant).count();
        assertTrue(significant > 0,
                "At least some comparisons should be significant");
    }

    @Test
    void generateConclusionShouldProduceMeaningfulText() {
        MetricComparison good = new MetricComparison(
                "throughput", 200, 150, 100, 50, 100, 50);
        StatisticalSignificance sig = new StatisticalSignificance(
                "throughput", 0.01, 0, 0, 1.0, true, 30);
        List<MetricComparison> comparisons = List.of(
                good, good, good, good, good, good, good);
        List<StatisticalSignificance> tests = List.of(
                sig, sig, sig, sig, sig);

        String conclusion = ClosedLoopValidationRunner.generateConclusion(
                comparisons, tests);

        assertNotNull(conclusion);
        assertFalse(conclusion.isBlank());
        assertTrue(conclusion.contains("closed-loop") || conclusion.contains("Closed-loop"));
    }

    @Test
    void shouldRunBaselineMode() {
        LoadScenario workload = new LoadScenario("load-1", "test workload");
        ThresholdPolicyConfig policy =
                new ThresholdPolicyConfig("t1", 2, 8, 4, 10, 2, 1);
        ValidationScenario scenario = new ValidationScenario(
                "test-baseline", workload,
                ManagedExecutorConfig.defaultConfig(),
                List.of(policy), policy, 30_000, 5, 1000);

        ClosedLoopValidationRunner runner = new ClosedLoopValidationRunner();
        ValidationRunResult result = runner.runBaselineMode(scenario);

        assertEquals(ValidationMode.BASELINE, result.mode());
        assertTrue(result.snapshotCount() >= 5,
                "Expected >= 5 snapshots, got " + result.snapshotCount());
        assertEquals(7, result.metrics().size());
        assertTrue(result.metrics().get("throughput") >= 0);
    }

    @Test
    void shouldRunStaticPolicyMode() {
        LoadScenario workload = new LoadScenario("load-1", "test workload");
        ThresholdPolicyConfig policy =
                new ThresholdPolicyConfig("t1", 2, 8, 4, 10, 2, 1);
        ValidationScenario scenario = new ValidationScenario(
                "test-static", workload,
                ManagedExecutorConfig.defaultConfig(),
                List.of(policy), policy, 30_000, 5, 1000);

        ClosedLoopValidationRunner runner = new ClosedLoopValidationRunner();
        ValidationRunResult result = runner.runStaticPolicyMode(scenario);

        assertEquals(ValidationMode.STATIC_POLICY, result.mode());
        assertEquals(0, result.adjustmentCount());
    }

    @Test
    void shouldRunClosedLoopMode() {
        LoadScenario workload = new LoadScenario("load-1", "test workload");
        ThresholdPolicyConfig policy =
                new ThresholdPolicyConfig("t1", 2, 8, 4, 10, 2, 1);
        ValidationScenario scenario = new ValidationScenario(
                "test-cl", workload,
                ManagedExecutorConfig.defaultConfig(),
                List.of(policy), policy, 30_000, 5, 1000);

        ClosedLoopValidationRunner runner = new ClosedLoopValidationRunner();
        ValidationRunResult result = runner.runClosedLoopMode(scenario);

        assertEquals(ValidationMode.CLOSED_LOOP, result.mode());
        assertEquals(7, result.metrics().size());
    }

    @Test
    void shouldRunFullValidation() {
        LoadScenario workload = new LoadScenario("load-1", "test workload");
        ThresholdPolicyConfig conservative = new ThresholdPolicyConfig(
                "conservative", 2, 4, 8, 10, 2, 1);
        ThresholdPolicyConfig moderate = new ThresholdPolicyConfig(
                "moderate", 3, 8, 4, 8, 2, 1);
        ValidationScenario scenario = new ValidationScenario(
                "full-validation", workload,
                ManagedExecutorConfig.defaultConfig(),
                List.of(conservative, moderate), conservative, 30_000, 5, 1000);

        ClosedLoopValidationRunner runner = new ClosedLoopValidationRunner();
        ValidationComparisonReport report = runner.validate(scenario);

        assertNotNull(report.reportId());
        assertEquals(3, List.of(report.closedLoopResult(), report.staticPolicyResult(),
                report.baselineResult()).size());
        assertEquals(7, report.comparisons().size());
        assertEquals(14, report.significanceTests().size());
        assertFalse(report.overallConclusion().isBlank());

        // Verify closed-loop has rejection rate not worse than baseline
        double clRejection = report.closedLoopResult().metrics()
                .getOrDefault("rejectionRate", 1.0);
        double blRejection = report.baselineResult().metrics()
                .getOrDefault("rejectionRate", 0.0);
        assertTrue(clRejection <= blRejection + 0.1,
                "Closed-loop rejection rate should not be significantly worse than baseline");
    }

    private static MetricComparison findMetric(
            List<MetricComparison> comparisons, String name) {
        return comparisons.stream()
                .filter(m -> m.metricName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
