package com.zhiwu.dynamicthreadpollermanager.experiment.classification;

import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.MetricValue;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.ObservedSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.RuntimeObservation;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.ThresholdPolicyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThresholdPolicyScorerTest {

    private ThresholdPolicyScorer scorer;
    private SnapshotPressureClassifier classifier;
    private ClassifierConfig classifierConfig;

    @BeforeEach
    void setUp() {
        scorer = new ThresholdPolicyScorer();
        classifier = new SnapshotPressureClassifier();
        classifierConfig = ClassifierConfig.defaults();
    }

    private static ObservedSnapshot snapshot(
            int activeThreads, int poolSize, int queueSize, long completed) {
        Instant now = Instant.now();
        PressureSnapshot ps = new PressureSnapshot(
                now, activeThreads, poolSize, queueSize, completed, 0.0);
        RuntimeObservation obs = new RuntimeObservation(
                now, MetricValue.present(activeThreads),
                MetricValue.present(queueSize), MetricValue.absent());
        return new ObservedSnapshot("run-1", ps, obs);
    }

    private PressureClassification classifyOverload() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(8, 8, 15, 10L), snapshot(8, 8, 15, 20L),
                snapshot(8, 8, 15, 30L), snapshot(8, 8, 15, 40L),
                snapshot(8, 8, 15, 50L));
        return classifier.classify(
                snapshots, new ClassifierConfig(5, 0.1, 10, 20), 0L, 5000L);
    }

    private PressureClassification classifyUnderUtilized() {
        List<ObservedSnapshot> snapshots = List.of(
                snapshot(0, 8, 0, 0L), snapshot(0, 8, 0, 0L),
                snapshot(0, 8, 0, 0L), snapshot(0, 8, 0, 0L),
                snapshot(0, 8, 0, 0L));
        return classifier.classify(snapshots, classifierConfig, 0L, 5000L);
    }

    @Test
    void shouldFavorAggressivePolicyForOverload() {
        PressureClassification c = classifyOverload();
        assertEquals(PressureState.OVERLOAD, c.state());

        ThresholdPolicyConfig aggressive = new ThresholdPolicyConfig(
                "aggressive", 2, 16, 4, 8, 2, 4);
        ThresholdPolicyConfig conservative = new ThresholdPolicyConfig(
                "conservative", 2, 16, 24, 16, 4, 2);

        PolicyScore aggressiveScore = scorer.score(c, aggressive);
        PolicyScore conservativeScore = scorer.score(c, conservative);

        assertTrue(aggressiveScore.responsivenessScore()
                        > conservativeScore.responsivenessScore(),
                "aggressive responsiveness should be higher for OVERLOAD: "
                        + aggressiveScore.responsivenessScore() + " vs "
                        + conservativeScore.responsivenessScore());
    }

    @Test
    void shouldFavorConservativePolicyForUnderUtilized() {
        PressureClassification c = classifyUnderUtilized();
        assertEquals(PressureState.UNDER_UTILIZED, c.state());

        ThresholdPolicyConfig conservative = new ThresholdPolicyConfig(
                "conservative", 2, 8, 24, 16, 4, 2);
        ThresholdPolicyConfig aggressive = new ThresholdPolicyConfig(
                "aggressive", 4, 16, 4, 8, 2, 4);

        PolicyScore conservativeScore = scorer.score(c, conservative);
        PolicyScore aggressiveScore = scorer.score(c, aggressive);

        assertTrue(conservativeScore.efficiencyScore()
                        > aggressiveScore.efficiencyScore(),
                "conservative efficiency should be higher for UNDER_UTILIZED: "
                        + conservativeScore.efficiencyScore() + " vs "
                        + aggressiveScore.efficiencyScore());
    }

    @Test
    void shouldReduceSafetyForCapacityInsufficientPolicy() {
        PressureClassification c = classifyOverload();

        ThresholdPolicyConfig insufficient = new ThresholdPolicyConfig(
                "small", 1, 2, 4, 8, 2, 2);
        ThresholdPolicyConfig adequate = new ThresholdPolicyConfig(
                "adequate", 2, 16, 4, 8, 2, 4);

        PolicyScore insufficientScore = scorer.score(c, insufficient);
        PolicyScore adequateScore = scorer.score(c, adequate);

        assertTrue(insufficientScore.safetyScore() < adequateScore.safetyScore(),
                "capacity-insufficient policy should have lower safety: "
                        + insufficientScore.safetyScore() + " vs "
                        + adequateScore.safetyScore());
    }

    @Test
    void shouldAllScoresBeInValidRange() {
        PressureClassification c = classifyOverload();

        ThresholdPolicyConfig config = ThresholdPolicyConfig.defaultAdaptive();
        PolicyScore score = scorer.score(c, config);

        assertTrue(score.compositeScore() >= 0.0 && score.compositeScore() <= 1.0);
        assertTrue(score.responsivenessScore() >= 0.0
                && score.responsivenessScore() <= 1.0);
        assertTrue(score.safetyScore() >= 0.0 && score.safetyScore() <= 1.0);
        assertTrue(score.stabilityScore() >= 0.0 && score.stabilityScore() <= 1.0);
        assertTrue(score.efficiencyScore() >= 0.0
                && score.efficiencyScore() <= 1.0);
    }

    @Test
    void shouldCompositeScoreEqualWeightedSum() {
        PressureClassification c = classifyOverload();
        ThresholdPolicyConfig config = ThresholdPolicyConfig.defaultAdaptive();
        PolicyScore score = scorer.score(c, config);

        double expected = score.responsivenessScore() * 0.35
                + score.safetyScore() * 0.30
                + score.stabilityScore() * 0.20
                + score.efficiencyScore() * 0.15;

        assertEquals(expected, score.compositeScore(), 0.001);
    }

    @Test
    void shouldCustomWeightsAffectCompositeScore() {
        ThresholdPolicyScorer customScorer = new ThresholdPolicyScorer(
                0.25, 0.25, 0.25, 0.25);

        PressureClassification c = classifyOverload();
        ThresholdPolicyConfig config = ThresholdPolicyConfig.defaultAdaptive();
        PolicyScore score = customScorer.score(c, config);

        double expected = score.responsivenessScore() * 0.25
                + score.safetyScore() * 0.25
                + score.stabilityScore() * 0.25
                + score.efficiencyScore() * 0.25;

        assertEquals(expected, score.compositeScore(), 0.001);
    }

    @Test
    void shouldRejectWeightsNotSummingToOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new ThresholdPolicyScorer(0.5, 0.5, 0.5, 0.5));
        assertThrows(IllegalArgumentException.class,
                () -> new ThresholdPolicyScorer(0.1, 0.1, 0.1, 0.1));
    }
}
