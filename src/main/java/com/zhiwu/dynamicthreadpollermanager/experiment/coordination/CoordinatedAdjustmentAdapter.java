package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentFailureCode;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentResult;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.AdjustmentStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorAdjustmentAdapter;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ExecutorStateSnapshot;
import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.ScaleAdjustmentCommand;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Decorator implementing {@link ExecutorAdjustmentAdapter} that injects
 * {@link GroupCoordinator} coordination before delegating to the wrapped
 * adapter. No modification to AdjustmentLoop or any v0.14.0 component.
 */
public final class CoordinatedAdjustmentAdapter implements ExecutorAdjustmentAdapter {

    private final ExecutorAdjustmentAdapter delegate;
    private final GroupCoordinator coordinator;
    private final String executorName;
    private final Supplier<Instant> clock;

    public CoordinatedAdjustmentAdapter(
            ExecutorAdjustmentAdapter delegate,
            GroupCoordinator coordinator,
            String executorName,
            Supplier<Instant> clock) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator must not be null");
        this.executorName = Objects.requireNonNull(executorName, "executorName must not be null");
        if (executorName.isBlank()) {
            throw new IllegalArgumentException("executorName must not be blank");
        }
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ExecutorStateSnapshot currentState() {
        return delegate.currentState();
    }

    @Override
    public AdjustmentResult apply(ScaleAdjustmentCommand command) {
        GroupCoordinationResult result = coordinator.coordinate(command, executorName);

        if (result.isRejected()) {
            ExecutorStateSnapshot state = delegate.currentState();
            return new AdjustmentResult(
                    command,
                    AdjustmentStatus.REJECTED,
                    state,
                    command.targetPoolSize(),
                    state.corePoolSize(),
                    state,
                    result.rationale(),
                    AdjustmentFailureCode.COORDINATION_REJECTED,
                    command.sourceDecisionRef(),
                    command.decisionTimestamp());
        }

        ScaleAdjustmentCommand toApply = result.outcome() == CoordinationOutcome.CAPPED
                ? result.approvedCommand()
                : command;

        return delegate.apply(toApply);
    }
}
