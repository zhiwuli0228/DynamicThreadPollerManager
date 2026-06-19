package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Thread-safe storage and query for group-level coordination records.
 * Uses the same CopyOnWriteArrayList pattern as AdjustmentHistory (v0.14.0).
 */
public final class GroupCoordinationHistory {

    private final List<GroupCoordinationEntry> entries = new CopyOnWriteArrayList<>();

    public void record(GroupCoordinationEntry entry) {
        entries.add(Objects.requireNonNull(entry, "entry must not be null"));
    }

    public List<GroupCoordinationEntry> recent(int count) {
        int from = Math.max(0, entries.size() - count);
        return List.copyOf(entries.subList(from, entries.size()));
    }

    public List<GroupCoordinationEntry> byExecutor(String executorId) {
        Objects.requireNonNull(executorId, "executorId must not be null");
        return entries.stream()
                .filter(e -> e.executorId().equals(executorId))
                .collect(Collectors.toUnmodifiableList());
    }

    public int totalCoordinationCount() {
        return entries.size();
    }

    public long rejectedCount() {
        return entries.stream()
                .filter(e -> e.result().outcome() == CoordinationOutcome.REJECTED)
                .count();
    }

    public long modifiedCount() {
        return entries.stream()
                .filter(e -> e.result().outcome() == CoordinationOutcome.MODIFIED
                        || e.result().outcome() == CoordinationOutcome.CAPPED)
                .count();
    }

    public long preemptionCount() {
        return entries.stream()
                .filter(e -> !e.result().conflicts().isEmpty())
                .count();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
    }
}
