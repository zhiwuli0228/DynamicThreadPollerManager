package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ranks multiple policy configurations by score for a given pressure state.
 */
public final class PolicyRanker {

    private final PolicyScorer scorer;

    public PolicyRanker(PolicyScorer scorer) {
        this.scorer = Objects.requireNonNull(scorer, "scorer must not be null");
    }

    public List<PolicyScore> rank(PressureClassification classification,
                                   List<ThresholdPolicyConfig> candidates) {
        Objects.requireNonNull(classification, "classification must not be null");
        Objects.requireNonNull(candidates, "candidates must not be null");

        if (candidates.isEmpty()) {
            return List.of();
        }

        return candidates.stream()
                .map(config -> scorer.score(classification, config))
                .sorted((a, b) -> Double.compare(b.compositeScore(), a.compositeScore()))
                .toList();
    }

    public PolicyScorer scorer() { return scorer; }

    public Optional<PolicyScore> best(PressureClassification classification,
                                       List<ThresholdPolicyConfig> candidates) {
        List<PolicyScore> ranked = rank(classification, candidates);
        return ranked.isEmpty() ? Optional.empty() : Optional.of(ranked.get(0));
    }
}
