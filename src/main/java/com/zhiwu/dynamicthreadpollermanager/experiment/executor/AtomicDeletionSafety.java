package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicDeletionSafety implements DeletionSafety {

    private final ConcurrentHashMap<String, AtomicInteger> refCounts = new ConcurrentHashMap<>();

    @Override
    public void acquire(String executorName) {
        refCounts.computeIfAbsent(executorName, k -> new AtomicInteger(0)).incrementAndGet();
    }

    @Override
    public void release(String executorName) {
        AtomicInteger counter = refCounts.get(executorName);
        if (counter == null) {
            throw new IllegalStateException(
                    "Cannot release '" + executorName + "': never acquired");
        }
        int current = counter.decrementAndGet();
        if (current < 0) {
            counter.incrementAndGet();
            throw new IllegalStateException(
                    "Cannot release '" + executorName + "': refCount already 0");
        }
        if (current == 0) {
            refCounts.remove(executorName);
        }
    }

    @Override
    public int referenceCount(String executorName) {
        AtomicInteger counter = refCounts.get(executorName);
        return counter != null ? counter.get() : 0;
    }

    @Override
    public boolean canRemove(String executorName, ExecutorRegistry registry) {
        if (referenceCount(executorName) > 0) {
            return false;
        }
        return registry.get(executorName)
                .map(executor -> executor.isTerminated())
                .orElse(true);
    }
}
