package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ScaleAdjustmentCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Detects oscillation patterns spanning multiple executors in a group.
 * Detection is advisory — flagged in GroupCoordinationResult but does
 * NOT block the adjustment (unlike per-executor OscillationDetector).
 */
public final class CrossExecutorOscillationDetector {

    private final int windowSize;

    public CrossExecutorOscillationDetector(int windowSize) {
        if (windowSize < 4) {
            throw new IllegalArgumentException(
                    "windowSize must be >= 4, was " + windowSize);
        }
        this.windowSize = windowSize;
    }

    public boolean wouldCrossOscillate(
            ScaleAdjustmentCommand pending,
            String sourceId,
            GroupCoordinationHistory history) {

        int pendingDelta = pending.targetPoolSize() - pending.currentPoolSize();
        if (pendingDelta <= 0) {
            return false;
        }

        List<GroupCoordinationEntry> recent = history.recent(windowSize);
        if (recent.size() < 2) {
            return false;
        }

        if (detectLockstep(pendingDelta, sourceId, recent)) {
            return true;
        }
        if (detectPingPong(recent)) {
            return true;
        }
        if (detectThrashing(recent)) {
            return true;
        }

        return false;
    }

    private boolean detectLockstep(
            int pendingDelta, String sourceId,
            List<GroupCoordinationEntry> recent) {

        Map<String, List<Integer>> deltasByExecutor = new LinkedHashMap<>();
        for (GroupCoordinationEntry entry : recent) {
            int delta = entry.command().targetPoolSize()
                    - entry.command().currentPoolSize();
            deltasByExecutor
                    .computeIfAbsent(entry.executorId(), k -> new ArrayList<>())
                    .add(delta);
        }

        for (Map.Entry<String, List<Integer>> e : deltasByExecutor.entrySet()) {
            if (e.getKey().equals(sourceId)) {
                continue;
            }
            List<Integer> deltas = e.getValue();
            int alternations = 0;
            Integer lastSourceDelta = null;
            Integer lastOtherDelta = null;
            for (int i = recent.size() - 1; i >= 0; i--) {
                GroupCoordinationEntry entry = recent.get(i);
                int delta = entry.command().targetPoolSize()
                        - entry.command().currentPoolSize();
                if (entry.executorId().equals(sourceId)) {
                    if (lastOtherDelta != null
                            && Math.signum(delta) != Math.signum(lastOtherDelta)) {
                        alternations++;
                    }
                    lastSourceDelta = delta;
                } else if (entry.executorId().equals(e.getKey())) {
                    if (lastSourceDelta != null
                            && Math.signum(delta) != Math.signum(lastSourceDelta)) {
                        alternations++;
                    }
                    lastOtherDelta = delta;
                }
            }
            if (alternations >= 2) {
                return true;
            }
        }
        return false;
    }

    private boolean detectPingPong(List<GroupCoordinationEntry> recent) {
        Map<String, Integer> pairSwapCount = new HashMap<>();
        for (int i = 0; i < recent.size() - 1; i++) {
            GroupCoordinationEntry a = recent.get(i);
            GroupCoordinationEntry b = recent.get(i + 1);
            if (!a.executorId().equals(b.executorId())) {
                int aDelta = a.command().targetPoolSize()
                        - a.command().currentPoolSize();
                int bDelta = b.command().targetPoolSize()
                        - b.command().currentPoolSize();
                if (Math.signum(aDelta) != Math.signum(bDelta)
                        && Math.abs(aDelta) == Math.abs(bDelta)) {
                    String pair = a.executorId() + "<->" + b.executorId();
                    pairSwapCount.merge(pair, 1, Integer::sum);
                }
            }
        }
        return pairSwapCount.values().stream().anyMatch(c -> c >= 3);
    }

    private boolean detectThrashing(List<GroupCoordinationEntry> recent) {
        Map<String, Integer> preemptCount = new HashMap<>();
        for (GroupCoordinationEntry entry : recent) {
            for (String conflict : entry.result().conflicts()) {
                String[] parts = conflict.split(":");
                if (parts.length > 0) {
                    String pair = entry.executorId() + "->" + parts[0];
                    preemptCount.merge(pair, 1, Integer::sum);
                }
            }
        }
        return preemptCount.values().stream().anyMatch(c -> c >= 3);
    }

    public Optional<String> detectedPattern(GroupCoordinationHistory history) {
        List<GroupCoordinationEntry> recent = history.recent(windowSize);
        if (recent.size() < 2) {
            return Optional.empty();
        }

        Map<String, List<Integer>> deltasByExecutor = new LinkedHashMap<>();
        for (GroupCoordinationEntry entry : recent) {
            int delta = entry.command().targetPoolSize()
                    - entry.command().currentPoolSize();
            deltasByExecutor
                    .computeIfAbsent(entry.executorId(), k -> new ArrayList<>())
                    .add(delta);
        }
        for (Map.Entry<String, List<Integer>> e : deltasByExecutor.entrySet()) {
            if (deltasByExecutor.size() > 1) {
                return Optional.of("lockstep-counter-adjustment:" + e.getKey());
            }
        }

        Map<String, Integer> preemptCount = new HashMap<>();
        for (GroupCoordinationEntry entry : recent) {
            for (String conflict : entry.result().conflicts()) {
                String pair = entry.executorId() + " preempts " + conflict;
                preemptCount.merge(pair, 1, Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> e : preemptCount.entrySet()) {
            if (e.getValue() >= 3) {
                return Optional.of("priority-thrashing:" + e.getKey());
            }
        }

        return Optional.empty();
    }
}
