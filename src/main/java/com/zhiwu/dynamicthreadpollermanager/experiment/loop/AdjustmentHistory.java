package com.zhiwu.dynamicthreadpollermanager.experiment.loop;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureClassification;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureState;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe store of adjustment records with query and success-counting support.
 */
public final class AdjustmentHistory {

    private final List<HistoryEntry> entries = new CopyOnWriteArrayList<>();

    public void record(AdjustmentDecision decision, AdjustmentResult result,
                       PressureClassification before, PressureClassification after) {
        entries.add(new HistoryEntry(decision, result, before, after, Instant.now()));
    }

    public List<HistoryEntry> recent(int count) {
        int from = Math.max(0, entries.size() - count);
        return List.copyOf(entries.subList(from, entries.size()));
    }

    public List<HistoryEntry> since(Instant timestamp) {
        return entries.stream()
                .filter(e -> e.recordedAt().isAfter(timestamp))
                .toList();
    }

    public int totalAdjustmentCount() {
        return entries.size();
    }

    public int successfulAdjustmentCount() {
        return (int) entries.stream()
                .filter(e -> isImprovement(
                        e.beforeClassification().state(),
                        e.afterClassification().state()))
                .count();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
    }

    /**
     * Higher ordinal = lower pressure = improvement.
     * NORMAL→NORMAL counts as success (maintaining steady state).
     */
    static boolean isImprovement(PressureState before, PressureState after) {
        if (before == after && after == PressureState.NORMAL) return true;
        return after.ordinal() > before.ordinal();
    }
}
