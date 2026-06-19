package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class RollbackAwareAdjustmentAdapterTest {

    private static final String RUN_ID = "test-run";
    private final Supplier<Instant> clock = Instant::now;

    private TestableDelegate delegate;
    private RecordingSafetyGate safetyGate;
    private DegradationConfig config;

    @BeforeEach
    void setUp() {
        delegate = new TestableDelegate(4, 10, 0);
        safetyGate = new RecordingSafetyGate();
        config = new DegradationConfig(50, 0.20, 0.50);
    }

    // --- Requirement 2.1: Rejects null delegate ---

    @Test
    void shouldRejectNullDelegate() {
        assertThrows(NullPointerException.class, () ->
                new RollbackAwareAdjustmentAdapter(null, safetyGate, config, null, clock));
    }

    @Test
    void shouldRejectNullSafetyGate() {
        assertThrows(NullPointerException.class, () ->
                new RollbackAwareAdjustmentAdapter(delegate, null, config, null, clock));
    }

    @Test
    void shouldRejectNullDegradationConfig() {
        assertThrows(NullPointerException.class, () ->
                new RollbackAwareAdjustmentAdapter(delegate, safetyGate, null, null, clock));
    }

    @Test
    void shouldRejectNullClock() {
        assertThrows(NullPointerException.class, () ->
                new RollbackAwareAdjustmentAdapter(delegate, safetyGate, config, null, null));
    }

    // --- Requirement 2.2: currentState delegates ---

    @Test
    void shouldDelegateCurrentState() {
        RollbackAwareAdjustmentAdapter adapter = createAdapter(null);
        ExecutorStateSnapshot state = adapter.currentState();
        assertEquals(4, state.corePoolSize());
        assertEquals(10, state.maximumPoolSize());
        assertEquals(0, state.queueSize());
    }

    // --- Requirement 2.3: Pre-adjustment snapshot captured ---

    @Test
    void shouldCapturePreSnapshotBeforeApply() {
        delegate.setQueueSizeForNextSnapshot(10);
        RollbackAwareAdjustmentAdapter adapter = createAdapter(null);

        ScaleAdjustmentCommand cmd = createCommand(4, 6);
        AdjustmentResult result = adapter.apply(cmd);

        // Before state should have queue=10 (the pre-snapshot)
        assertEquals(10, result.beforeState().queueSize());
    }

    // --- Requirement 2.4: Degradation triggers rollback ---

    @Test
    void shouldTriggerRollbackOnDegradation() {
        // Pre-snapshot: queue=0, post-snapshot: queue=60 (> threshold of 50)
        delegate.setQueueSizeForNextSnapshot(0);
        RollbackAwareAdjustmentAdapter adapter = createAdapter(null);

        // After delegate.apply(), queue jumps to 60
        delegate.setPostApplyQueueSize(60);

        ScaleAdjustmentCommand cmd = createCommand(4, 6);
        AdjustmentResult result = adapter.apply(cmd);

        // Rollback should have been attempted
        assertTrue(safetyGate.evaluateCalled());
        // The rollback command should target the original pool size (4)
        ScaleAdjustmentCommand rollbackCmd = safetyGate.lastEvaluatedCommand();
        assertEquals(4, rollbackCmd.targetPoolSize());
    }

    @Test
    void shouldNotTriggerRollbackWhenNoDegradation() {
        // Pre-snapshot: queue=0, post-snapshot: queue=10 (< threshold of 50)
        delegate.setQueueSizeForNextSnapshot(0);
        RollbackAwareAdjustmentAdapter adapter = createAdapter(null);

        ScaleAdjustmentCommand cmd = createCommand(4, 6);
        AdjustmentResult result = adapter.apply(cmd);

        // No rollback should have been attempted
        assertFalse(safetyGate.evaluateCalled());
        assertEquals(AdjustmentStatus.APPLIED, result.status());
        assertEquals(6, result.appliedPoolSize());
    }

    // --- Requirement 2.5: Rollback bounded to 1 ---

    @Test
    void shouldNotRecurseWhenRollbackDegrades() {
        // First apply: queue goes from 0 to 60 (triggers rollback)
        // Rollback apply: queue stays at 60 (still degraded)
        // But should NOT attempt a second rollback
        delegate.setQueueSizeForNextSnapshot(0);
        delegate.setPostApplyQueueSize(60);
        delegate.setRollbackPostQueueSize(60); // rollback also "degrades"

        AtomicInteger applyCount = new AtomicInteger(0);
        delegate.setOnApply(cmd -> applyCount.incrementAndGet());

        RollbackAwareAdjustmentAdapter adapter = createAdapter(null);

        ScaleAdjustmentCommand cmd = createCommand(4, 6);
        adapter.apply(cmd);

        // apply() called twice: once for original, once for rollback
        assertEquals(2, applyCount.get());
        // safetyGate.evaluate() called once (for the rollback command)
        assertEquals(1, safetyGate.evaluateCallCount());
    }

    // --- Requirement 2.6: Safety gate integration ---

    @Test
    void shouldApplyRollbackWhenGateAllows() {
        safetyGate.setAllowRollback(true);
        delegate.setQueueSizeForNextSnapshot(0);
        delegate.setPostApplyQueueSize(60);

        RollbackAwareAdjustmentAdapter adapter = createAdapter(null);

        ScaleAdjustmentCommand cmd = createCommand(4, 6);
        AdjustmentResult result = adapter.apply(cmd);

        // Rollback was applied, result should be the rollback result
        assertTrue(safetyGate.evaluateCalled());
        assertTrue(safetyGate.recordAppliedCalled());
    }

    @Test
    void shouldReturnOriginalResultWhenGateRejectsRollback() {
        safetyGate.setAllowRollback(false);
        delegate.setQueueSizeForNextSnapshot(0);
        delegate.setPostApplyQueueSize(60);

        RollbackAwareAdjustmentAdapter adapter = createAdapter(null);

        ScaleAdjustmentCommand cmd = createCommand(4, 6);
        AdjustmentResult result = adapter.apply(cmd);

        // Gate rejected rollback, original result returned
        assertTrue(safetyGate.evaluateCalled());
        assertFalse(safetyGate.recordAppliedCalled());
        assertEquals(AdjustmentStatus.APPLIED, result.status());
        assertEquals(6, result.appliedPoolSize());
    }

    // --- Requirement 2.7: Rollback evidence recording ---

    @Test
    void shouldRecordSuccessfulRollbackViaListener() {
        safetyGate.setAllowRollback(true);
        delegate.setQueueSizeForNextSnapshot(0);
        delegate.setPostApplyQueueSize(60);

        AtomicReference<AdjustmentResult> capturedOriginal = new AtomicReference<>();
        AtomicReference<AdjustmentResult> capturedRollback = new AtomicReference<>();

        RollbackAwareAdjustmentAdapter adapter = createAdapter((orig, rb) -> {
            capturedOriginal.set(orig);
            capturedRollback.set(rb);
        });

        ScaleAdjustmentCommand cmd = createCommand(4, 6);
        adapter.apply(cmd);

        assertNotNull(capturedOriginal.get());
        assertNotNull(capturedRollback.get());
        assertEquals(AdjustmentStatus.APPLIED, capturedOriginal.get().status());
    }

    @Test
    void shouldRecordRejectedRollbackViaListener() {
        safetyGate.setAllowRollback(false);
        delegate.setQueueSizeForNextSnapshot(0);
        delegate.setPostApplyQueueSize(60);

        AtomicReference<AdjustmentResult> capturedOriginal = new AtomicReference<>();
        AtomicReference<AdjustmentResult> capturedRollback = new AtomicReference<>();

        RollbackAwareAdjustmentAdapter adapter = createAdapter((orig, rb) -> {
            capturedOriginal.set(orig);
            capturedRollback.set(rb);
        });

        ScaleAdjustmentCommand cmd = createCommand(4, 6);
        adapter.apply(cmd);

        assertNotNull(capturedOriginal.get());
        assertNull(capturedRollback.get()); // null because gate rejected
    }

    // --- Requirement 2.8: Configurable degradation threshold ---

    @Test
    void shouldNotTriggerRollbackWhenBelowThreshold() {
        // Threshold is 50, increase is 49
        DegradationConfig strictConfig = new DegradationConfig(50, 0.20, 0.50);
        delegate.setQueueSizeForNextSnapshot(0);
        delegate.setPostApplyQueueSize(49); // just below threshold

        RollbackAwareAdjustmentAdapter adapter = new RollbackAwareAdjustmentAdapter(
                delegate, safetyGate, strictConfig, null, clock);

        ScaleAdjustmentCommand cmd = createCommand(4, 6);
        AdjustmentResult result = adapter.apply(cmd);

        assertFalse(safetyGate.evaluateCalled());
        assertEquals(AdjustmentStatus.APPLIED, result.status());
    }

    @Test
    void shouldTriggerRollbackWhenAboveThreshold() {
        // Threshold is 50, increase is 51
        DegradationConfig strictConfig = new DegradationConfig(50, 0.20, 0.50);
        delegate.setQueueSizeForNextSnapshot(0);
        delegate.setPostApplyQueueSize(51); // just above threshold

        RollbackAwareAdjustmentAdapter adapter = new RollbackAwareAdjustmentAdapter(
                delegate, safetyGate, strictConfig, null, clock);

        ScaleAdjustmentCommand cmd = createCommand(4, 6);
        adapter.apply(cmd);

        assertTrue(safetyGate.evaluateCalled());
    }

    @Test
    void shouldNotTriggerRollbackWhenQueueDecreases() {
        delegate.setQueueSizeForNextSnapshot(50);
        delegate.setPostApplyQueueSize(0); // queue decreased

        RollbackAwareAdjustmentAdapter adapter = createAdapter(null);

        ScaleAdjustmentCommand cmd = createCommand(4, 6);
        AdjustmentResult result = adapter.apply(cmd);

        assertFalse(safetyGate.evaluateCalled());
        assertEquals(AdjustmentStatus.APPLIED, result.status());
    }

    // --- Helpers ---

    private RollbackAwareAdjustmentAdapter createAdapter(
            java.util.function.BiConsumer<AdjustmentResult, AdjustmentResult> listener) {
        return new RollbackAwareAdjustmentAdapter(
                delegate, safetyGate, config, listener, clock);
    }

    private ScaleAdjustmentCommand createCommand(int current, int target) {
        return ScaleAdjustmentCommand.create(
                RUN_ID, clock.get(), current, target,
                "test adjustment", "test-ref", clock);
    }

    /**
     * Test delegate that allows controlling queue size for
     * pre-snapshot and post-snapshot.
     */
    private static class TestableDelegate implements ExecutorAdjustmentAdapter {
        private int corePoolSize;
        private final int maximumPoolSize;
        private int queueSize;
        private int postApplyQueueSize;
        private int rollbackPostQueueSize;
        private boolean rollbackMode;
        private java.util.function.Consumer<ScaleAdjustmentCommand> onApply;

        TestableDelegate(int corePoolSize, int maximumPoolSize, int queueSize) {
            this.corePoolSize = corePoolSize;
            this.maximumPoolSize = maximumPoolSize;
            this.queueSize = queueSize;
            this.postApplyQueueSize = queueSize;
            this.rollbackPostQueueSize = queueSize;
        }

        void setQueueSizeForNextSnapshot(int queueSize) {
            this.queueSize = queueSize;
        }

        void setPostApplyQueueSize(int postApplyQueueSize) {
            this.postApplyQueueSize = postApplyQueueSize;
        }

        void setRollbackPostQueueSize(int rollbackPostQueueSize) {
            this.rollbackPostQueueSize = rollbackPostQueueSize;
        }

        void setOnApply(java.util.function.Consumer<ScaleAdjustmentCommand> onApply) {
            this.onApply = onApply;
        }

        @Override
        public ExecutorStateSnapshot currentState() {
            int q = rollbackMode ? rollbackPostQueueSize : queueSize;
            return ExecutorStateSnapshot.builder(Instant.now())
                    .corePoolSize(corePoolSize)
                    .maximumPoolSize(maximumPoolSize)
                    .queueSize(q)
                    .queueCapacity(100)
                    .build();
        }

        @Override
        public AdjustmentResult apply(ScaleAdjustmentCommand command) {
            if (onApply != null) {
                onApply.accept(command);
            }

            ExecutorStateSnapshot before = currentState();

            // Simulate applying the command
            int oldCore = corePoolSize;
            corePoolSize = command.targetPoolSize();

            // After apply, set queue to post-apply value
            if (!rollbackMode) {
                queueSize = postApplyQueueSize;
            } else {
                queueSize = rollbackPostQueueSize;
            }

            // Next currentState() call will use the new queue size
            // But we need to return the after state now
            ExecutorStateSnapshot after = ExecutorStateSnapshot.builder(Instant.now())
                    .corePoolSize(corePoolSize)
                    .maximumPoolSize(maximumPoolSize)
                    .queueSize(queueSize)
                    .queueCapacity(100)
                    .build();

            // If this looks like a rollback command, set rollback mode
            if (command.reason() != null && command.reason().contains("rollback")) {
                rollbackMode = true;
            }

            return new AdjustmentResult(
                    command,
                    AdjustmentStatus.APPLIED,
                    before,
                    command.targetPoolSize(),
                    after.corePoolSize(),
                    after,
                    command.reason(),
                    null,
                    command.sourceDecisionRef(),
                    command.decisionTimestamp());
        }
    }

    /**
     * Test safety gate that records calls and can be configured
     * to allow or reject rollback commands.
     */
    private static class RecordingSafetyGate implements RuntimeAdjustmentSafetyGate {
        private boolean allowRollback = true;
        private boolean evaluateCalled;
        private boolean recordAppliedCalled;
        private ScaleAdjustmentCommand lastEvaluatedCommand;
        private int evaluateCallCount;

        void setAllowRollback(boolean allow) {
            this.allowRollback = allow;
        }

        boolean evaluateCalled() {
            return evaluateCalled;
        }

        boolean recordAppliedCalled() {
            return recordAppliedCalled;
        }

        ScaleAdjustmentCommand lastEvaluatedCommand() {
            return lastEvaluatedCommand;
        }

        int evaluateCallCount() {
            return evaluateCallCount;
        }

        @Override
        public SafetyGateDecision evaluate(ScaleAdjustmentCommand command,
                                           ExecutorStateSnapshot currentState,
                                           ReadinessAssessment readiness) {
            evaluateCalled = true;
            evaluateCallCount++;
            lastEvaluatedCommand = command;

            if (allowRollback) {
                return SafetyGateDecision.allow(0, command);
            } else {
                return SafetyGateDecision.rejected(
                        AdjustmentFailureCode.COOLDOWN_ACTIVE,
                        "cooldown active for testing");
            }
        }

        @Override
        public void recordApplied(SafetyGateDecision decision) {
            recordAppliedCalled = true;
        }
    }
}
