package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import java.util.List;
import java.util.Optional;

/**
 * Derives a {@link EvidenceSummary} from a recorded snapshot stream. The
 * builder must not invent pressure values for empty streams and must report
 * time bounds from the recorded snapshots in insertion order.
 */
public interface EvidenceSummaryBuilder {

    EvidenceSummary summarize(String runId, List<ObservedSnapshot> snapshots);
}
