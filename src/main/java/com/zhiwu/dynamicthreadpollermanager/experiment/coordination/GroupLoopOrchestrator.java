package com.zhiwu.dynamicthreadpollermanager.experiment.coordination;

import com.zhiwu.dynamicthreadpollermanager.experiment.adjustment.RuntimeAdjustmentSafetyGate;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.AtomicDeletionSafety;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessAssessment;
import com.zhiwu.dynamicthreadpollermanager.experiment.analysis.ReadinessStatus;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.ClassifierConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PolicyRanker;
import com.zhiwu.dynamicthreadpollermanager.experiment.classification.PressureClassifier;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ExecutorRegistry;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutor;
import com.zhiwu.dynamicthreadpollermanager.experiment.executor.ManagedExecutorAdjustmentAdapter;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.AdjustmentHistory;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.AdjustmentLoop;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.AntiOscillationGuard;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.DecisionOrchestrator;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.FeedbackCalibrator;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.LoopConfig;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.LoopEvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.LoopSession;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.LoopState;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.OscillationDetector;
import com.zhiwu.dynamicthreadpollermanager.experiment.loop.PressureStateMachine;
import com.zhiwu.dynamicthreadpollermanager.experiment.metrics.EvidenceRecorder;
import com.zhiwu.dynamicthreadpollermanager.experiment.policy.PolicyEvaluator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Manages the lifecycle of multiple AdjustmentLoop instances as a
 * coordinated group. Provides aggregated GroupHealth status.
 */
public final class GroupLoopOrchestrator {

    private final ExecutorGroup group;
    private final Map<String, AdjustmentLoop> loops;

    public GroupLoopOrchestrator(ExecutorGroup group) {
        this.group = Objects.requireNonNull(group, "group must not be null");
        this.loops = new ConcurrentHashMap<>();
    }

    /**
     * Components needed to construct an AdjustmentLoop for one executor.
     */
    public record LoopComponents(
            LoopConfig loopConfig,
            DecisionOrchestrator orchestrator,
            PressureClassifier classifier,
            PolicyEvaluator evaluator,
            ClassifierConfig classifierConfig,
            RuntimeAdjustmentSafetyGate safetyGate,
            AdjustmentHistory history,
            LoopEvidenceRecorder loopEvidenceRecorder,
            PressureStateMachine stateMachine,
            EvidenceRecorder evidenceRecorder,
            Supplier<Instant> clock,
            OscillationDetector oscillationDetector,
            FeedbackCalibrator calibrator,
            AntiOscillationGuard antiOscillationGuard) {
    }

    public Map<String, LoopSession> startAll(
            Map<String, LoopComponents> componentsByExecutor) {

        ExecutorRegistry registry = new ExecutorRegistry(new AtomicDeletionSafety());
        Map<String, LoopSession> sessions = new LinkedHashMap<>();
        for (Map.Entry<String, ManagedExecutor> entry : group.getMembers().entrySet()) {
            String name = entry.getKey();
            ManagedExecutor executor = entry.getValue();
            LoopComponents comps = componentsByExecutor.get(name);
            if (comps == null) {
                throw new IllegalArgumentException(
                        "No components for executor: " + name);
            }

            registry.register(name, executor);
            CoordinatedAdjustmentAdapter coordinatedAdapter =
                    new CoordinatedAdjustmentAdapter(
                            new ManagedExecutorAdjustmentAdapter(
                                    registry,
                                    comps.safetyGate(),
                                    name,
                                    new ReadinessAssessment(
                                            ReadinessStatus.READY,
                                            List.of(), List.of(), List.of(), List.of(),
                                            "runtime-loop", List.of())),
                            group.getCoordinator(),
                            name,
                            comps.clock());

            AdjustmentLoop loop = new AdjustmentLoop(
                    comps.loopConfig(),
                    comps.orchestrator(),
                    comps.classifier(),
                    comps.evaluator(),
                    comps.classifierConfig(),
                    coordinatedAdapter,
                    comps.safetyGate(),
                    comps.history(),
                    comps.loopEvidenceRecorder(),
                    comps.stateMachine(),
                    comps.evidenceRecorder(),
                    comps.clock(),
                    comps.oscillationDetector(),
                    comps.calibrator(),
                    comps.antiOscillationGuard());

            loops.put(name, loop);
            sessions.put(name, loop.start(executor));
        }
        return sessions;
    }

    public void pauseAll() {
        loops.values().forEach(loop -> {
            if (loop.getState() == LoopState.RUNNING) {
                loop.pause();
            }
        });
    }

    public void resumeAll() {
        loops.values().forEach(loop -> {
            if (loop.getState() == LoopState.PAUSED) {
                loop.resume();
            }
        });
    }

    public Map<String, LoopSession> stopAll() {
        Map<String, LoopSession> results = new LinkedHashMap<>();
        loops.forEach((id, loop) -> {
            LoopState state = loop.getState();
            if (state == LoopState.RUNNING || state == LoopState.PAUSED) {
                results.put(id, loop.stop());
            } else {
                results.put(id, loop.getCurrentSession());
            }
        });
        return results;
    }

    public void emergencyStopAll(String reason) {
        loops.values().forEach(loop -> {
            LoopState state = loop.getState();
            if (state == LoopState.RUNNING || state == LoopState.PAUSED) {
                loop.emergencyStop(reason);
            }
        });
    }

    public GroupHealth getGroupHealth() {
        int running = 0, paused = 0, stopped = 0, emergencyStopped = 0;
        Map<String, LoopState> loopStates = new LinkedHashMap<>();

        for (Map.Entry<String, ManagedExecutor> entry : group.getMembers().entrySet()) {
            String id = entry.getKey();
            AdjustmentLoop loop = loops.get(id);
            LoopState state = loop != null ? loop.getState() : null;
            if (state != null) {
                loopStates.put(id, state);
                switch (state) {
                    case RUNNING: running++; break;
                    case PAUSED: paused++; break;
                    case STOPPED: stopped++; break;
                    case EMERGENCY_STOPPED: emergencyStopped++; break;
                    default: break;
                }
            }
        }

        List<String> warnings = new ArrayList<>();
        ResourceBudget budget = group.getBudget();
        double utilization = (double) budget.totalAllocatedThreads()
                / group.getConfig().maxTotalThreads();
        if (utilization >= 0.9) {
            warnings.add(String.format(
                    "budget >= %.0f%% utilized", utilization * 100));
        }

        return new GroupHealth(
                group.getConfig().groupId(),
                group.size(), running, paused, stopped, emergencyStopped,
                loopStates,
                budget.getThreadAllocations(),
                budget.availableThreads(),
                warnings);
    }

    public ExecutorGroup getGroup() {
        return group;
    }
}
