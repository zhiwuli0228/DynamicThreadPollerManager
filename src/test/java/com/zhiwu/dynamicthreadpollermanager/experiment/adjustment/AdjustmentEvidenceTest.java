package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration-style tests for {@link AdjustmentEvidence}: end-to-end
 * gate + adapter + evidence wiring, plus evidence hygiene.
 */
class AdjustmentEvidenceTest {

    private static final Instant T0 = Instant.parse("2026-06-05T10:00:00Z");
    private static final Supplier<Instant> CLOCK = () -> T0;

    private static ReadinessAssessment ready() {
        return new ReadinessAssessment(
                ReadinessStatus.READY,
                List.of(ScenarioProfile.STEADY, ScenarioProfile.RAMP, ScenarioProfile.BURST),
                List.of(),
                List.of(),
                List.of(),
                ReadinessAssessment.DEFAULT_CONFIG_LABEL,
                List.of("run-1"));
    }

    private static ScaleAdjustmentCommand scaleUp(int from, int to) {
        return ScaleAdjustmentCommand.create("run-1", T0, from, to, "scale up", "policy-1:0", CLOCK);
    }

    @Test
    void shouldBuildEvidenceFromAppliedResult() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        ScaleAdjustmentCommand command = scaleUp(4, 6);
        AdjustmentResult result = probe.apply(command);
        Instant recorded = T0.plusSeconds(1);

        AdjustmentEvidence evidence = new AdjustmentEvidence(
                command,
                result.beforeState(),
                result.requestedPoolSize(),
                result.appliedPoolSize(),
                result.afterState(),
                result.status(),
                result.reason(),
                result.failureCode(),
                result.sourceDecisionRef(),
                result.decisionTimestamp(),
                recorded);

