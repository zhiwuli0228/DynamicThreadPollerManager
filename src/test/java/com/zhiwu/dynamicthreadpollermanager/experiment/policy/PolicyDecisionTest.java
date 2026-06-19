package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.ScaleDecision;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolicyDecisionTest {

    private static final Instant FIXED = Instant.parse("2026-06-04T10:00:00Z");

    @Test
    void shouldExposeAllFields() {
        PolicyDecision decision = new PolicyDecision(
                "run-1", "policy-1", FIXED,
                PolicyAction.SCALE_UP, GateStatus.ACCEPTED,
                8, 12, "scale up triggered");

        assertEquals("run-1", decision.runId());
        assertEquals("policy-1", decision.policyId());
        assertEquals(FIXED, decision.timestamp());
        assertEquals(PolicyAction.SCALE_UP, decision.action());
        assertEquals(GateStatus.ACCEPTED, decision.gateStatus());
        assertEquals(8, decision.currentPoolSize());
        assertEquals(12, decision.proposedPoolSize());
        assertEquals("scale up triggered", decision.reason());
    }

    @Test
    void shouldPreserveReason() {
        PolicyDecision decision = new PolicyDecision(
                "run-1", "policy-1", FIXED,
                PolicyAction.HOLD, GateStatus.HOLD,
                4, 4, "no change needed");
        assertEquals("no change needed", decision.reason());
    }

    @Test
    void shouldRejectBlankRunId() {
        assertThrows(IllegalArgumentException.class, () -> new PolicyDecision(
                "", "policy-1", FIXED,
                PolicyAction.HOLD, GateStatus.HOLD, 4, 4, "reason"));
    }

    @Test
    void shouldRejectBlankPolicyId() {
        assertThrows(IllegalArgumentException.class, () -> new PolicyDecision(
                "run-1", " ", FIXED,
                PolicyAction.HOLD, GateStatus.HOLD, 4, 4, "reason"));
    }

    @Test
    void shouldRejectNullTimestamp() {
        assertThrows(NullPointerException.class, () -> new PolicyDecision(
                "run-1", "policy-1", null,
                PolicyAction.HOLD, GateStatus.HOLD, 4, 4, "reason"));
    }

    @Test
    void shouldRejectNullAction() {
        assertThrows(NullPointerException.class, () -> new PolicyDecision(
                "run-1", "policy-1", FIXED,
                null, GateStatus.HOLD, 4, 4, "reason"));
    }

    @Test
    void shouldRejectNullGateStatus() {
        assertThrows(NullPointerException.class, () -> new PolicyDecision(
                "run-1", "policy-1", FIXED,
                PolicyAction.HOLD, null, 4, 4, "reason"));
    }

    @Test
    void shouldRejectNegativeCurrentPoolSize() {
        assertThrows(IllegalArgumentException.class, () -> new PolicyDecision(
                "run-1", "policy-1", FIXED,
                PolicyAction.SCALE_UP, GateStatus.ACCEPTED, -1, 2, "reason"));
    }

    @Test
    void shouldRejectNegativeProposedPoolSize() {
        assertThrows(IllegalArgumentException.class, () -> new PolicyDecision(
                "run-1", "policy-1", FIXED,
                PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 4, -1, "reason"));
    }

    @Test
    void shouldRejectBlankReason() {
        assertThrows(IllegalArgumentException.class, () -> new PolicyDecision(
                "run-1", "policy-1", FIXED,
                PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 4, 8, ""));
    }

    @Test
    void shouldConvertAcceptedScaleUpToScaleDecision() {
        PolicyDecision decision = new PolicyDecision(
                "run-1", "policy-1", FIXED,
                PolicyAction.SCALE_UP, GateStatus.ACCEPTED,
                8, 12, "scale up triggered");

        ScaleDecision converted = decision.toScaleDecision();

        assertEquals(FIXED, converted.timestamp());
        assertEquals("run-1", converted.runId());
        assertEquals(8, converted.currentPoolSize());
        assertEquals(12, converted.proposedPoolSize());
        assertEquals("scale up triggered", converted.reasoning());
    }

    @Test
    void shouldConvertAcceptedScaleDownToScaleDecision() {
        PolicyDecision decision = new PolicyDecision(
                "run-1", "policy-1", FIXED,
                PolicyAction.SCALE_DOWN, GateStatus.ACCEPTED,
                8, 6, "scale down triggered");

        ScaleDecision converted = decision.toScaleDecision();

        assertEquals(6, converted.proposedPoolSize());
    }

    @Test
    void shouldConvertCappedScaleUpToScaleDecision() {
        PolicyDecision decision = new PolicyDecision(
                "run-1", "policy-1", FIXED,
                PolicyAction.SCALE_UP, GateStatus.CAPPED,
                8, 32, "capped at max");

        ScaleDecision converted = decision.toScaleDecision();

        assertEquals(32, converted.proposedPoolSize());
    }

    @Test
    void shouldRefuseToConvertHoldDecision() {
        PolicyDecision decision = new PolicyDecision(
                "run-1", "policy-1", FIXED,
                PolicyAction.HOLD, GateStatus.HOLD,
                8, 8, "hold");

        assertThrows(IllegalStateException.class, decision::toScaleDecision);
    }

    @Test
    void shouldRefuseToConvertRejectedDecision() {
        PolicyDecision decision = new PolicyDecision(
                "run-1", "policy-1", FIXED,
                PolicyAction.SCALE_UP, GateStatus.REJECTED,
                8, 8, "rejected");

        assertThrows(IllegalStateException.class, decision::toScaleDecision);
    }

    @Test
    void shouldRefuseToConvertHoldActionEvenWithAcceptedStatus() {
        PolicyDecision decision = new PolicyDecision(
                "run-1", "policy-1", FIXED,
                PolicyAction.HOLD, GateStatus.ACCEPTED,
                8, 8, "hold action");

        assertThrows(IllegalStateException.class, decision::toScaleDecision);
    }
}
