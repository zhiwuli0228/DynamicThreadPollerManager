package com.zhiwu.dynamicthreadpollermanager.experiment.executor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ExecutorRegistry {

    private final ConcurrentHashMap<String, ManagedExecutor> executors = new ConcurrentHashMap<>();
    private final DeletionSafety deletionSafety;

    public ExecutorRegistry(DeletionSafety deletionSafety) {
        this.deletionSafety = Objects.requireNonNull(deletionSafety, "deletionSafety must not be null");
    }

    public void register(String name, ManagedExecutor executor) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        ManagedExecutor existing = executors.putIfAbsent(name, executor);
        if (existing != null) {
            throw new IllegalArgumentException("Executor already registered: " + name);
        }
    }

    public Optional<ManagedExecutor> get(String name) {
        Objects.requireNonNull(name, "name must not be null");
        return Optional.ofNullable(executors.get(name));
    }

    public List<String> list() {
        return List.copyOf(executors.keySet());
    }

    public boolean remove(String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (!deletionSafety.canRemove(name, this)) {
            return false;
        }
        return executors.remove(name) != null;
    }

    public int size() {
        return executors.size();
    }
}
