package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.LoadScenario;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationContractsTest {

    @Test
    void validationRunResultShouldPreserveFields() {
        ValidationRunResult result = new ValidationRunResult(
                ValidationMode.BASELINE, "run-1", 30,
                Map.of("throughput", 100.0, "latency", 50.0),
                30_000, 0, "steady");

        assertEquals(ValidationMode.BASELINE, result.mode());
        assertEquals(30, result.snapshotCount());
        assertEquals(2, result.metrics().size());
        assertEquals(0, result.adjustmentCount());
    }

    @Test
    void validationRunResultShouldDefensiveCopyMetrics() {
        Map<String, Double> mutable = new java.util.HashMap<>(Map.of("a", 1.0));
        ValidationRunResult result = new ValidationRunResult(
                ValidationMode.BASELINE, "r", 0, mutable, 0, 0, "s");
        mutable.put("b", 2.0);
        assertEquals(1, result.metrics().size());
    }

    @Test
    void metricComparisonShouldHavePositiveDeltaForBetterClosedLoop() {
        MetricComparison mc = new MetricComparison(
                "throughput", 200, 150, 100, 50, 100, 50);
        assertTrue(mc.closedLoopVsBaselineDelta() > 0);
        assertTrue(mc.closedLoopValue() > mc.baselineValue());
    }

    @Test
    void statisticalSignificanceShouldReportSignificantWhenPValueBelow05() {
        StatisticalSignificance sig = new StatisticalSignificance(
                "throughput", 0.01, -10, 30, 1.2, true, 30);
        assertTrue(sig.isSignificant());
        assertTrue(sig.pValue() < 0.05);
    }

    @Test
    void statisticalSignificanceShouldReportSampleSize() {
        StatisticalSignificance sig = new StatisticalSignificance(
                "latency", 0.5, 0, 0, 0, false, 30);
        assertEquals(30, sig.sampleSize());
    }

    @Test
    void validationComparisonReportShouldContainAllModes() {
        LoadScenario workload = new LoadScenario("load-1", "test");
        ManagedExecutorConfig execConfig = ManagedExecutorConfig.defaultConfig();
        ThresholdPolicyConfig policy =
                new ThresholdPolicyConfig("t1", 2, 8, 4, 10, 2, 1);
        ValidationScenario scenario = new ValidationScenario(
                "s1", workload, execConfig, List.of(policy), policy, 30_000, 5, 1000);

        ValidationRunResult cl = new ValidationRunResult(
                ValidationMode.CLOSED_LOOP, "r-cl", 30, Map.of(), 30_000, 5, "steady");
        ValidationRunResult sp = new ValidationRunResult(
                ValidationMode.STATIC_POLICY, "r-sp", 30, Map.of(), 30_000, 0, "steady");
        ValidationRunResult bl = new ValidationRunResult(
                ValidationMode.BASELINE, "r-bl", 30, Map.of(), 30_000, 0, "steady");

        ValidationComparisonReport report = new ValidationComparisonReport(
                "rpt-1", scenario, cl, sp, bl,
                List.of(), List.of(), "No data", Instant.now());

        assertEquals(ValidationMode.CLOSED_LOOP, report.closedLoopResult().mode());
        assertEquals(ValidationMode.STATIC_POLICY, report.staticPolicyResult().mode());
        assertEquals(ValidationMode.BASELINE, report.baselineResult().mode());
    }

    @Test
    void validationComparisonReportShouldDefensiveCopyLists() {
        LoadScenario workload = new LoadScenario("load-1", "test");
        ManagedExecutorConfig execConfig = ManagedExecutorConfig.defaultConfig();
        ThresholdPolicyConfig policy =
                new ThresholdPolicyConfig("t1", 2, 8, 4, 10, 2, 1);
        ValidationScenario scenario = new ValidationScenario(
                "s1", workload, execConfig, List.of(policy), policy, 30_000, 5, 1000);
        ValidationRunResult result = new ValidationRunResult(
                ValidationMode.BASELINE, "r", 0, Map.of(), 0, 0, "s");

        List<MetricComparison> mutableComparisons = new java.util.ArrayList<>();
        List<StatisticalSignificance> mutableSignificance = new java.util.ArrayList<>();

        ValidationComparisonReport report = new ValidationComparisonReport(
                "rpt", scenario, result, result, result,
                mutableComparisons, mutableSignificance, "conclusion", Instant.now());

        mutableComparisons.add(new MetricComparison("m", 0, 0, 0, 0, 0, 0));
        mutableSignificance.add(new StatisticalSignificance("m", 0.5, 0, 0, 0, false, 1));

        assertEquals(0, report.comparisons().size());
        assertEquals(0, report.significanceTests().size());
    }
}
