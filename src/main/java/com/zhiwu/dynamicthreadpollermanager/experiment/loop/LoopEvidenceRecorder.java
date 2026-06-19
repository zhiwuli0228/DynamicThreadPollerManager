package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureClassification;

import java.util.List;

/**
 * Records evidence for each loop iteration.
 *
 * <h3>Thread-safety contract</h3>
 * Implementations of this interface MUST be safe for concurrent use.
 * {@code recordIteration()} may be called from the loop thread while
 * {@code getIterationEvidence()} is called concurrently from reporting
 * or monitoring threads. {@code recordSessionStart()} and
 * {@code recordSessionEnd()} may be called during lifecycle transitions
 * that overlap with iteration recording. Callers may safely invoke any
 * combination of methods from any number of threads.
 */
public interface LoopEvidenceRecorder {

    void recordIteration(LoopSession session, int iterationIndex,
                         AdjustmentDecision decision, AdjustmentResult result,
                         PressureClassification beforeClassification);

    void recordSessionStart(LoopSession session);

    void recordSessionEnd(LoopSession session);

    List<LoopIterationEvidence> getIterationEvidence(String sessionId);
}
