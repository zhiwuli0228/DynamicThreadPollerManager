package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.*;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLoopEvidenceRecorderTest {

    private InMemoryLoopEvidenceRecorder recorder;
    private LoopSession session;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        recorder = new InMemoryLoopEvidenceRecorder();
        var config = LoopConfig.defaults(ThresholdPolicyConfig.defaultAdaptive());
        session = LoopSession.started(config);
    }

    @Test
    void shouldRecordAndRetrieveIterationEvidence() {
        AdjustmentDecision decision = createDecision("policy-A");

        recorder.recordIteration(session, 1, decision, null, null);
        recorder.recordIteration(session, 2, decision, null, null);
        recorder.recordIteration(session, 3, decision, null, null);

        List<LoopIterationEvidence> evidence = recorder.getIterationEvidence(session.sessionId());
        assertEquals(3, evidence.size());
        assertEquals(1, evidence.get(0).iterationIndex());
        assertEquals(2, evidence.get(1).iterationIndex());
        assertEquals(3, evidence.get(2).iterationIndex());
    }

    @Test
    void shouldReturnEmptyListForUnknownSession() {
        List<LoopIterationEvidence> evidence = recorder.getIterationEvidence("unknown-session");
        assertTrue(evidence.isEmpty());
    }

    @Test
    void shouldFilterBySessionId() {
        var config2 = LoopConfig.defaults(ThresholdPolicyConfig.defaultAdaptive());
        LoopSession session2 = LoopSession.started(config2);

        recorder.recordIteration(session, 1, createDecision("A"), null, null);
        recorder.recordIteration(session2, 1, createDecision("B"), null, null);

        assertEquals(1, recorder.getIterationEvidence(session.sessionId()).size());
        assertEquals(1, recorder.getIterationEvidence(session2.sessionId()).size());
    }

    @Test
    void shouldHandleSessionStartAndEnd() {
        assertDoesNotThrow(() -> recorder.recordSessionStart(session));
        assertDoesNotThrow(() -> recorder.recordSessionEnd(session));
    }

    @Test
    void shouldBeThreadSafe() throws Exception {
        int threads = 4;
        int perThread = 25;
        var latch = new CountDownLatch(threads);
        var executor = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            final int threadIdx = t;
            executor.submit(() -> {
                for (int i = 0; i < perThread; i++) {
                    recorder.recordIteration(session, threadIdx * perThread + i,
                            createDecision("policy-A"), null, null);
                }
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();
        assertEquals(threads * perThread,
                recorder.getIterationEvidence(session.sessionId()).size());
    }

    @Test
    void shouldStoreCorrectSessionId() {
        recorder.recordIteration(session, 1, createDecision("policy-A"), null, null);
        LoopIterationEvidence evidence =
                recorder.getIterationEvidence(session.sessionId()).get(0);
        assertEquals(session.sessionId(), evidence.sessionId());
    }

    // --- helpers ---

    private AdjustmentDecision createDecision(String policyId) {
        ThresholdPolicyConfig config = new ThresholdPolicyConfig(
                policyId, 2, 16, 4, 8, 2, 4);
        PolicyDecision pDecision = new PolicyDecision("r1", policyId, now,
                PolicyAction.SCALE_UP, GateStatus.ACCEPTED, 4, 6, "test");
        PressureClassification classification = new PressureClassification(
                PressureState.NORMAL, 0.8, List.of("test"),
                NormalizedPressureMetrics.fromSnapshots(List.of(), 0L, 4, 5),
                now);
        return new AdjustmentDecision(
                classification,
                new PolicyScore(policyId, 0.8, 0.8, 0.8, 0.8, 0.8, "test"),
                config, pDecision, "test rationale", now);
    }
}
