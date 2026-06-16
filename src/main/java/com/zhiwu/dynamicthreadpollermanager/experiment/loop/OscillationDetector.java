package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Stateless sliding-window oscillation pattern detector.
 *
 * <p>Detects three oscillation patterns from adjustment history:
 * ping-pong (direction alternation), over-adjustment (consecutive same direction),
 * and policy switching (same policy selected non-consecutively).
 */
public class OscillationDetector {

    private final int windowSize;
    private final int patternThreshold;

    public OscillationDetector(int windowSize, int patternThreshold) {
        if (windowSize < 4) {
            throw new IllegalArgumentException("windowSize must be >= 4, was " + windowSize);
        }
        if (patternThreshold < 1) {
            throw new IllegalArgumentException("patternThreshold must be >= 1, was " + patternThreshold);
        }
        this.windowSize = windowSize;
        this.patternThreshold = patternThreshold;
    }

    public OscillationDetector() {
        this(6, 2);
    }

    /**
     * Check if adding {@code pending} to history would trigger an oscillation pattern.
     *
     * @param pending the decision about to be executed
     * @param history the adjustment history (may be empty)
     * @return true if executing pending would form an oscillation pattern
     */
    public boolean wouldOscillate(AdjustmentDecision pending, AdjustmentHistory history) {
        if (pending.isNoOp()) return false;

        List<HistoryEntry> recent = history.recent(windowSize);
        if (recent.size() < windowSize) return false;

        List<Integer> targets = new ArrayList<>();
        for (HistoryEntry e : recent) {
            if (!e.decision().isNoOp()) {
                targets.add(e.decision().policyDecision().proposedPoolSize());
            }
        }
        targets.add(pending.policyDecision().proposedPoolSize());

        if (targets.size() < 4) return false;

        if (detectPingPong(targets)) return true;
        if (detectOverAdjustment(targets)) return true;
        if (detectPolicySwitching(recent, pending)) return true;

        return false;
    }

    boolean detectPingPong(List<Integer> targets) {
        int directionChanges = 0;
        Integer prevDiff = null;
        for (int i = 1; i < targets.size(); i++) {
            int diff = targets.get(i) - targets.get(i - 1);
            if (diff == 0) continue;
            int sign = diff > 0 ? 1 : -1;
            if (prevDiff != null && sign != prevDiff) {
                directionChanges++;
            }
            prevDiff = sign;
        }
        return directionChanges >= patternThreshold;
    }

    boolean detectOverAdjustment(List<Integer> targets) {
        int consecutiveSame = 1;
        for (int i = 2; i < targets.size(); i++) {
            int diff1 = targets.get(i - 1) - targets.get(i - 2);
            int diff2 = targets.get(i) - targets.get(i - 1);
            if (diff1 != 0 && diff2 != 0
                    && Integer.signum(diff1) == Integer.signum(diff2)) {
                consecutiveSame++;
                if (consecutiveSame >= 3) return true;
            } else {
                consecutiveSame = 1;
            }
        }
        return false;
    }

    boolean detectPolicySwitching(List<HistoryEntry> recent, AdjustmentDecision pending) {
        Set<String> policies = new HashSet<>();
        Map<String, Integer> policyCounts = new HashMap<>();
        for (HistoryEntry e : recent) {
            if (e.decision().selectedPolicy() != null) {
                String pid = e.decision().selectedPolicy().policyId();
                policies.add(pid);
                policyCounts.merge(pid, 1, Integer::sum);
            }
        }
        if (pending.selectedPolicy() != null) {
            String pendingPid = pending.selectedPolicy().policyId();
            policies.add(pendingPid);
            policyCounts.merge(pendingPid, 1, Integer::sum);
        }

        if (policies.size() <= 1) return false;
        return policyCounts.values().stream().anyMatch(c -> c >= 2);
    }

    /**
     * Returns a human-readable description of the detected oscillation pattern.
     */
    public Optional<String> detectedPattern(AdjustmentHistory history) {
        List<HistoryEntry> recent = history.recent(windowSize);
        if (recent.isEmpty()) return Optional.empty();

        List<Integer> targets = recent.stream()
                .filter(e -> !e.decision().isNoOp())
                .map(e -> e.decision().policyDecision().proposedPoolSize())
                .toList();

        if (detectPingPong(targets)) return Optional.of("ping-pong oscillation");
        if (detectOverAdjustment(targets)) return Optional.of("over-adjustment");

        if (recent.size() >= 4 && recent.get(recent.size() - 1).decision().selectedPolicy() != null) {
            List<HistoryEntry> subList = recent.subList(0, recent.size() - 1);
            AdjustmentDecision lastDecision = recent.get(recent.size() - 1).decision();
            if (detectPolicySwitching(subList, lastDecision)) {
                return Optional.of("policy switching oscillation");
            }
        }

        return Optional.empty();
    }
}
