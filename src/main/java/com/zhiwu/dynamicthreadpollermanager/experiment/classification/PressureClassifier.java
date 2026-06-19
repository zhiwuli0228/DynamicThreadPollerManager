package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;

import java.util.List;

/**
 * Classifies executor pressure state from a time series of snapshots.
 */
public interface PressureClassifier {

    /**
     * @param snapshots         time series of observed snapshots
     * @param config            classifier configuration
     * @param rejectedTaskCount externally provided rejection count (from
     *                          ScenarioRunOutcome or ManagedExecutor)
     * @param totalDurationMs   wall-clock duration of the run in milliseconds
     * @return classification with state, confidence, evidence, and computed metrics
     */
    PressureClassification classify(
            List<ObservedSnapshot> snapshots,
            ClassifierConfig config,
            long rejectedTaskCount,
            long totalDurationMs);
}
