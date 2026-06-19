package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultControlGateTest {

    private static final Instant FIXED = Instant.parse("2026-06-04T10:00:00Z");
    private static final ThresholdPolicyConfig CONFIG =
            new ThresholdPolicyConfig("policy-1", 4, 16, 12, 8, 4, 2);
    private static final DefaultControlGate GATE = new DefaultControlGate();

    private static PolicyEvaluationInput input(int poolSize) {
        PressureSnapshot snapshot = new PressureSnapshot(FIXED, 0, poolSize, 0, 0L, 0.0);
        return new PolicyEvaluationInput("run-1", snapshot, FIXED);
    }

    @Test
    void shouldHoldWhenActionIsHold() {
        PolicyDecision decision = GATE.apply(input(8), CONFIG,
                PolicyAction.HOLD, 8, "policy says hold");

        assertEquals(PolicyAction.HOLD, decision.action());
        assertEquals(GateStatus.HOLD, decision.gateStatus());
        assertEquals(8, decision.currentPoolSize());
        assertEquals(8, decision.proposedPoolSize());
        assertEquals("policy says hold", decision.reason());
    }

    @Test
    void shouldAcceptProposalWithinBounds() {
        PolicyDecision decision = GATE.apply(input(8), CONFIG,
                PolicyAction.SCALE_UP, 10, "scale up safe");

        assertEquals(PolicyAction.SCALE_UP, decision.action());
        assertEquals(GateStatus.ACCEPTED, decision.gateStatus());
        assertEquals(8, decision.currentPoolSize());
        assertEquals(10, decision.proposedPoolSize());
        assertEquals(FIXED, decision.timestamp());
    }

    @Test
    void shouldCapProposalAboveMaximum() {
        PolicyDecision decision = GATE.apply(input(8), CONFIG,
                PolicyAction.SCALE_UP, 100, "scale up too far");

        assertEquals(GateStatus.CAPPED, decision.gateStatus());
        assertEquals(16, decision.proposedPoolSize());
        assertEquals(8, decision.currentPoolSize());
    }

    @Test
    void shouldHoldWhenMaxCapEqualsCurrentSize() {
        PolicyDecision decision = GATE.apply(input(16), CONFIG,
                PolicyAction.SCALE_UP, 100, "already at max");

        assertEquals(GateStatus.HOLD, decision.gateStatus());
        assertEquals(16, decision.currentPoolSize());
        assertEquals(16, decision.proposedPoolSize());
    }

    @Test
    void shouldCapProposalBelowMinimum() {
        PolicyDecision decision = GATE.apply(input(8), CONFIG,
                PolicyAction.SCALE_DOWN, 1, "scale down too far");

        assertEquals(GateStatus.CAPPED, decision.gateStatus());
        assertEquals(4, decision.proposedPoolSize());
        assertEquals(8, decision.currentPoolSize());
    }

    @Test
    void shouldHoldWhenMinCapEqualsCurrentSize() {
        PolicyDecision decision = GATE.apply(input(4), CONFIG,
                PolicyAction.SCALE_DOWN, 0, "already at min");

        assertEquals(GateStatus.HOLD, decision.gateStatus());
        assertEquals(4, decision.currentPoolSize());
        assertEquals(4, decision.proposedPoolSize());
    }

    @Test
    void shouldHoldWhenProposedEqualsCurrent() {
        PolicyDecision decision = GATE.apply(input(8), CONFIG,
                PolicyAction.SCALE_UP, 8, "no-op");

        assertEquals(GateStatus.HOLD, decision.gateStatus());
        assertEquals(8, decision.currentPoolSize());
        assertEquals(8, decision.proposedPoolSize());
    }

    @Test
    void shouldPreserveRunIdPolicyIdAndTimestamp() {
        PolicyDecision decision = GATE.apply(input(8), CONFIG,
                PolicyAction.SCALE_UP, 12, "scale up");

        assertEquals("run-1", decision.runId());
        assertEquals("policy-1", decision.policyId());
        assertEquals(FIXED, decision.timestamp());
    }
}
