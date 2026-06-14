package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;

import java.util.Objects;

/**
 * Default runtime safety gate implementation. Enforces the design
 * rules in the order specified by the spec:
 * <ol>
 *   <li>Input validation (negative sizes).</li>
 *   <li>Readiness: {@code NOT_READY} → reject.</li>
 *   <li>Readiness: {@code READY_WITH_RISK} without acceptance → reject.</li>
 *   <li>Cooldown: not elapsed → reject and decrement counter.</li>
 *   <li>Per-run limit reached → reject.</li>
 *   <li>Opposite direction (when configured) → reject.</li>
 *   <li>No-op: command target equals current state → no-op.</li>
 *   <li>Otherwise → allow.</li>
 * </ol>
 *
 * <p>The gate keeps a per-instance, single-run history. A new run
 * should use a fresh gate instance. This class is thread-safe:
 * {@code evaluate} and {@code recordApplied} are synchronized.
 */
public final class DefaultRuntimeAdjustmentSafetyGate implements RuntimeAdjustmentSafetyGate {

    private final SafetyGateConfig config;
    private int appliedAdjustmentsForRun;
    private int cooldownRemaining;
    private Direction lastAppliedDirection;
    private int lastAppliedTargetSize;

    public DefaultRuntimeAdjustmentSafetyGate() {
        this(SafetyGateConfig.defaults());
    }

    public DefaultRuntimeAdjustmentSafetyGate(SafetyGateConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.appliedAdjustmentsForRun = 0;
        this.cooldownRemaining = 0;
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

        if (cooldownRemaining > 0) {
            int remaining = cooldownRemaining;
            cooldownRemaining -= 1;
            return SafetyGateDecision.rejected(AdjustmentFailureCode.COOLDOWN_ACTIVE,
                    "cooldown window active, " + remaining + " decision interval(s) remaining");
        }

        if (appliedAdjustmentsForRun >= config.maxAdjustmentsPerRun()) {
            return SafetyGateDecision.rejected(AdjustmentFailureCode.RUN_LIMIT_EXCEEDED,
                    "per-run adjustment count " + appliedAdjustmentsForRun
                            + " reached maxAdjustmentsPerRun=" + config.maxAdjustmentsPerRun());
        }

        Direction next = directionOf(command);
        if (config.blockImmediateOppositeDirection()
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
        cooldownRemaining = config.cooldownDecisionIntervals();
        lastAppliedDirection = directionOf(command);
        lastAppliedTargetSize = command.targetPoolSize();
    }

    public synchronized int appliedAdjustmentsForRun() {
        return appliedAdjustmentsForRun;
    }

    public synchronized int cooldownRemaining() {
        return cooldownRemaining;
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
