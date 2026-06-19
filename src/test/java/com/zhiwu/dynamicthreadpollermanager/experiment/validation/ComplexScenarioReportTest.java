package com.zhiwu.dynamicthreadpollermanager.experiment.validation;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComplexScenarioReportTest {

    private static final Instant NOW = Instant.parse("2026-06-05T10:00:00Z");

    @Test
    void shouldCreateWithAllIdentificationFields() {
        List<ObservationWindow> windows = List.of(
                new ObservationWindow(0, List.of(), List.of(), NOW));

        ComplexScenarioReport report = new ComplexScenarioReport(
                "r1", "s1", 42L, "profile=LONG_TAIL,stepCount=10",
                10, 3, 2, 0.667, 5000L, 150L, 300L,
                15, 5, -20.0,
                windows, NOW);

        assertEquals("r1", report.reportId());
        assertEquals("s1", report.scenarioId());
        assertEquals(42L, report.seed());
        assertEquals("profile=LONG_TAIL,stepCount=10", report.scenarioConfig());
    }

    @Test
    void shouldReflectCounts() {
        List<ObservationWindow> windows = List.of();
        ComplexScenarioReport report = new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                10, 3, 2, 0.667, 5000L, 150L, 300L,
                15, 5, -20.0,
                windows, NOW);

        assertEquals(10, report.adjustmentCount());
        assertEquals(3, report.blockedCount());
        assertEquals(2, report.rollbackCount());
    }

    @Test
    void shouldComputeRollbackSuccessRate() {
        ComplexScenarioReport report = new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                10, 0, 3, 0.667, 5000L, 150L, 300L,
                0, 0, 0.0,
                List.of(), NOW);

        assertEquals(0.667, report.rollbackSuccessRate(), 0.001);
    }

    @Test
    void shouldAcceptZeroRollbackSuccessRate() {
        ComplexScenarioReport report = new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                5, 0, 0, 0.0, 0L, 0L, 0L,
                0, 0, 0.0,
                List.of(), NOW);

        assertEquals(0.0, report.rollbackSuccessRate());
    }

    @Test
    void shouldStoreRecoveryTime() {
        ComplexScenarioReport report = new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                5, 0, 0, 0.0, 5000L, 150L, 300L,
                0, 0, 0.0,
                List.of(), NOW);

        assertEquals(5000L, report.recoveryTimeMs());
    }

    @Test
    void shouldStorePercentileLatencies() {
        ComplexScenarioReport report = new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                5, 0, 0, 0.0, 0L, 150L, 300L,
                0, 0, 0.0,
                List.of(), NOW);

        assertEquals(150L, report.p95LatencyMs());
        assertEquals(300L, report.p99LatencyMs());
    }

    @Test
    void shouldStoreDeltas() {
        ComplexScenarioReport report = new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                5, 0, 0, 0.0, 0L, 0L, 0L,
                0, 15, -20.0,
                List.of(), NOW);

        assertEquals(15, report.queueDepthDelta());
        assertEquals(-20.0, report.throughputDelta());
    }

    @Test
    void shouldStoreRejectionCount() {
        ComplexScenarioReport report = new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                5, 0, 0, 0.0, 0L, 0L, 0L,
                15, 0, 0.0,
                List.of(), NOW);

        assertEquals(15, report.rejectionCount());
    }

    @Test
    void shouldStoreDecisionWindows() {
        List<ObservationWindow> windows = List.of(
                new ObservationWindow(0,
                        List.of(snapshot(4, 0, NOW)),
                        List.of(snapshot(6, 5, NOW.plusMillis(100))),
                        NOW),
                new ObservationWindow(1,
                        List.of(snapshot(6, 5, NOW.plusMillis(200))),
                        List.of(snapshot(4, 2, NOW.plusMillis(300))),
                        NOW.plusMillis(200)));

        ComplexScenarioReport report = new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                2, 0, 0, 0.0, 0L, 0L, 0L,
                0, 0, 0.0,
                windows, NOW);

        assertEquals(2, report.decisionWindows().size());
        assertEquals(0, report.decisionWindows().get(0).decisionIndex());
        assertEquals(1, report.decisionWindows().get(1).decisionIndex());
        assertEquals(1, report.decisionWindows().get(0).preDecisionSnapshots().size());
        assertEquals(1, report.decisionWindows().get(1).postDecisionSnapshots().size());
    }

    @Test
    void shouldHaveNonNullGeneratedAt() {
        ComplexScenarioReport report = new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                0, 0, 0, 0.0, 0L, 0L, 0L,
                0, 0, 0.0,
                List.of(), NOW);

        assertEquals(NOW, report.generatedAt());
    }

    @Test
    void shouldRejectNullReportId() {
        assertThrows(NullPointerException.class, () -> new ComplexScenarioReport(
                null, "s1", 42L, "test",
                0, 0, 0, 0.0, 0L, 0L, 0L,
                0, 0, 0.0,
                List.of(), NOW));
    }

    @Test
    void shouldRejectNegativeAdjustmentCount() {
        assertThrows(IllegalArgumentException.class, () -> new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                -1, 0, 0, 0.0, 0L, 0L, 0L,
                0, 0, 0.0,
                List.of(), NOW));
    }

    @Test
    void shouldRejectNegativeBlockedCount() {
        assertThrows(IllegalArgumentException.class, () -> new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                0, -1, 0, 0.0, 0L, 0L, 0L,
                0, 0, 0.0,
                List.of(), NOW));
    }

    @Test
    void shouldRejectRollbackSuccessRateAboveOne() {
        assertThrows(IllegalArgumentException.class, () -> new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                0, 0, 0, 1.5, 0L, 0L, 0L,
                0, 0, 0.0,
                List.of(), NOW));
    }

    @Test
    void shouldRejectRollbackSuccessRateBelowZero() {
        assertThrows(IllegalArgumentException.class, () -> new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                0, 0, 0, -0.1, 0L, 0L, 0L,
                0, 0, 0.0,
                List.of(), NOW));
    }

    @Test
    void shouldRejectNegativeLatency() {
        assertThrows(IllegalArgumentException.class, () -> new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                0, 0, 0, 0.0, 0L, -1L, 0L,
                0, 0, 0.0,
                List.of(), NOW));
    }

    @Test
    void shouldRejectNullDecisionWindows() {
        assertThrows(NullPointerException.class, () -> new ComplexScenarioReport(
                "r1", "s1", 42L, "test",
                0, 0, 0, 0.0, 0L, 0L, 0L,
                0, 0, 0.0,
                null, NOW));
    }

    private static ExecutorStateSnapshot snapshot(int poolSize, int queueSize, Instant time) {
        return ExecutorStateSnapshot.builder(time)
                .corePoolSize(poolSize).maximumPoolSize(poolSize * 2)
                .poolSize(poolSize).activeCount(poolSize)
                .queueSize(queueSize).build();
    }
}
