package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OscillationDetectorTest {

    private OscillationDetector detector;
    private AdjustmentHistory history;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        detector = new OscillationDetector();
        history = new AdjustmentHistory();
    }

    @Test
    void shouldUseDefaultWindowSizeAndPatternThreshold() {
        OscillationDetector d = new OscillationDetector();
        // Verify it doesn't throw - defaults to windowSize=6, patternThreshold=2
        assertNotNull(d);
    }

    @Test
    void shouldRejectWindowSizeBelow4() {
        assertThrows(IllegalArgumentException.class, () -> new OscillationDetector(3, 1));
    }

    @Test
    void shouldRejectPatternThresholdBelow1() {
        assertThrows(IllegalArgumentException.class, () -> new OscillationDetector(4, 0));
    }

    @Test
    void shouldReturnFalseForEmptyHistory() {
        AdjustmentDecision pending = createDecision("policy-A", 10);
        assertFalse(detector.wouldOscillate(pending, history));
    }

    @Test
    void shouldReturnFalseForInsufficientData() {
        // Only 3 entries, windowSize=6 → not enough data
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 12);
        addHistoryEntry("policy-A", 14);
        AdjustmentDecision pending = createDecision("policy-A", 16);
        assertFalse(detector.wouldOscillate(pending, history));
    }

    @Test
    void shouldDetectPingPongOscillation() {
        // Targets: [10, 20, 10, 20, 10, 20] — alternating directions
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 20);
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 20);
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 20);
        AdjustmentDecision pending = createDecision("policy-A", 10);
        assertTrue(detector.wouldOscillate(pending, history));
    }

    @Test
    void shouldDetectOverAdjustment() {
        // Targets: [10, 15, 20, 25, 30, 35] — 5 consecutive same direction
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 15);
        addHistoryEntry("policy-A", 20);
        addHistoryEntry("policy-A", 25);
        addHistoryEntry("policy-A", 30);
        addHistoryEntry("policy-A", 35);
        AdjustmentDecision pending = createDecision("policy-A", 40);
        assertTrue(detector.wouldOscillate(pending, history));
    }

    @Test
    void shouldDetectPolicySwitching() {
        // Policies: [A, B, A, C, A, B] — policy A appears 3 times non-consecutively
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-B", 12);
        addHistoryEntry("policy-A", 14);
        addHistoryEntry("policy-C", 16);
        addHistoryEntry("policy-A", 18);
        addHistoryEntry("policy-B", 20);
        AdjustmentDecision pending = createDecision("policy-A", 22);
        assertTrue(detector.wouldOscillate(pending, history));
    }

    @Test
    void shouldNotDetectStableHistory() {
        // Stable adjustments: [10, 15, 15, 15, 15, 15]
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 15);
        addHistoryEntry("policy-A", 15);
        addHistoryEntry("policy-A", 15);
        addHistoryEntry("policy-A", 15);
        addHistoryEntry("policy-A", 15);
        AdjustmentDecision pending = createDecision("policy-A", 15);
        assertFalse(detector.wouldOscillate(pending, history));
    }

    @Test
    void shouldSkipNoOpDecisionsInTargets() {
        // Mix of NO_OP and real decisions: only 3 non-NO_OP entries in history,
        // targets=[10, 12, 14] + pending [12] = 4 targets
        // Directions: (+) (+) (-) = 1 direction change (< patternThreshold=2) → no oscillation
        addHistoryEntry("policy-A", 10);
        addNoOpHistoryEntry();
        addHistoryEntry("policy-A", 12);
        addNoOpHistoryEntry();
        addHistoryEntry("policy-A", 14);
        addNoOpHistoryEntry();
        AdjustmentDecision pending = createDecision("policy-A", 12);
        assertFalse(detector.wouldOscillate(pending, history));
    }

    @Test
    void shouldReturnFalseForNoOpPending() {
        // Even with oscillating history, NO_OP pending never triggers oscillation
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 20);
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 20);
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 20);
        AdjustmentDecision pending = createNoOpDecision();
        assertFalse(detector.wouldOscillate(pending, history));
    }

    @Test
    void detectedPatternShouldReturnPingPong() {
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 20);
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 20);
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 20);
        assertTrue(detector.detectedPattern(history).orElse("").contains("ping-pong"));
    }

    @Test
    void detectedPatternShouldReturnOverAdjustment() {
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 15);
        addHistoryEntry("policy-A", 20);
        addHistoryEntry("policy-A", 25);
        addHistoryEntry("policy-A", 30);
        addHistoryEntry("policy-A", 35);
        assertTrue(detector.detectedPattern(history).orElse("").contains("over-adjustment"));
    }

    @Test
    void detectedPatternShouldReturnEmptyForEmptyHistory() {
        assertTrue(detector.detectedPattern(history).isEmpty());
    }

    @Test
    void shouldAcceptValidWindowSizeAndThreshold() {
        OscillationDetector d = new OscillationDetector(6, 2);
        assertNotNull(d);
    }

    @Test
    void pingPongWithSinglePatternShouldNeedMoreData() {
        // [10, 20, 15, 10] + pending [10] = [10, 20, 15, 10, 10]
        // Directions: (+) (-) (-) (0) = 1 direction change (from + to -)
        // < patternThreshold=2 → not detected
        detector = new OscillationDetector(4, 2);
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 20);
        addHistoryEntry("policy-A", 15);
        addHistoryEntry("policy-A", 10);
        AdjustmentDecision pending = createDecision("policy-A", 10);
        assertFalse(detector.wouldOscillate(pending, history));
    }

    // --- helpers ---

    private AdjustmentDecision createDecision(String policyId, int targetPoolSize) {
        ThresholdPolicyConfig config = new ThresholdPolicyConfig(
                policyId, 2, 16, 4, 8, 2, 4);
        PolicyDecision pDecision = new PolicyDecision("r1", policyId, now,
                PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 4, targetPoolSize, "test");
        PressureClassification classification = createClassification(PressureState.NORMAL);
        return new AdjustmentDecision(
                classification,
                new PolicyScore(policyId, 0.8, 0.8, 0.8, 0.8, 0.8, "test"),
                config, pDecision, "test rationale", now);
    }

    private AdjustmentDecision createNoOpDecision() {
        PolicyDecision pDecision = new PolicyDecision("r1", "noop", now,
                PolicyAction.HOLD, GateStatus.HOLD, 10, 10, "no-op test");
        PressureClassification classification = createClassification(PressureState.NORMAL);
        return new AdjustmentDecision(
                classification, null, null, pDecision, "no-op test", now);
    }

    private void addHistoryEntry(String policyId, int targetPoolSize) {
        AdjustmentDecision decision = createDecision(policyId, targetPoolSize);
        PressureClassification classification = decision.classification();
        ThresholdPolicyConfig config = decision.selectedPolicy();
        PolicyDecision pDecision = decision.policyDecision();

        AdjustmentDecision histDecision = new AdjustmentDecision(
                classification,
                new PolicyScore(policyId, 0.8, 0.8, 0.8, 0.8, 0.8, "test"),
                config, pDecision, "test rationale", now);

        ExecutorStateSnapshot beforeState = ExecutorStateSnapshot.builder(now)
                .corePoolSize(4).maximumPoolSize(8).poolSize(4)
                .activeCount(4).queueSize(0).build();
        ExecutorStateSnapshot afterState = ExecutorStateSnapshot.builder(now)
                .corePoolSize(6).maximumPoolSize(8).poolSize(6)
                .activeCount(6).queueSize(0).build();
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "r1", now, 4, targetPoolSize, "test", "ref", Instant::now);
        AdjustmentResult result = new AdjustmentResult(
                command, AdjustmentStatus.APPLIED,
                beforeState, targetPoolSize, targetPoolSize, afterState,
                "applied", null, "ref", now);
        history.record(histDecision, result, classification, classification);
    }

    private void addNoOpHistoryEntry() {
        AdjustmentDecision decision = createNoOpDecision();
        PressureClassification classification = decision.classification();
        ExecutorStateSnapshot state = ExecutorStateSnapshot.builder(now)
                .corePoolSize(4).maximumPoolSize(8).poolSize(4)
                .activeCount(4).queueSize(0).build();
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.noOp(
                "r1", now, 4, "no-op", "ref", Instant::now);
        AdjustmentResult result = new AdjustmentResult(
                command, AdjustmentStatus.NO_OP,
                state, 4, 4, state,
                "no-op", null, "ref", now);
        history.record(decision, result, classification, classification);
    }

    private PressureClassification createClassification(PressureState state) {
        return new PressureClassification(
                state, 0.8, List.of("test"),
                NormalizedPressureMetrics.fromSnapshots(List.of(), 0L, 4, 5),
                now);
    }
}
