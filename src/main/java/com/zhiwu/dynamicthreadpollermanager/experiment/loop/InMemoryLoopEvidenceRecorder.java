package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureClassification;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory implementation of {@link LoopEvidenceRecorder}.
 */
public final class InMemoryLoopEvidenceRecorder implements LoopEvidenceRecorder {

    private final List<LoopIterationEvidence> evidence = new CopyOnWriteArrayList<>();

    @Override
    public void recordIteration(LoopSession session, int iterationIndex,
                                AdjustmentDecision decision, AdjustmentResult result,
                                PressureClassification beforeClassification) {
        evidence.add(new LoopIterationEvidence(
                session.sessionId(),
                iterationIndex,
                decision,
                result,
                beforeClassification,
                Instant.now()));
    }

    @Override
    public void recordSessionStart(LoopSession session) {
        // Session lifecycle is tracked via the session object itself;
        // start/end markers are visible through getIterationEvidence filtering.
    }

    @Override
    public void recordSessionEnd(LoopSession session) {
        // Session lifecycle is tracked via the session object itself.
    }

    @Override
    public List<LoopIterationEvidence> getIterationEvidence(String sessionId) {
        return evidence.stream()
                .filter(e -> e.sessionId().equals(sessionId))
                .toList();
    }
}
