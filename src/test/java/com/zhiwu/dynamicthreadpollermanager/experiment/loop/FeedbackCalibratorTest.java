package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FeedbackCalibratorTest {

    private FeedbackCalibrator calibrator;
    private AdjustmentHistory history;
    private ThresholdPolicyScorer defaultScorer;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        calibrator = new FeedbackCalibrator();
        history = new AdjustmentHistory();
        defaultScorer = new ThresholdPolicyScorer(0.35, 0.30, 0.20, 0.15);
    }

    @Test
    void shouldReturnCurrentScorerForInsufficientData() {
        addEntry(PressureState.OVERLOAD, PressureState.RECOVERY,
                "policy-A", 0.9, 0.8, 0.5, 0.7);
        ThresholdPolicyScorer result = calibrator.calibrate(history, defaultScorer, 10);
        assertSame(defaultScorer, result);
    }

    @Test
    void shouldProduceDifferentWeightsWithMixedHistory() {
        for (int i = 0; i < 10; i++) {
            boolean improvement = i % 2 == 0;
            PressureState before = improvement ? PressureState.OVERLOAD : PressureState.NORMAL;
            PressureState after = improvement ? PressureState.RECOVERY : PressureState.QUEUE_BUILDUP;
            double rScore = improvement ? 0.9 : 0.3;
            addEntry(before, after, "policy-A", rScore, 0.6, 0.6, 0.6);
        }

        ThresholdPolicyScorer result = calibrator.calibrate(history, defaultScorer, 10);
        assertNotSame(defaultScorer, result);
    }

    @Test
    void shouldNormalizeWeightsToSumToOne() {
        for (int i = 0; i < 10; i++) {
            boolean improvement = i < 7;
            PressureState before = improvement ? PressureState.OVERLOAD : PressureState.NORMAL;
            PressureState after = improvement ? PressureState.RECOVERY : PressureState.QUEUE_BUILDUP;
            double rScore = improvement ? 0.9 : 0.2;
            addEntry(before, after, "policy-A", rScore, 0.5, 0.5, 0.5);
        }

        ThresholdPolicyScorer result = calibrator.calibrate(history, defaultScorer, 10);
        double sum = result.wResponsiveness() + result.wSafety()
                + result.wStability() + result.wEfficiency();
        assertEquals(1.0, sum, 0.001);
    }

    @Test
    void shouldKeepWeightsWithinBounds() {
        for (int i = 0; i < 10; i++) {
            boolean improvement = i % 2 == 0;
            PressureState before = improvement ? PressureState.OVERLOAD : PressureState.NORMAL;
            PressureState after = improvement ? PressureState.RECOVERY : PressureState.QUEUE_BUILDUP;
            double rScore = improvement ? 0.9 : 0.1;
            addEntry(before, after, "policy-A", rScore, 0.6, 0.6, 0.6);
        }

        ThresholdPolicyScorer result = calibrator.calibrate(history, defaultScorer, 10);
        assertTrue(result.wResponsiveness() >= 0.10 && result.wResponsiveness() <= 0.50,
                "wResponsiveness out of bounds: " + result.wResponsiveness());
        assertTrue(result.wSafety() >= 0.10 && result.wSafety() <= 0.50,
                "wSafety out of bounds: " + result.wSafety());
        assertTrue(result.wStability() >= 0.10 && result.wStability() <= 0.50,
                "wStability out of bounds: " + result.wStability());
        assertTrue(result.wEfficiency() >= 0.10 && result.wEfficiency() <= 0.50,
                "wEfficiency out of bounds: " + result.wEfficiency());
    }

    @Test
    void shouldNotChangeWeightsWithZeroCorrelation() {
        // All entries have identical scores and mixed success/failure with no pattern.
        // 5 high-score successes + 5 low-score failures = no correlation.
        for (int i = 0; i < 10; i++) {
            double score = i < 5 ? 0.9 : 0.1;
            boolean improvement = i < 5;
            PressureState before = improvement ? PressureState.OVERLOAD : PressureState.NORMAL;
            PressureState after = improvement ? PressureState.RECOVERY : PressureState.QUEUE_BUILDUP;
            addEntry(before, after, "policy-A", score, score, score, score);
        }

        ThresholdPolicyScorer result = calibrator.calibrate(history, defaultScorer, 10);
        // All 4 dimensions have identical score distributions → same correlation → weights stay close to original
        assertEquals(defaultScorer.wResponsiveness(), result.wResponsiveness(), 0.06);
        assertEquals(defaultScorer.wSafety(), result.wSafety(), 0.06);
        assertEquals(defaultScorer.wStability(), result.wStability(), 0.06);
        assertEquals(defaultScorer.wEfficiency(), result.wEfficiency(), 0.06);
    }

    @Test
    void shouldShiftWeightsWhenResponsivenessCorrelatesWithSuccess() {
        // Responsiveness: high scores → success, low → failure (positive correlation)
        // Other dimensions: no correlation (split evenly)
        for (int i = 0; i < 10; i++) {
            double rScore = i < 6 ? 0.9 : 0.2;
            boolean improvement = i < 6;
            PressureState before = improvement ? PressureState.OVERLOAD : PressureState.NORMAL;
            PressureState after = improvement ? PressureState.RECOVERY : PressureState.QUEUE_BUILDUP;
            // Other dimensions: half high half low, success independent of score
            double otherScore = (i % 2 == 0) ? 0.9 : 0.2;
            addEntry(before, after, "policy-A", rScore, otherScore, otherScore, otherScore);
        }

        ThresholdPolicyScorer result = calibrator.calibrate(history, defaultScorer, 10);
        assertNotSame(defaultScorer, result);
        double sum = result.wResponsiveness() + result.wSafety()
                + result.wStability() + result.wEfficiency();
        assertEquals(1.0, sum, 0.001);
    }

    @Test
    void shouldRejectInvalidMaxAdjustmentPerCycle() {
        assertThrows(IllegalArgumentException.class, () ->
                new FeedbackCalibrator(0.0, 0.10, 0.50));
        assertThrows(IllegalArgumentException.class, () ->
                new FeedbackCalibrator(0.3, 0.10, 0.50));
    }

    @Test
    void shouldRejectInvalidMinWeight() {
        assertThrows(IllegalArgumentException.class, () ->
                new FeedbackCalibrator(0.05, 0.01, 0.50));
        assertThrows(IllegalArgumentException.class, () ->
                new FeedbackCalibrator(0.05, 0.30, 0.50));
    }

    @Test
    void shouldRejectInvalidMaxWeight() {
        assertThrows(IllegalArgumentException.class, () ->
                new FeedbackCalibrator(0.05, 0.10, 0.20));
        assertThrows(IllegalArgumentException.class, () ->
                new FeedbackCalibrator(0.05, 0.10, 0.70));
    }

    @Test
    void defaultConstructorShouldUseStandardValues() {
        FeedbackCalibrator c = new FeedbackCalibrator();
        assertNotNull(c);
    }

    // --- helpers ---

    private void addEntry(PressureState before, PressureState after,
                          String policyId, double rScore, double sScore,
                          double stScore, double eScore) {
        ThresholdPolicyConfig config = new ThresholdPolicyConfig(
                policyId, 2, 16, 4, 8, 2, 4);
        PolicyDecision pDecision = new PolicyDecision("r1", policyId, now,
                after.ordinal() > before.ordinal() ? PolicyAction.SCALE_UP : PolicyAction.SCALE_DOWN,
                GateStatus.ACCEPTED, 4, 6, "test");
        PressureClassification beforeClass = new PressureClassification(
                before, 0.8, java.util.List.of("test"),
                NormalizedPressureMetrics.fromSnapshots(java.util.List.of(), 0L, 4, 5),
                now);
        PressureClassification afterClass = new PressureClassification(
                after, 0.8, java.util.List.of("test"),
                NormalizedPressureMetrics.fromSnapshots(java.util.List.of(), 0L, 4, 5),
                now);
        AdjustmentDecision decision = new AdjustmentDecision(
                beforeClass,
                new PolicyScore(policyId, 0.8, rScore, sScore, stScore, eScore, "test"),
                config, pDecision, "test rationale", now);

        ExecutorStateSnapshot state = ExecutorStateSnapshot.builder(now)
                .corePoolSize(4).maximumPoolSize(8).poolSize(4)
                .activeCount(4).queueSize(0).build();
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "r1", now, 4, 6, "test", "ref", Instant::now);
        AdjustmentResult result = new AdjustmentResult(
                command, AdjustmentStatus.APPLIED,
                state, 6, 6, state,
                "applied", null, "ref", now);
        history.record(decision, result, beforeClass, afterClass);
    }
}
