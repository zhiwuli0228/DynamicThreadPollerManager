package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureClassification;

import java.util.List;

/**
 * Records evidence for each loop iteration. Change 1 provides a stub;
 * Change 2 provides a concrete implementation.
 */
public interface LoopEvidenceRecorder {

    void recordIteration(LoopSession session, int iterationIndex,
                         AdjustmentDecision decision, AdjustmentResult result,
                         PressureClassification beforeClassification);

    void recordSessionStart(LoopSession session);

    void recordSessionEnd(LoopSession session);

    List<LoopIterationEvidence> getIterationEvidence(String sessionId);
}
