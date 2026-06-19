package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AntiOscillationGuardTest {

    private static final Instant NOW = Instant.parse("2026-06-05T10:00:00Z");

    private OscillationDetector detector;
    private AdjustmentHistory history;

    @BeforeEach
    void setUp() {
        detector = new OscillationDetector();
        history = new AdjustmentHistory();
    }

    // --- Requirement 4.1: No oscillation allows adjustment ---

    @Test
    void shouldAllowWhenNoOscillation() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 3);

        AdjustmentDecision decision = createDecision("policy-up", 6);
        SafetyGateDecision result = guard.evaluate(decision, history, false);

        assertTrue(result.isAllowed());
        assertFalse(guard.isActivated());
        assertEquals(0, guard.consecutiveOscillations());
    }

    @Test
    void shouldAllowFirstDecision() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 2);

        AdjustmentDecision decision = createDecision("policy-up", 6);
        SafetyGateDecision result = guard.evaluate(decision, history, false);

        assertTrue(result.isAllowed());
        assertEquals(0, guard.consecutiveOscillations());
    }

    // --- Requirement 4.2: Threshold controls activation ---

    @Test
    void shouldNotActivateBelowThreshold() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 3);

        // 6 entries creating ping-pong: 10→20→10→20→10→20
        populatePingPongHistory();

        // 1st oscillation
        AdjustmentDecision d1 = createDecision("policy-x", 10);
        guard.evaluate(d1, history, false);
        assertEquals(1, guard.consecutiveOscillations());
        assertFalse(guard.isActivated());

        // 2nd oscillation
        AdjustmentDecision d2 = createDecision("policy-y", 20);
        guard.evaluate(d2, history, false);
        assertEquals(2, guard.consecutiveOscillations());
        // threshold=3, so not activated yet
        assertFalse(guard.isActivated());
    }

    @Test
    void shouldActivateAtThreshold() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 2);

        populatePingPongHistory();

        // 1st oscillation
        AdjustmentDecision d1 = createDecision("policy-x", 10);
        guard.evaluate(d1, history, false);
        assertFalse(guard.isActivated());

        // 2nd oscillation triggers activation
        AdjustmentDecision d2 = createDecision("policy-y", 20);
        SafetyGateDecision result = guard.evaluate(d2, history, false);

        assertTrue(guard.isActivated());
        assertEquals(2, guard.consecutiveOscillations());
        assertFalse(result.isAllowed());
        assertSame(AdjustmentFailureCode.ANTI_OSCILLATION_ACTIVE, result.failureCode());
    }

    // --- Requirement 4.3: Sustained oscillation blocks non-emergency ---

    @Test
    void shouldBlockNonEmergencyWhenActivated() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 2);

        populatePingPongHistory();
        activateGuard(guard);

        AdjustmentDecision decision = createDecision("policy-up", 6);
        SafetyGateDecision result = guard.evaluate(decision, history, false);

        assertFalse(result.isAllowed());
        assertSame(AdjustmentFailureCode.ANTI_OSCILLATION_ACTIVE, result.failureCode());
        assertNotNull(result.reason());
        assertTrue(result.reason().contains("anti-oscillation guard active"));
    }

    // --- Requirement 4.4: Emergency rollback bypasses guard ---

    @Test
    void shouldAllowEmergencyRollbackWhenGuardActivated() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 2);

        populatePingPongHistory();
        activateGuard(guard);
        assertTrue(guard.isActivated());

        AdjustmentDecision decision = createDecision("policy-down", 4);
        SafetyGateDecision result = guard.evaluate(decision, history, true);

        assertTrue(result.isAllowed());
        // Guard remains activated after emergency bypass
        assertTrue(guard.isActivated());
    }

    @Test
    void shouldBlockNonEmergencyWhenGuardActive() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 2);

        populatePingPongHistory();
        activateGuard(guard);

        // Non-emergency blocked
        AdjustmentDecision nonEmergency = createDecision("policy-down", 4);
        SafetyGateDecision blocked = guard.evaluate(nonEmergency, history, false);
        assertFalse(blocked.isAllowed());

        // Emergency rollback bypasses
        AdjustmentDecision emergency = createDecision("policy-down-2", 4);
        SafetyGateDecision emergencyResult = guard.evaluate(emergency, history, true);
        assertTrue(emergencyResult.isAllowed());
    }

    // --- Requirement 4.5: Guard resets on stable adjustment ---

    @Test
    void shouldResetOnStableAdjustment() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 3);

        populatePingPongHistory();
        // Build up oscillations but not enough to activate
        AdjustmentDecision d1 = createDecision("policy-x", 10);
        guard.evaluate(d1, history, false);
        assertEquals(1, guard.consecutiveOscillations());

        AdjustmentDecision d2 = createDecision("policy-y", 20);
        guard.evaluate(d2, history, false);
        assertEquals(2, guard.consecutiveOscillations());

        // Stable: add more entries in same direction so no oscillation
        addHistoryEntry("policy-stable-1", 20);
        addHistoryEntry("policy-stable-2", 20);
        addHistoryEntry("policy-stable-3", 20);
        addHistoryEntry("policy-stable-4", 20);
        addHistoryEntry("policy-stable-5", 20);
        addHistoryEntry("policy-stable-6", 20);

        AdjustmentDecision stable = createDecision("policy-stable-x", 20);
        SafetyGateDecision result = guard.evaluate(stable, history, false);

        assertTrue(result.isAllowed());
        assertEquals(0, guard.consecutiveOscillations());
        assertFalse(guard.isActivated());
    }

    @Test
    void shouldRemainActiveDuringContinuedOscillation() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 2);

        populatePingPongHistory();
        activateGuard(guard);
        assertTrue(guard.isActivated());

        // Another oscillating decision — guard stays active
        AdjustmentDecision another = createDecision("policy-down-again", 10);
        guard.evaluate(another, history, false);

        assertTrue(guard.isActivated());
        assertEquals(3, guard.consecutiveOscillations());
    }

    // --- Requirement 4.6: No-op decisions do not trigger guard ---

    @Test
    void shouldPassThroughNoOp() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 2);

        AdjustmentDecision noOp = createNoOpDecision();
        SafetyGateDecision result = guard.evaluate(noOp, history, false);

        assertEquals(SafetyGateDecision.Outcome.NO_OP, result.outcome());
        assertEquals(0, guard.consecutiveOscillations());
        assertFalse(guard.isActivated());
    }

    // --- Requirement 4.7: Rejects null parameters ---

    @Test
    void shouldRejectNullDetector() {
        assertThrows(NullPointerException.class, () ->
                new AntiOscillationGuard(null, 2));
    }

    @Test
    void shouldRejectInvalidThreshold() {
        assertThrows(IllegalArgumentException.class, () ->
                new AntiOscillationGuard(detector, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new AntiOscillationGuard(detector, -1));
    }

    @Test
    void shouldRejectNullDecision() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 2);
        assertThrows(NullPointerException.class, () ->
                guard.evaluate(null, history, false));
    }

    @Test
    void shouldRejectNullHistory() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 2);
        AdjustmentDecision decision = createDecision("policy-up", 6);
        assertThrows(NullPointerException.class, () ->
                guard.evaluate(decision, null, false));
    }

    // --- Requirement 4.8: Manual reset ---

    @Test
    void shouldResetManually() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 2);

        populatePingPongHistory();
        activateGuard(guard);
        assertTrue(guard.isActivated());

        guard.reset();
        assertFalse(guard.isActivated());
        assertEquals(0, guard.consecutiveOscillations());
    }

    // --- Requirement 4.9: Configurable block threshold ---

    @Test
    void shouldSupportDifferentThresholds() {
        AntiOscillationGuard strictGuard = new AntiOscillationGuard(detector, 1);

        populatePingPongHistory();
        // Threshold=1: activates on first oscillation
        AdjustmentDecision d1 = createDecision("policy-down", 10);
        SafetyGateDecision result = strictGuard.evaluate(d1, history, false);

        assertTrue(strictGuard.isActivated());
        assertFalse(result.isAllowed());
    }

    @Test
    void shouldExposeBlockThreshold() {
        AntiOscillationGuard guard = new AntiOscillationGuard(detector, 5);
        assertEquals(5, guard.blockThreshold());
    }

    // --- Helpers (matching OscillationDetectorTest patterns) ---

    private AdjustmentDecision createDecision(String policyId, int targetPoolSize) {
        ThresholdPolicyConfig config = new ThresholdPolicyConfig(
                policyId, 2, 16, 4, 8, 2, 4);
        PolicyDecision pDecision = new PolicyDecision("r1", policyId, NOW,
                PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 4, targetPoolSize, "test");
        PressureClassification classification = createClassification(PressureState.NORMAL);
        return new AdjustmentDecision(
                classification,
                new PolicyScore(policyId, 0.8, 0.8, 0.8, 0.8, 0.8, "test"),
                config, pDecision, "test rationale", NOW);
    }

    private AdjustmentDecision createNoOpDecision() {
        PolicyDecision pDecision = new PolicyDecision("r1", "noop", NOW,
                PolicyAction.HOLD, GateStatus.HOLD, 10, 10, "no-op test");
        PressureClassification classification = createClassification(PressureState.NORMAL);
        return new AdjustmentDecision(
                classification, null, null, pDecision, "no-op test", NOW);
    }

    private PressureClassification createClassification(PressureState state) {
        return new PressureClassification(
                state, 0.8, List.of("test"),
                NormalizedPressureMetrics.fromSnapshots(List.of(), 0L, 4, 5),
                NOW);
    }

    private void addHistoryEntry(String policyId, int targetPoolSize) {
        AdjustmentDecision decision = createDecision(policyId, targetPoolSize);
        PressureClassification classification = decision.classification();
        ThresholdPolicyConfig config = decision.selectedPolicy();
        PolicyDecision pDecision = decision.policyDecision();

        AdjustmentDecision histDecision = new AdjustmentDecision(
                classification,
                new PolicyScore(policyId, 0.8, 0.8, 0.8, 0.8, 0.8, "test"),
                config, pDecision, "test rationale", NOW);

        ExecutorStateSnapshot beforeState = ExecutorStateSnapshot.builder(NOW)
                .corePoolSize(4).maximumPoolSize(8).poolSize(4)
                .activeCount(4).queueSize(0).build();
        ExecutorStateSnapshot afterState = ExecutorStateSnapshot.builder(NOW)
                .corePoolSize(6).maximumPoolSize(8).poolSize(6)
                .activeCount(6).queueSize(0).build();
        ScaleAdjustmentCommand command = ScaleAdjustmentCommand.create(
                "r1", NOW, 4, targetPoolSize, "test", "ref", Instant::now);
        AdjustmentResult result = new AdjustmentResult(
                command, AdjustmentStatus.APPLIED,
                beforeState, targetPoolSize, targetPoolSize, afterState,
                "applied", null, "ref", NOW);
        history.record(histDecision, result, classification, classification);
    }

    private void populatePingPongHistory() {
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 20);
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 20);
        addHistoryEntry("policy-A", 10);
        addHistoryEntry("policy-A", 20);
    }

    private void activateGuard(AntiOscillationGuard guard) {
        AdjustmentDecision d1 = createDecision("policy-x", 10);
        guard.evaluate(d1, history, false);
        AdjustmentDecision d2 = createDecision("policy-y", 20);
        guard.evaluate(d2, history, false);
    }
}
