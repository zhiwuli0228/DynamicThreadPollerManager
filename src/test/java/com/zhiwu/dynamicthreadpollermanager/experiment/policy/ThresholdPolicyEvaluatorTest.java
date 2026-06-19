package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThresholdPolicyEvaluatorTest {

    private static final Instant FIXED = Instant.parse("2026-06-04T10:00:00Z");
    private static final ThresholdPolicyConfig CONFIG =
            new ThresholdPolicyConfig("policy-1", 4, 16, 12, 8, 4, 2);
    private static final ThresholdPolicyEvaluator EVALUATOR = new ThresholdPolicyEvaluator();

    private static PolicyEvaluationInput input(int poolSize, int activeThreads, int queueSize) {
        PressureSnapshot snapshot = new PressureSnapshot(FIXED, activeThreads, poolSize, queueSize, 0L, 0.0);
        return new PolicyEvaluationInput("run-1", snapshot, FIXED);
    }

    @Test
    void shouldScaleUpWhenActiveThreadsReachThreshold() {
        PolicyDecision decision = EVALUATOR.evaluate(input(8, 12, 0), CONFIG);

        assertEquals(PolicyAction.SCALE_UP, decision.action());
        assertEquals(GateStatus.ACCEPTED, decision.gateStatus());
        assertEquals(8, decision.currentPoolSize());
        assertEquals(10, decision.proposedPoolSize());
        assertTrue(decision.reason().contains("active threads"),
                () -> "reason should mention active threads: " + decision.reason());
    }

    @Test
    void shouldScaleUpWhenActiveThreadsExceedThreshold() {
        PolicyDecision decision = EVALUATOR.evaluate(input(8, 20, 0), CONFIG);

        assertEquals(PolicyAction.SCALE_UP, decision.action());
        assertEquals(10, decision.proposedPoolSize());
    }

    @Test
    void shouldScaleUpWhenQueueSizeReachesThreshold() {
        PolicyDecision decision = EVALUATOR.evaluate(input(8, 0, 8), CONFIG);

        assertEquals(PolicyAction.SCALE_UP, decision.action());
        assertEquals(8, decision.currentPoolSize());
        assertEquals(10, decision.proposedPoolSize());
        assertTrue(decision.reason().contains("queue size"),
                () -> "reason should mention queue size: " + decision.reason());
    }

    @Test
    void shouldScaleUpWhenQueueSizeExceedsThreshold() {
        PolicyDecision decision = EVALUATOR.evaluate(input(8, 0, 50), CONFIG);

        assertEquals(PolicyAction.SCALE_UP, decision.action());
        assertEquals(10, decision.proposedPoolSize());
    }

    @Test
    void shouldScaleDownWhenActiveBelowThresholdAndQueueEmpty() {
        PolicyDecision decision = EVALUATOR.evaluate(input(8, 4, 0), CONFIG);

        assertEquals(PolicyAction.SCALE_DOWN, decision.action());
        assertEquals(GateStatus.ACCEPTED, decision.gateStatus());
        assertEquals(8, decision.currentPoolSize());
        assertEquals(6, decision.proposedPoolSize());
        assertTrue(decision.reason().contains("low active threads"),
                () -> "reason should mention low active threads: " + decision.reason());
        assertTrue(decision.reason().contains("empty queue"),
                () -> "reason should mention empty queue: " + decision.reason());
    }

    @Test
    void shouldScaleDownWhenActiveIsZeroAndQueueEmpty() {
        PolicyDecision decision = EVALUATOR.evaluate(input(8, 0, 0), CONFIG);

        assertEquals(PolicyAction.SCALE_DOWN, decision.action());
        assertEquals(6, decision.proposedPoolSize());
    }

    @Test
    void shouldNotScaleDownWhenQueueNotEmpty() {
        PolicyDecision decision = EVALUATOR.evaluate(input(8, 4, 1), CONFIG);

        assertEquals(PolicyAction.HOLD, decision.action());
        assertEquals(8, decision.proposedPoolSize());
    }

    @Test
    void shouldNotScaleDownWhenActiveAboveDownThreshold() {
        PolicyDecision decision = EVALUATOR.evaluate(input(8, 5, 0), CONFIG);

        assertEquals(PolicyAction.HOLD, decision.action());
    }

    @Test
    void shouldHoldOnNormalPressure() {
        PolicyDecision decision = EVALUATOR.evaluate(input(8, 6, 2), CONFIG);

        assertEquals(PolicyAction.HOLD, decision.action());
        assertEquals(GateStatus.HOLD, decision.gateStatus());
        assertEquals(8, decision.currentPoolSize());
        assertEquals(8, decision.proposedPoolSize());
        assertTrue(decision.reason().contains("Normal pressure"),
                () -> "reason should mention normal pressure: " + decision.reason());
    }

    @Test
    void shouldPreferScaleUpWhenBothScaleUpAndScaleDownConditionsAppearTrue() {
        // activeThreads 2 <= scaleDown threshold 4 AND queue 0 would trigger scale-down
        // but activeThreads 2 is also not >= scaleUp threshold 12, so pure scale-down case.
        // Construct a snapshot where the queue is non-empty (blocks scale-down)
        // and active is below the down threshold, queue is 0, but also queue >= scaleUp threshold.
        // That cannot happen with a single boolean. Instead, the design's exact case is
        // "if both scale-up and scale-down conditions appear true, scale-up wins".
        // Force scale-up via queue >= threshold while also keeping active low.
        PolicyDecision decision = EVALUATOR.evaluate(input(8, 2, 8), CONFIG);

        assertEquals(PolicyAction.SCALE_UP, decision.action());
        assertEquals(10, decision.proposedPoolSize());
    }

    @Test
    void shouldPreferScaleUpWhenActiveReachesScaleUpWhileQueueEmpty() {
        // activeThreads 12 (scale-up) AND activeThreads 12 > scaleDown threshold 4 (so scale-down blocked)
        // Demonstrates scale-up precedence when both branches are eligible.
        // For the strictest overlap we lower scaleDown threshold to match.
        ThresholdPolicyConfig overlapping = new ThresholdPolicyConfig("policy-1", 1, 32, 4, 100, 12, 1);
        PolicyDecision decision = EVALUATOR.evaluate(input(8, 12, 0), overlapping);

        assertEquals(PolicyAction.SCALE_UP, decision.action());
        assertEquals(9, decision.proposedPoolSize());
    }

    @Test
    void shouldUseEvaluatedAtAsDecisionTimestamp() {
        Instant evaluatedAt = Instant.parse("2026-01-01T00:00:00Z");
        PressureSnapshot snapshot = new PressureSnapshot(evaluatedAt.minusSeconds(60),
                0, 8, 0, 0L, 0.0);
        PolicyEvaluationInput input = new PolicyEvaluationInput("run-1", snapshot, evaluatedAt);

        PolicyDecision decision = EVALUATOR.evaluate(input, CONFIG);

        assertEquals(evaluatedAt, decision.timestamp());
    }

    @Test
    void shouldProduceDecisionsWithRunIdAndPolicyId() {
        PolicyDecision decision = EVALUATOR.evaluate(input(8, 6, 2), CONFIG);

        assertEquals("run-1", decision.runId());
        assertEquals("policy-1", decision.policyId());
    }

    @Test
    void shouldCapScaleUpAtMaxThroughEvaluator() {
        // current=16, scale-up proposes 18 but max is 16 → cap to 16 equals current → HOLD
        PolicyDecision decision = EVALUATOR.evaluate(input(16, 20, 0), CONFIG);

        assertEquals(PolicyAction.SCALE_UP, decision.action());
        assertEquals(GateStatus.HOLD, decision.gateStatus());
        assertEquals(16, decision.proposedPoolSize());
    }

    @Test
    void shouldCapScaleDownAtMinThroughEvaluator() {
        // current=4, scale-down proposes 2 but min is 4 → cap to 4 equals current → HOLD
        PolicyDecision decision = EVALUATOR.evaluate(input(4, 0, 0), CONFIG);

        assertEquals(PolicyAction.SCALE_DOWN, decision.action());
        assertEquals(GateStatus.HOLD, decision.gateStatus());
        assertEquals(4, decision.proposedPoolSize());
    }

    @Test
    void shouldCapScaleUpButNotEqualToCurrent() {
        // current=15, scale-up proposes 17 (> max 16). Cap to 16 != current 15 → CAPPED
        PolicyDecision decision = EVALUATOR.evaluate(input(15, 20, 0), CONFIG);

        assertEquals(PolicyAction.SCALE_UP, decision.action());
        assertEquals(GateStatus.CAPPED, decision.gateStatus());
        assertEquals(16, decision.proposedPoolSize());
    }

    @Test
    void shouldCapScaleDownButNotEqualToCurrent() {
        // current=5, scale-down proposes 3 (< min 4). Cap to 4 != current 5 → CAPPED
        PolicyDecision decision = EVALUATOR.evaluate(input(5, 0, 0), CONFIG);

        assertEquals(PolicyAction.SCALE_DOWN, decision.action());
        assertEquals(GateStatus.CAPPED, decision.gateStatus());
        assertEquals(4, decision.proposedPoolSize());
    }

    @Test
    void shouldAcceptCustomControlGate() {
        ControlGate recordingGate = (input, config, action, proposed, reason) ->
                new PolicyDecision(input.runId(), config.policyId(), input.evaluatedAt(),
                        action, GateStatus.ACCEPTED,
                        input.snapshot().poolSize(), proposed, "custom");

        ThresholdPolicyEvaluator customEvaluator = new ThresholdPolicyEvaluator(recordingGate);
        PolicyDecision decision = customEvaluator.evaluate(input(8, 12, 0), CONFIG);

        assertEquals(GateStatus.ACCEPTED, decision.gateStatus());
        assertEquals("custom", decision.reason());
    }
}
