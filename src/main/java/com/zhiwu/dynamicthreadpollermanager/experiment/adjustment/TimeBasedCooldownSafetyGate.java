package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Time-based safety gate implementing {@link RuntimeAdjustmentSafetyGate}.
 * Uses injectable {@link Supplier}{@code <Instant>} for wall-clock cooldown
 * instead of decision-interval counters. Emergency rollback commands bypass
 * cooldown via {@link ScaleAdjustmentCommand#isEmergencyRollback()}.
 *
 * <p>Thread-safe: {@code evaluate} and {@code recordApplied} are synchronized.
 */
public final class TimeBasedCooldownSafetyGate implements RuntimeAdjustmentSafetyGate {

    private final SafetyGateConfig config;
    private final Duration cooldownDuration;
    private final Supplier<Instant> clock;
    private final Map<String, Instant> lastAppliedInstant;
    private int appliedAdjustmentsForRun;
    private Direction lastAppliedDirection;
    private int lastAppliedTargetSize;

    public TimeBasedCooldownSafetyGate(SafetyGateConfig config,
                                       Duration cooldownDuration,
                                       Supplier<Instant> clock) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.cooldownDuration = Objects.requireNonNull(cooldownDuration, "cooldownDuration must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        if (cooldownDuration.isNegative()) {
            throw new IllegalArgumentException("cooldownDuration must not be negative");
        }
        this.lastAppliedInstant = new HashMap<>();
        this.appliedAdjustmentsForRun = 0;
        this.lastAppliedDirection = null;
        this.lastAppliedTargetSize = -1;
    }

    @Override
    public synchronized SafetyGateDecision evaluate(ScaleAdjustmentCommand command,
                                                     ExecutorStateSnapshot currentState,
                                                     ReadinessAssessment readiness) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(currentState, "currentState must not be null");
        Objects.requireNonNull(readiness, "readiness must not be null");

        if (command.targetPoolSize() < 1) {
            return SafetyGateDecision.rejected(AdjustmentFailureCode.INVALID_COMMAND,
                    "command targetPoolSize must be >= 1, was " + command.targetPoolSize());
        }
        if (command.currentPoolSize() < 0) {
            return SafetyGateDecision.rejected(AdjustmentFailureCode.INVALID_COMMAND,
                    "command currentPoolSize must be >= 0, was " + command.currentPoolSize());
        }

        if (readiness.status() == ReadinessStatus.NOT_READY) {
            return SafetyGateDecision.rejected(AdjustmentFailureCode.NOT_READY,
                    "readiness status is NOT_READY");
        }
        if (readiness.status() == ReadinessStatus.READY_WITH_RISK && !config.allowReadyWithRisk()) {
            return SafetyGateDecision.rejected(AdjustmentFailureCode.RISK_NOT_ACCEPTED,
                    "readiness status is READY_WITH_RISK and risk profile is not accepted");
        }

        if (!command.isEmergencyRollback()) {
            Instant lastApplied = lastAppliedInstant.get(command.runId());
            if (lastApplied != null) {
                Duration elapsed = Duration.between(lastApplied, clock.get());
                if (elapsed.compareTo(cooldownDuration) < 0) {
                    return SafetyGateDecision.rejected(AdjustmentFailureCode.COOLDOWN_ACTIVE,
                            "cooldown window active, " + elapsed.toMillis() + "ms elapsed, "
                                    + cooldownDuration.toMillis() + "ms required");
                }
            }
        }

        if (appliedAdjustmentsForRun >= config.maxAdjustmentsPerRun()) {
            return SafetyGateDecision.rejected(AdjustmentFailureCode.RUN_LIMIT_EXCEEDED,
                    "per-run adjustment count " + appliedAdjustmentsForRun
                            + " reached maxAdjustmentsPerRun=" + config.maxAdjustmentsPerRun());
        }

        Direction next = directionOf(command);
        if (!command.isEmergencyRollback()
                && config.blockImmediateOppositeDirection()
                && lastAppliedDirection != null
                && lastAppliedDirection != next
                && lastAppliedTargetSize == command.currentPoolSize()) {
            return SafetyGateDecision.rejected(AdjustmentFailureCode.OPPOSITE_DIRECTION,
                    "command direction " + next + " reverses last applied direction "
                            + lastAppliedDirection);
        }

        if (command.isNoOp()) {
            return SafetyGateDecision.noOp("command currentPoolSize equals targetPoolSize");
        }
        if (command.targetPoolSize() == currentState.corePoolSize()) {
            return SafetyGateDecision.noOp("command target equals current pool size");
        }

        return SafetyGateDecision.allow(appliedAdjustmentsForRun, command);
    }

    @Override
    public synchronized void recordApplied(SafetyGateDecision decision) {
        Objects.requireNonNull(decision, "decision must not be null");
        if (decision.outcome() != SafetyGateDecision.Outcome.ALLOW) {
            return;
        }
        ScaleAdjustmentCommand command = decision.appliedCommand();
        if (command == null) {
            return;
        }
        appliedAdjustmentsForRun += 1;
        lastAppliedInstant.put(command.runId(), clock.get());
        lastAppliedDirection = directionOf(command);
        lastAppliedTargetSize = command.targetPoolSize();
    }

    public synchronized int appliedAdjustmentsForRun() {
        return appliedAdjustmentsForRun;
    }

    public synchronized Instant lastAppliedInstant(String runId) {
        return lastAppliedInstant.get(runId);
    }

    public Duration cooldownDuration() {
        return cooldownDuration;
    }

    public SafetyGateConfig config() {
        return config;
    }

    private static Direction directionOf(ScaleAdjustmentCommand command) {
        if (command.targetPoolSize() > command.currentPoolSize()) {
            return Direction.UP;
        }
        if (command.targetPoolSize() < command.currentPoolSize()) {
            return Direction.DOWN;
        }
        return Direction.NEUTRAL;
    }

    private enum Direction { UP, DOWN, NEUTRAL }
}
