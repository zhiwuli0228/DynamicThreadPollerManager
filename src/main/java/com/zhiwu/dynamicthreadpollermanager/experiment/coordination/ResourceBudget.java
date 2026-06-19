package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe shared resource budget tracking per-executor thread
 * allocations with atomic reserve/release.
 *
 * <p>Invariants (always true after any operation):
 * <ul>
 *   <li>{@code sum(threadAllocations.values()) <= maxTotalThreads}</li>
 *   <li>Each individual allocation {@code >= 0}</li>
 *   <li>{@code availableThreads() >= 0}</li>
 * </ul>
 */
public final class ResourceBudget {

    private final int maxTotalThreads;
    private final int maxTotalQueueCapacity;
    private final Map<String, Integer> threadAllocations;
    private final Map<String, Integer> queueAllocations;

    public ResourceBudget(int maxTotalThreads, int maxTotalQueueCapacity) {
        if (maxTotalThreads < 1) {
            throw new IllegalArgumentException(
                    "maxTotalThreads must be >= 1, was " + maxTotalThreads);
        }
        if (maxTotalQueueCapacity < 0) {
            throw new IllegalArgumentException(
                    "maxTotalQueueCapacity must be >= 0, was " + maxTotalQueueCapacity);
        }
        this.maxTotalThreads = maxTotalThreads;
        this.maxTotalQueueCapacity = maxTotalQueueCapacity;
        this.threadAllocations = new ConcurrentHashMap<>();
        this.queueAllocations = new ConcurrentHashMap<>();
    }

    public synchronized int availableThreads() {
        return maxTotalThreads - totalAllocatedThreads();
    }

    public synchronized int totalAllocatedThreads() {
        return threadAllocations.values().stream()
                .mapToInt(Integer::intValue).sum();
    }

    public int allocatedThreads(String executorId) {
        return threadAllocations.getOrDefault(executorId, 0);
    }

    public synchronized Map<String, Integer> getThreadAllocations() {
        return Map.copyOf(threadAllocations);
    }

    public synchronized void reserve(String executorId, int threadDelta) {
        if (threadDelta > 0 && threadDelta > availableThreads()) {
            throw new IllegalStateException(
                    "Insufficient threads: requested=" + threadDelta
                    + ", available=" + availableThreads());
        }
        int current = threadAllocations.getOrDefault(executorId, 0);
        int newAlloc = current + threadDelta;
        if (newAlloc < 0) {
            throw new IllegalStateException(
                    "Cannot release more than allocated: allocated=" + current
                    + ", delta=" + threadDelta);
        }
        if (newAlloc == 0) {
            threadAllocations.remove(executorId);
        } else {
            threadAllocations.put(executorId, newAlloc);
        }
    }

    public synchronized void release(String executorId, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0, was " + amount);
        }
        reserve(executorId, -amount);
    }

    public synchronized ResourceBudget snapshot() {
        ResourceBudget copy = new ResourceBudget(maxTotalThreads, maxTotalQueueCapacity);
        copy.threadAllocations.putAll(this.threadAllocations);
        copy.queueAllocations.putAll(this.queueAllocations);
        return copy;
    }
}
