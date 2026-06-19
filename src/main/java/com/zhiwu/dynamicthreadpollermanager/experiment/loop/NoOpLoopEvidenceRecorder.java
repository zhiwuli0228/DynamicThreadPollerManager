package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureClassification;

import java.util.List;

/**
 * Stub implementation that discards all evidence. Used in Change 1;
 * Change 2 provides InMemoryLoopEvidenceRecorder.
 */
public final class NoOpLoopEvidenceRecorder implements LoopEvidenceRecorder {

    @Override
    public void recordIteration(LoopSession session, int iterationIndex,
                                AdjustmentDecision decision, AdjustmentResult result,
                                PressureClassification beforeClassification) {
        // no-op
    }

    @Override
    public void recordSessionStart(LoopSession session) {
        // no-op
    }

    @Override
    public void recordSessionEnd(LoopSession session) {
        // no-op
    }

    @Override
    public List<LoopIterationEvidence> getIterationEvidence(String sessionId) {
        return List.of();
    }
}