        assertEquals("runtime_adjustment", evidence.evidenceType());
        assertEquals(command.commandId(), evidence.commandId());
        assertEquals("run-1", evidence.runId());
        assertEquals("policy-1:0", evidence.sourceDecisionRef());
        assertEquals(T0, evidence.decisionTimestamp());
        assertEquals(recorded, evidence.recordedTimestamp());
        assertEquals(AdjustmentStatus.APPLIED, evidence.status());
        assertEquals(6, evidence.requestedPoolSize());
        assertEquals(6, evidence.appliedPoolSize());
        assertSame(result.beforeState(), evidence.beforeState());
        assertSame(result.afterState(), evidence.afterState());
    }

    @Test
    void shouldBuildEvidenceFromRejectedResult() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        ScaleAdjustmentCommand command = scaleUp(4, 16); // exceeds max
        AdjustmentResult result = probe.apply(command);
        assertEquals(AdjustmentStatus.REJECTED, result.status());

        AdjustmentEvidence evidence = new AdjustmentEvidence(
                command,
                result.beforeState(),
                result.requestedPoolSize(),
                result.appliedPoolSize(),
                result.afterState(),
                result.status(),
                result.reason(),
                result.failureCode(),
                result.sourceDecisionRef(),
                result.decisionTimestamp(),
                T0.plusSeconds(1));

        assertEquals("runtime_adjustment", evidence.evidenceType());
        assertEquals(AdjustmentStatus.REJECTED, evidence.status());
        assertSame(AdjustmentFailureCode.INVALID_COMMAND, evidence.failureCode());
        // Reason is preserved from the source command in all statuses.
        assertEquals("scale up", evidence.reason());
        assertSame(result.beforeState(), evidence.beforeState());
        assertEquals(4, evidence.appliedPoolSize());
    }

    @Test
    void shouldBuildEvidenceFromFailedResult() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK) {
            @Override
            protected void doSetCorePoolSize(int newCore) {
                throw new IllegalStateException("boom");
            }
        };
        ScaleAdjustmentCommand command = scaleUp(4, 6);
        AdjustmentResult result = probe.apply(command);
        assertEquals(AdjustmentStatus.FAILED, result.status());

        AdjustmentEvidence evidence = new AdjustmentEvidence(
                command,
                result.beforeState(),
                result.requestedPoolSize(),
                result.appliedPoolSize(),
                result.afterState(),
                result.status(),
                result.reason(),
                result.failureCode(),
                result.sourceDecisionRef(),
                result.decisionTimestamp(),
                T0.plusSeconds(1));

        assertEquals(AdjustmentStatus.FAILED, evidence.status());
        assertSame(AdjustmentFailureCode.PROBE_FAILURE, evidence.failureCode());
        assertEquals(4, probe.currentState().corePoolSize());
    }

    @Test
    void shouldDistinguishFromOfflineReplayEvidence() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        ScaleAdjustmentCommand command = scaleUp(4, 6);
        AdjustmentResult result = probe.apply(command);

        AdjustmentEvidence evidence = new AdjustmentEvidence(
                command,
                result.beforeState(),
                result.requestedPoolSize(),
                result.appliedPoolSize(),
                result.afterState(),
                result.status(),
                result.reason(),
                result.failureCode(),
                result.sourceDecisionRef(),
                result.decisionTimestamp(),
                T0.plusSeconds(1));

        assertEquals("runtime_adjustment", evidence.evidenceType());
        assertNotNull(evidence.recordedTimestamp());
        // The evidence type must not be the offline replay mode tag.
        assertFalse(evidence.evidenceType().equals("offline_replay"));
        assertFalse(evidence.evidenceType().contains("replay"));
    }

    @Test
    void shouldRecordAllRequiredFieldsForAppliedEvidence() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        ScaleAdjustmentCommand command = scaleUp(4, 6);
        AdjustmentResult result = probe.apply(command);

        AdjustmentEvidence evidence = new AdjustmentEvidence(
                command,
                result.beforeState(),
                result.requestedPoolSize(),
                result.appliedPoolSize(),
                result.afterState(),
                result.status(),
                result.reason(),
                result.failureCode(),
                result.sourceDecisionRef(),
                result.decisionTimestamp(),
                T0.plusSeconds(1));

        // Spec requirement: evidence MUST include these fields for APPLIED status.
        assertNotNull(evidence.evidenceType());
        assertNotNull(evidence.commandId());
        assertNotNull(evidence.runId());
        assertNotNull(evidence.sourceDecisionRef());
        assertNotNull(evidence.beforeState());
        assertEquals(6, evidence.requestedPoolSize());
        assertEquals(6, evidence.appliedPoolSize());
        assertNotNull(evidence.afterState());
        assertEquals(AdjustmentStatus.APPLIED, evidence.status());
        assertNotNull(evidence.reason());
        assertNotNull(evidence.decisionTimestamp());
    }

    @Test
    void fullPipelineShouldEmitAppliedEvidence() {
        InMemoryAdjustableExecutorProbe probe = new InMemoryAdjustableExecutorProbe(4, 8, 10, CLOCK);
        RuntimeAdjustmentSafetyGate gate = new DefaultRuntimeAdjustmentSafetyGate();
        ScaleAdjustmentCommand command = scaleUp(4, 6);
        SafetyGateDecision decision = gate.evaluate(command, probe.currentState(), ready());
        assertTrue(decision.isAllowed());
        gate.recordApplied(decision);

        AdjustmentResult result = probe.apply(command);
        assertEquals(AdjustmentStatus.APPLIED, result.status());

        AdjustmentEvidence evidence = new AdjustmentEvidence(
                command,
                result.beforeState(),
                result.requestedPoolSize(),
                result.appliedPoolSize(),
                result.afterState(),
                result.status(),
                result.reason(),
                result.failureCode(),
                result.sourceDecisionRef(),
                result.decisionTimestamp(),
                T0.plusSeconds(1));
        assertEquals("runtime_adjustment", evidence.evidenceType());
        assertEquals(6, evidence.appliedPoolSize());
        // APPLIED status MUST NOT carry a failure code.
        assertEquals(null, evidence.failureCode());
    }
}
