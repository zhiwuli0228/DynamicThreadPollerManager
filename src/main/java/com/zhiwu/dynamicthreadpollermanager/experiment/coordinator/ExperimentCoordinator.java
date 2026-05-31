package com.zhiwu.dynamicthreadpollermanager.experiment.coordinator;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.AnalysisSummary;
import com.zhiwu.dynamicthreadpollermanager.experiment.model.ExperimentRun;
import com.zhiwu.dynamicthreadpollermanager.experiment.state.RunState;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal experiment coordinator that creates and tracks runs by scenario and policy identity.
 * Handles lifecycle transitions (start, stop, finalize) without sampling or mutation.
 */
public class ExperimentCoordinator {

    private final Map<String, ExperimentRun> runs = new ConcurrentHashMap<>();

    public ExperimentRun createRun(String scenarioId, String policyId) {
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        Objects.requireNonNull(policyId, "policyId must not be null");

        ExperimentRun run = new ExperimentRun(scenarioId, policyId);
        runs.put(run.runId(), run);
        return run;
    }

    public ExperimentRun startRun(String runId) {
        ExperimentRun run = getRun(runId);
        validateTransition(run, RunState.RUNNING);
        ExperimentRun updated = run.withState(RunState.RUNNING);
        runs.put(runId, updated);
        return updated;
    }

    public ExperimentRun stopRun(String runId) {
        ExperimentRun run = getRun(runId);
        validateTransition(run, RunState.STOPPED);
        ExperimentRun updated = run.withState(RunState.STOPPED);
        runs.put(runId, updated);
        return updated;
    }

    public ExperimentRun finalizeRun(String runId) {
        ExperimentRun run = getRun(runId);
        validateTransition(run, RunState.FINALIZED);
        ExperimentRun updated = run.withState(RunState.FINALIZED);
        runs.put(runId, updated);
        return updated;
    }

    public ExperimentRun getRun(String runId) {
        ExperimentRun run = runs.get(runId);
        if (run == null) {
            throw new IllegalArgumentException("Run not found: " + runId);
        }
        return run;
    }

    public AnalysisSummary generateSummary(String runId) {
        ExperimentRun run = getRun(runId);
        if (run.state() != RunState.FINALIZED) {
            throw new IllegalStateException("Run must be finalized before generating summary");
        }
        return new AnalysisSummary(
                run.runId(),
                run.experimentKey(),
                run.scenarioId(),
                run.policyId(),
                run.createdAt(),
                Instant.now(),
                "COMPLETED",
                0
        );
    }

    private void validateTransition(ExperimentRun run, RunState targetState) {
        RunState currentState = run.state();
        boolean valid = switch (targetState) {
            case RUNNING -> currentState == RunState.CREATED;
            case STOPPED -> currentState == RunState.RUNNING;
            case FINALIZED -> currentState == RunState.STOPPED;
            default -> false;
        };
        if (!valid) {
            throw new IllegalStateException(
                    "Invalid state transition from " + currentState + " to " + targetState);
        }
    }
}
