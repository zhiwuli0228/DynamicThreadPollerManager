package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Decorator implementing {@link ExecutorAdjustmentAdapter} that adds
 * rollback-aware adjustment semantics. Before delegating to the
 * wrapped adapter, it captures a pre-adjustment snapshot. After the
 * delegate completes, it samples a post-adjustment snapshot and
 * compares against configurable degradation thresholds. If
 * degradation is detected, a rollback command restoring the prior
 * core pool size is issued through the injected
 * {@link RuntimeAdjustmentSafetyGate}.
 *
 * <p>Rollback is bounded to at most 1 per original adjustment
 * decision. If the rollback itself degrades, the adapter returns the
 * rollback result without entering an infinite loop.
 *
 * <p>Rollback commands are always evaluated through the safety gate.
 * If the safety gate rejects the rollback, the original
 * (non-rolled-back) result is returned.
 *
 * <p>Rollback actions are reported via an injected
 * {@link BiConsumer} callback that receives the original result and
 * the rollback result (or {@code null} if the safety gate rejected
 * the rollback). This allows the caller (e.g., the adjustment loop)
 * to record evidence via {@code LoopEvidenceRecorder} without the
 * adapter depending on session context.
 */
public final class RollbackAwareAdjustmentAdapter implements ExecutorAdjustmentAdapter {

    private final ExecutorAdjustmentAdapter delegate;
    private final RuntimeAdjustmentSafetyGate safetyGate;
    private final DegradationConfig degradationConfig;
    private final BiConsumer<AdjustmentResult, AdjustmentResult> rollbackListener;
    private final Supplier<Instant> clock;

    /**
     * @param delegate           the wrapped adapter (must not be null)
     * @param safetyGate         gate used to evaluate rollback commands
     *                           (must not be null)
     * @param degradationConfig  thresholds for degradation detection
     *                           (must not be null)
     * @param rollbackListener   callback invoked with
     *                           (originalResult, rollbackResult) when
     *                           a rollback is attempted; may be null
     *                           if no listener is needed
     * @param clock              time source (must not be null)
     */
    public RollbackAwareAdjustmentAdapter(
            ExecutorAdjustmentAdapter delegate,
            RuntimeAdjustmentSafetyGate safetyGate,
            DegradationConfig degradationConfig,
            BiConsumer<AdjustmentResult, AdjustmentResult> rollbackListener,
            Supplier<Instant> clock) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.safetyGate = Objects.requireNonNull(safetyGate, "safetyGate must not be null");
        this.degradationConfig = Objects.requireNonNull(degradationConfig, "degradationConfig must not be null");
        this.rollbackListener = rollbackListener;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ExecutorStateSnapshot currentState() {
        return delegate.currentState();
    }

    @Override
    public AdjustmentResult apply(ScaleAdjustmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        ExecutorStateSnapshot preSnapshot = delegate.currentState();
        AdjustmentResult delegateResult = delegate.apply(command);

        if (delegateResult.status() != AdjustmentStatus.APPLIED) {
            return delegateResult;
        }

        ExecutorStateSnapshot postSnapshot = delegate.currentState();

        if (!isDegraded(preSnapshot, postSnapshot)) {
            return delegateResult;
        }

        return executeRollback(command, preSnapshot, delegateResult);
    }

    private boolean isDegraded(ExecutorStateSnapshot pre, ExecutorStateSnapshot post) {
        Integer preQueue = pre.queueSize();
        Integer postQueue = post.queueSize();
        if (preQueue != null && postQueue != null) {
            int queueIncrease = postQueue - preQueue;
            if (queueIncrease > degradationConfig.queueDepthThreshold()) {
                return true;
            }
        }
        return false;
    }

    private AdjustmentResult executeRollback(
            ScaleAdjustmentCommand originalCommand,
            ExecutorStateSnapshot preSnapshot,
            AdjustmentResult delegateResult) {

        Instant now = clock.get();
        int currentPoolSize = delegateResult.appliedPoolSize() != null
                ? delegateResult.appliedPoolSize()
                : originalCommand.currentPoolSize();

        ScaleAdjustmentCommand rollbackCommand = ScaleAdjustmentCommand.create(
                originalCommand.runId(),
                now,
                currentPoolSize,
                preSnapshot.corePoolSize(),
                "rollback: degradation detected after " + originalCommand.commandId(),
                "rollback:" + originalCommand.sourceDecisionRef(),
                clock);

        ExecutorStateSnapshot currentState = delegate.currentState();
        ReadinessAssessment readiness = new ReadinessAssessment(
                ReadinessStatus.READY,
                List.of(), List.of(), List.of(), List.of(),
                ReadinessAssessment.DEFAULT_CONFIG_LABEL,
                List.of());

        SafetyGateDecision gateDecision = safetyGate.evaluate(
                rollbackCommand, currentState, readiness);

        if (gateDecision.outcome() == SafetyGateDecision.Outcome.REJECTED) {
            if (rollbackListener != null) {
                rollbackListener.accept(delegateResult, null);
            }
            return delegateResult;
        }

        safetyGate.recordApplied(gateDecision);
        AdjustmentResult rollbackResult = delegate.apply(rollbackCommand);

        if (rollbackListener != null) {
            rollbackListener.accept(delegateResult, rollbackResult);
        }

        return rollbackResult;
    }
}
