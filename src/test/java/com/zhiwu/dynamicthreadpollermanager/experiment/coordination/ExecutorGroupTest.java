package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorAdjustmentAdapter;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.InMemoryAdjustableExecutorProbe;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ExecutorGroupTest {

    private final Supplier<Instant> clock = Instant::now;

    @Test
    void shouldConstructWithValidMembers() {
        ManagedExecutor execA = new ManagedExecutor(2, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        ManagedExecutor execB = new ManagedExecutor(3, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));

        ExecutorGroupConfig config = ExecutorGroupConfig.defaults("test", 10);
        Map<String, ManagedExecutor> members = Map.of("exec-A", execA, "exec-B", execB);
        Map<String, ExecutorAdjustmentAdapter> adapters = Map.of(
                "exec-A", new InMemoryAdjustableExecutorProbe(2, 8, 10, clock),
                "exec-B", new InMemoryAdjustableExecutorProbe(3, 8, 10, clock));

        ExecutorGroup group = new ExecutorGroup(config, members, adapters, clock);

        assertEquals(2, group.size());
        assertTrue(group.contains("exec-A"));
        assertTrue(group.contains("exec-B"));
        assertFalse(group.contains("exec-C"));
        assertNotNull(group.getCoordinator());
        assertNotNull(group.getHistory());
        assertEquals(5, group.getBudget().totalAllocatedThreads()); // 2+3
    }

    @Test
    void shouldRejectBudgetExceeded() {
        ManagedExecutor execA = new ManagedExecutor(6, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        ManagedExecutor execB = new ManagedExecutor(6, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));

        ExecutorGroupConfig config = ExecutorGroupConfig.defaults("test", 10);
        Map<String, ManagedExecutor> members = Map.of("exec-A", execA, "exec-B", execB);
        Map<String, ExecutorAdjustmentAdapter> adapters = Map.of(
                "exec-A", new InMemoryAdjustableExecutorProbe(6, 8, 10, clock),
                "exec-B", new InMemoryAdjustableExecutorProbe(6, 8, 10, clock));

        assertThrows(IllegalArgumentException.class, () ->
                new ExecutorGroup(config, members, adapters, clock));
    }

    @Test
    void shouldRejectEmptyMembers() {
        ExecutorGroupConfig config = ExecutorGroupConfig.defaults("test", 10);
        assertThrows(IllegalArgumentException.class, () ->
                new ExecutorGroup(config, Map.of(), Map.of(), clock));
    }

    @Test
    void getMembersShouldBeImmutable() {
        ManagedExecutor exec = new ManagedExecutor(2, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10));
        ExecutorGroupConfig config = ExecutorGroupConfig.defaults("test", 10);
        Map<String, ManagedExecutor> members = Map.of("exec-A", exec);
        Map<String, ExecutorAdjustmentAdapter> adapters = Map.of(
                "exec-A", new InMemoryAdjustableExecutorProbe(2, 8, 10, clock));

        ExecutorGroup group = new ExecutorGroup(config, members, adapters, clock);
        assertThrows(UnsupportedOperationException.class, () ->
                group.getMembers().put("exec-B", exec));
    }
}
