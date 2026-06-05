package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import com.zhiwu.dynamicthreadpollermanager.experiment.policy.GateStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyAction;
import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregates a list of {@link ReplayDecisionEvidence} for a single
 * (run, policyConfigLabel) pair into a {@link ReplayRunSummary}, and
 * a list of run summaries for a single (scenarioProfile,
 * policyConfigLabel) pair into a {@link ReplayScenarioSummary}.
 *
 * <p>Counting rules:
 * <ul>
 *   <li>{@code decisionCount + skippedCount == evidenceCount}.</li>
 *   <li>{@code directionFlipCount} and {@code alternatingStreakMax}
 *       are computed on the non-{@code HOLD} subsequence only.</li>
 *   <li>{@code holdRatio = holdCount / evidenceCount} (zero when
 *       {@code evidenceCount == 0}).</li>
 *   <li>{@code cappedRatio = cappedCount / evidenceCount} (zero when
 *       {@code evidenceCount == 0}).</li>
 * </ul>
 */
public final class ReplaySummaryBuilder {

    public ReplayRunSummary build(String runId,
                                  String scenarioId,
                                  ScenarioProfile profile,
                                  String policyConfigLabel,
                                  List<ReplayDecisionEvidence> decisions,
                                  List<String> skippedReasons) {
        Objects.requireNonNull(decisions, "decisions must not be null");
        Objects.requireNonNull(skippedReasons, "skippedReasons must not be null");
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId must not be blank");
        }
        Objects.requireNonNull(profile, "profile must not be null");
        if (policyConfigLabel == null || policyConfigLabel.isBlank()) {
            throw new IllegalArgumentException("policyConfigLabel must not be blank");
        }

        int evidenceCount = decisions.size() + skippedReasons.size();
        int decisionCount = decisions.size();
        int skippedCount = skippedReasons.size();

        int scaleUp = 0;
        int scaleDown = 0;
        int hold = 0;
        int accepted = 0;
        int capped = 0;
        int gateHold = 0;
        int rejected = 0;

        List<PolicyAction> nonHoldSequence = new ArrayList<>(decisions.size());
        for (ReplayDecisionEvidence ev : decisions) {
            switch (ev.action()) {
                case SCALE_UP -> {
                    scaleUp++;
                    nonHoldSequence.add(PolicyAction.SCALE_UP);
                }
                case SCALE_DOWN -> {
                    scaleDown++;
                    nonHoldSequence.add(PolicyAction.SCALE_DOWN);
                }
                case HOLD -> {
                    hold++;
                }
            }
            switch (ev.gateStatus()) {
                case ACCEPTED -> accepted++;
                case CAPPED -> capped++;
                case HOLD -> gateHold++;
                case REJECTED -> rejected++;
            }
        }

        int directionFlips = countDirectionFlips(nonHoldSequence);
        int alternatingStreak = countAlternatingStreakMax(nonHoldSequence);

        double holdRatio = evidenceCount == 0 ? 0.0 : (double) hold / evidenceCount;
        double cappedRatio = evidenceCount == 0 ? 0.0 : (double) capped / evidenceCount;

        return new ReplayRunSummary(
                runId, scenarioId, profile, policyConfigLabel,
                evidenceCount, decisionCount, skippedCount,
                scaleUp, scaleDown, hold,
                accepted, capped, gateHold, rejected,
                directionFlips, alternatingStreak,
                holdRatio, cappedRatio,
                decisions, skippedReasons);
    }

    public static ReplayScenarioSummary summarizeScenario(ScenarioProfile profile,
                                                           String policyConfigLabel,
                                                           List<ReplayRunSummary> runs) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(runs, "runs must not be null");

        List<String> runIds = new ArrayList<>(runs.size());
        int totalEvidence = 0;
        int totalDecisions = 0;
        int totalSkipped = 0;
        int aggregateFlips = 0;
        int aggregateStreakMax = 0;
        long totalHoldWeighted = 0;
        long totalCappedWeighted = 0;

        for (ReplayRunSummary run : runs) {
            if (run.scenarioProfile() != profile) {
                throw new IllegalArgumentException(
                        "run scenarioProfile " + run.scenarioProfile()
                                + " does not match expected " + profile);
            }
            if (!run.policyConfigLabel().equals(policyConfigLabel)) {
                throw new IllegalArgumentException(
                        "run policyConfigLabel " + run.policyConfigLabel()
                                + " does not match expected " + policyConfigLabel);
            }
            runIds.add(run.runId());
            totalEvidence += run.evidenceCount();
            totalDecisions += run.decisionCount();
            totalSkipped += run.skippedCount();
            aggregateFlips += run.directionFlipCount();
            if (run.alternatingStreakMax() > aggregateStreakMax) {
                aggregateStreakMax = run.alternatingStreakMax();
            }
            totalHoldWeighted += Math.round(run.holdRatio() * run.evidenceCount());
            totalCappedWeighted += Math.round(run.cappedRatio() * run.evidenceCount());
        }

        double aggregateHoldRatio = totalEvidence == 0 ? 0.0 : (double) totalHoldWeighted / totalEvidence;
        double aggregateCappedRatio = totalEvidence == 0 ? 0.0 : (double) totalCappedWeighted / totalEvidence;

        return new ReplayScenarioSummary(
                profile, policyConfigLabel, runIds, runs,
                totalEvidence, totalDecisions, totalSkipped,
                aggregateFlips, aggregateStreakMax,
                aggregateHoldRatio, aggregateCappedRatio);
    }

    static int countDirectionFlips(List<PolicyAction> sequence) {
        int flips = 0;
        PolicyAction previous = null;
        for (PolicyAction action : sequence) {
            if (previous != null && previous != action) {
                flips++;
            }
            previous = action;
        }
        return flips;
    }

    static int countAlternatingStreakMax(List<PolicyAction> sequence) {
        int best = 0;
        int current = 0;
        PolicyAction previous = null;
        for (PolicyAction action : sequence) {
            if (previous == null || previous == action) {
                current = 1;
            } else {
                current++;
            }
            if (current > best) {
                best = current;
            }
            previous = action;
        }
        return best;
    }
}
