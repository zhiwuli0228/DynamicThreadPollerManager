package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import java.util.Objects;

/**
 * Compares three {@link ReplayRunSummary} instances (one per fixed
 * config: {@code default}, {@code conservative}, {@code aggressive})
 * and produces a {@link SensitivityComparison} with signed deltas
 * against {@code default}.
 *
 * <p>The analyzer is a pure data transformer; it never inspects the
 * policy layer and never consults wall-clock time.
 */
public final class ThresholdSensitivityAnalyzer {

    public SensitivityComparison compare(String runId,
                                         ReplayRunSummary defaultSummary,
                                         ReplayRunSummary conservativeSummary,
                                         ReplayRunSummary aggressiveSummary) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        Objects.requireNonNull(defaultSummary, "defaultSummary must not be null");
        Objects.requireNonNull(conservativeSummary, "conservativeSummary must not be null");
        Objects.requireNonNull(aggressiveSummary, "aggressiveSummary must not be null");

        if (!defaultSummary.runId().equals(runId)
                || !conservativeSummary.runId().equals(runId)
                || !aggressiveSummary.runId().equals(runId)) {
            throw new IllegalArgumentException(
                    "all summaries must share runId " + runId);
        }

        if (!SensitivityConfigSet.DEFAULT_LABEL.equals(defaultSummary.policyConfigLabel())
                || !SensitivityConfigSet.CONSERVATIVE_LABEL.equals(conservativeSummary.policyConfigLabel())
                || !SensitivityConfigSet.AGGRESSIVE_LABEL.equals(aggressiveSummary.policyConfigLabel())) {
            throw new IllegalArgumentException(
                    "summaries must be labeled default/conservative/aggressive respectively");
        }

        return new SensitivityComparison(runId, defaultSummary, conservativeSummary, aggressiveSummary);
    }
}
