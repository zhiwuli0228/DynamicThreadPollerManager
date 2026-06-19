# v0.15.0 Objectives and Scope

## Header

- Version name: `v0.15.0`
- Status: `VERSION_DESIGN_DRAFT`
- Current phase: `VERSION_BASELINE`
- Requirement theme: multi-executor group coordination, resource budgeting, adjustment priority, closed-loop cross-validation

## 1. Background

### 1.1 Capability baseline

| Version | Capability | Status |
|---|---|---|
| v0.1.0 | Experiment foundation (ExperimentRun, LoadScenario, ControlPolicy) | IMPLEMENTED |
| v0.2.0 | Metrics collection and recording (ObservedSnapshot, EvidenceRecorder) | IMPLEMENTED |
| v0.3.0 | Scenario runners (ScenarioPlanner, BaselineWorkloadExecutor) | IMPLEMENTED |
| v0.4.0 | Adaptive policy and control gate (ControlGate, ThresholdPolicyEvaluator) | IMPLEMENTED |
| v0.5.0 | Offline replay and readiness (OfflinePolicyReplay, MutationReadinessGate) | IMPLEMENTED |
| v0.6.0 | Data acquisition baseline (RunManifest, DataQualityValidator, ReportWriter) | IMPLEMENTED |
| v0.7.0 | ManagedExecutor domain and closed-loop experiment (ManagedExecutor, ExecutorRegistry, AdjustmentAdapter, ScaleAdjustmentCommand) | IMPLEMENTED |
| v0.8.0 | Real executor data acquisition and G7-G9 quality gates | IMPLEMENTED |
| v0.9.0 | Queue capacity dynamic adjustment (QueueResizeCommand, ExecutorRebuildStrategy) | IMPLEMENTED |
| v0.10.0 | Rejection policy dynamic replacement (RejectionPolicyCommand) | IMPLEMENTED |
| v0.11.0 | Persistent evidence recording and autonomous sampling (FileBackedEvidenceRecorder, LivePressureSampler) | IMPLEMENTED |
| v0.12.0 | Baseline comparison experiment framework (BaselineExecutorCatalog, ComparableScenarioRunner, NormalizedComparisonMetrics, ComparisonReportArtifact) | ARCHIVED |
| v0.13.0 | Pressure classification and policy scoring (PressureClassifier, PolicyScorer, PolicyRanker, SystemCpuProbe) | IMPLEMENTED |
| v0.14.0 | Adaptive closed-loop adjustment (AdjustmentLoop, DecisionOrchestrator, PressureStateMachine, OscillationDetector, FeedbackCalibrator) | ARCHIVED |

### 1.2 Current gaps

v0.14.0 delivered autonomous closed-loop adjustment for a **single ManagedExecutor**. The system can now diagnose pressure, select policies, apply adjustments, and guard against oscillation — all without human intervention. Two structural gaps remain:

1. **No multi-executor coordination** — Real applications have multiple thread pools (request-processing, background-tasks, I/O-operations) running concurrently. Each pool can run its own closed-loop, but their adjustments are independent and uncoordinated. When resources are shared (total threads, memory), independent loops can make conflicting decisions — e.g., two loops both scale up, exceeding a shared resource budget. Missing components: `ExecutorGroup` (group multiple executors under shared coordination), `GroupCoordinator` (serialize and resolve adjustment conflicts), `ResourceBudget` (enforce total resource limits), and `AdjustmentPriority` (decide which executor gets resources first when constrained). This gap was explicitly deferred as v0.14.0 DFR-01.

2. **No empirical validation of closed-loop effectiveness** — We can run closed-loop, but we have not proven it outperforms static policies or no-adjustment baselines. v0.12.0 delivered comparison infrastructure (`ComparableScenarioRunner`, `NormalizedComparisonMetrics`, `ComparisonReportArtifact`) and v0.13.0's diagnostic data showed that the system can correctly classify pressure states and score policies — but no version has run a controlled experiment comparing closed-loop outcomes against alternatives. Missing components: `ClosedLoopValidationRunner` (orchestrate 3-way comparison runs), `ValidationScenario` (define comparison setup), `ValidationComparisonReport` (side-by-side results with statistical significance). This gap was explicitly deferred as v0.14.0 DFR-04.

### 1.3 JDK API feasibility assessment

v0.15.0 does not introduce new `ThreadPoolExecutor` property changes:

| Question | Answer |
|---|---|
| Does this version introduce new TPE property changes? | No — coordination is an orchestration layer above individual AdjustmentLoop instances. Validation reuses v0.12.0 comparison infrastructure and v0.14.0 closed-loop. |
| Does coordination require new external dependencies? | No — all data sources (ExecutorRegistry, AdjustmentLoop, LivePressureSampler) exist in prior versions. Validation reuses ComparableScenarioRunner from v0.12.0. |
| Does coordination modify executor behavior? | No — GroupCoordinator intercepts and potentially modifies adjustment decisions before they reach AdjustmentAdapter, but does not alter how executors execute tasks. |

### 1.4 Relationship to existing infrastructure

Multi-executor coordination and validation are orchestration layers built on existing capabilities:

| Existing component | Role in v0.15.0 |
|---|---|
| `AdjustmentLoop` (v0.14.0) | Each executor's individual closed-loop controller. GroupCoordinator intercepts decisions between loop output and adapter input |
| `DecisionOrchestrator` (v0.14.0) | Each executor's decision pipeline. GroupCoordinator receives its AdjustmentDecision output |
| `AdjustmentAdapter` (v0.7.0-v0.10.0) | Execution layer. GroupCoordinator routes approved/modified decisions to adapter |
| `RuntimeAdjustmentSafetyGate` (v0.7.0) | Per-executor safety validation. Still runs before group coordination |
| `AdjustmentHistory` (v0.14.0) | Per-executor history. GroupCoordinator also maintains group-level coordination history |
| `OscillationDetector` (v0.14.0) | Per-executor oscillation detection. GroupCoordinator adds cross-executor oscillation detection (e.g., exec-A scales up while exec-B scales down in lockstep) |
| `ExecutorRegistry` (v0.7.0) | Source of truth for which executors belong to a group |
| `ComparableScenarioRunner` (v0.12.0) | Executes identical workload for cross-validation |
| `NormalizedComparisonMetrics` (v0.12.0) | Normalization layer for fair comparison across modes |
| `ComparisonReportArtifact` (v0.12.0) | Report template extended with closed-loop-specific metrics |
| `FeedbackCalibrator` (v0.14.0) | Per-executor weight calibration. Validation runs provide ground-truth data for calibration |

Key principle: **GroupCoordinator is a coordination interceptor, not a replacement for individual AdjustmentLoops**. Each executor runs its own loop independently; the coordinator serializes, validates, and potentially modifies adjustments that would conflict at the group level.

### 1.5 Why now

- v0.14.0 DFR-01 (multi-executor coordination) and DFR-04 (cross-validation) were explicitly deferred to v0.15.0
- Single-executor closed-loop is validated and stable — 857 tests, 0 failures, no known defects
- Multi-executor is the natural increment: from "one pool adjusts itself" to "multiple pools coordinate"
- Cross-validation answers the fundamental question: "does closed-loop actually improve outcomes?"
- v0.12.0 comparison infrastructure has been idle since its delivery — v0.15.0 is its intended consumer
- Without coordination, deploying multiple AdjustmentLoops in the same JVM would be unsafe (uncoordinated resource consumption)
- Without validation, the closed-loop feature lacks empirical justification

## 2. Objectives

`v0.15.0` focuses on the following objectives:

1. **ExecutorGroup**: Define a named group of ManagedExecutors that share a resource budget and are coordinated together
2. **GroupCoordinator**: Intercept adjustment decisions from individual AdjustmentLoops, serialize them, resolve conflicts against the shared resource budget, and apply priority-based allocation
3. **ResourceBudget**: Define total resource limits for the group (max total threads, max total queue capacity) and track current allocation per executor
4. **AdjustmentPriority**: Assign priority levels (CRITICAL/HIGH/NORMAL/LOW) to executors; higher-priority executors get resources first under contention
5. **GroupLoopOrchestrator**: Orchestrate the lifecycle of multiple AdjustmentLoops (start all, pause all, stop all, emergency-stop all) with coordinated sequencing
6. **ClosedLoopValidationRunner**: Run the same workload through three modes (closed-loop, best-static-policy, no-adjustment baseline) using v0.12.0's ComparableScenarioRunner
7. **ValidationComparisonReport**: Produce side-by-side comparison with statistical significance assessment of throughput, latency, rejection rate, and stability
8. **End-to-end multi-executor scenario**: A 2-executor group handles concurrent workloads, coordinator resolves at least 1 resource conflict, both executors converge to stable states

## 3. In Scope

- `ExecutorGroup` class (group identity, member executors, shared budget, coordinator reference)
- `ExecutorGroupConfig` record (groupId, maxTotalThreads, maxTotalQueueCapacity, coordinationMode)
- `GroupCoordinator` class (intercepts AdjustmentDecision, resolves conflicts, enforces budget, returns GroupCoordinationResult)
- `GroupCoordinationResult` record (original decision, approved decision, modification rationale, conflict details)
- `ResourceBudget` class (total limits, current allocation per executor, available headroom query)
- `AdjustmentPriority` enum (CRITICAL, HIGH, NORMAL, LOW)
- `GroupLoopOrchestrator` class (manages lifecycle of multiple AdjustmentLoops, group-level health monitoring)
- `GroupCoordinationEntry` record (timestamp, executor, originalDecision, result, conflictDescription)
- `GroupCoordinationHistory` class (group-level coordination record)
- `CrossExecutorOscillationDetector` class (detects oscillation patterns across executors, e.g., A scales up while B scales down)
- `ClosedLoopValidationRunner` class (orchestrates 3-way comparison: closed-loop vs static-policy vs baseline)
- `ValidationScenario` record (workload definition, executor config, candidate policies, duration)
- `ValidationComparisonReport` class (side-by-side metrics comparison, statistical significance)
- `StatisticalSignificance` record (metric, p-value, confidence interval, effect size, isSignificant)
- `experiment.coordination` new package
- `experiment.validation` new package
- Existing 857 tests zero regression

## 4. Out of Scope

- Cross-JVM / distributed coordination (single JVM only — this is a v0.16.0+ candidate)
- Dynamic priority rebalancing based on real-time workload analysis
- Automatic executor group formation or discovery (groups are explicitly configured)
- Group-level feedback weight calibration (per-executor calibration by v0.14.0 is sufficient)
- Multi-dimensional simultaneous adjustment per executor (still v0.14.0 DFR-06 for v0.16.0+)
- Policy auto-generation or parameter auto-tuning (still v0.14.0 DFR-03 for v0.16.0+)
- Closed-loop performance benchmarks (still v0.14.0 DFR-02 for v0.16.0+)
- Real-time monitoring dashboard or external system integration
- CLI entry
- New executor mutation or dynamic configuration dimensions
- Modification to AdjustmentLoop, DecisionOrchestrator, or other v0.14.0 components (coordination is an interceptor layer, not a modification)

## 5. Architecture Alignment

| Architecture document | How this version addresses it |
|---|---|
| `operational-and-evolution-boundaries.md` | No new external dependencies, no cross-JVM coordination. Coordination is in-process using existing ExecutorRegistry and AdjustmentAdapter interfaces. Validation reuses v0.12.0 comparison infrastructure. |
| `managed-executor-domain-model.md` | Extends the orchestration layer with group-level coordination. ManagedExecutor itself is unchanged. Follows "every mutable operation must be explicit and traceable" — all coordination decisions are recorded in GroupCoordinationHistory. |
| `observability-and-experiment-strategy.md` | Closes the loop on "observe behavior before optimizing" — cross-validation provides empirical evidence that closed-loop optimization actually works. Validation reports are versioned evidence artifacts. |
| `scheduling-reconfiguration-and-recovery-model.md` | GroupCoordinator adds group-level safety: resource budget caps, conflict detection, cross-executor oscillation detection. Group-level emergency stop when coordination detects unresolvable resource contention. |

## 6. Module Boundaries

| Module | Change type | Description |
|---|---|---|
| `experiment.coordination` | **New** `ExecutorGroup` | Group identity and member management |
| `experiment.coordination` | **New** `ExecutorGroupConfig` | Group-level configuration record |
| `experiment.coordination` | **New** `GroupCoordinator` | Central coordination interceptor |
| `experiment.coordination` | **New** `GroupCoordinationResult` | Coordination decision record |
| `experiment.coordination` | **New** `ResourceBudget` | Shared resource budget tracking |
| `experiment.coordination` | **New** `AdjustmentPriority` | Priority enum for resource allocation |
| `experiment.coordination` | **New** `GroupLoopOrchestrator` | Multi-loop lifecycle manager |
| `experiment.coordination` | **New** `GroupCoordinationEntry` | Coordination history record |
| `experiment.coordination` | **New** `GroupCoordinationHistory` | Group-level coordination history |
| `experiment.coordination` | **New** `CrossExecutorOscillationDetector` | Cross-executor oscillation detection |
| `experiment.validation` | **New** `ClosedLoopValidationRunner` | 3-way comparison orchestrator |
| `experiment.validation` | **New** `ValidationScenario` | Validation setup record |
| `experiment.validation` | **New** `ValidationComparisonReport` | Side-by-side comparison report |
| `experiment.validation` | **New** `StatisticalSignificance` | Statistical test result record |
| `experiment.loop` | Unchanged | AdjustmentLoop, DecisionOrchestrator unchanged |
| `experiment.classification` | Unchanged | Classifier, scorer, ranker unchanged |
| `experiment.policy` | Unchanged | PolicyEvaluator unchanged |
| `experiment.adjustment` | Unchanged | AdjustmentAdapter, SafetyGate unchanged |
| `experiment.executor` | Unchanged | ManagedExecutor unchanged |
| `experiment.comparison` | Unchanged | ComparableScenarioRunner reused |

### Dependency direction

```text
experiment.coordination (ExecutorGroup, GroupCoordinator, ResourceBudget,
                         AdjustmentPriority, GroupLoopOrchestrator,
                         GroupCoordinationEntry, GroupCoordinationHistory,
                         CrossExecutorOscillationDetector, ExecutorGroupConfig,
                         GroupCoordinationResult)
    ├── experiment.loop (AdjustmentLoop, AdjustmentDecision, DecisionOrchestrator — intercept)
    ├── experiment.adjustment (AdjustmentAdapter, SafetyGate — route approved decisions)
    ├── experiment.executor (ManagedExecutor, ExecutorRegistry — read)
    └── experiment.metrics (RuntimeObservation, PressureSnapshot — read)

experiment.validation (ClosedLoopValidationRunner, ValidationScenario,
                       ValidationComparisonReport, StatisticalSignificance)
    ├── experiment.comparison (ComparableScenarioRunner, NormalizedComparisonMetrics,
    │                          ComparisonReportArtifact — reuse)
    ├── experiment.loop (AdjustmentLoop, LoopConfig — run closed-loop mode)
    ├── experiment.policy (ThresholdPolicyConfig — run static-policy mode)
    └── experiment.coordination (ExecutorGroup, GroupCoordinator — optional group validation)
```

## 7. Core Technical Design

### 7.1 ExecutorGroup and Group Configuration

```java
public final class ExecutorGroup {
    private final ExecutorGroupConfig config;
    private final List<ManagedExecutor> members;
    private final ResourceBudget budget;
    private final GroupCoordinator coordinator;
    private final GroupCoordinationHistory history;

    public ExecutorGroup(ExecutorGroupConfig config, List<ManagedExecutor> members);
    public GroupCoordinationResult submitAdjustment(AdjustmentDecision decision, ManagedExecutor source);
    public ResourceBudget getBudget();
    public List<ManagedExecutor> getMembers();
    public GroupCoordinationHistory getHistory();
}

public record ExecutorGroupConfig(
    String groupId,
    int maxTotalThreads,            // Sum of all executor maxPoolSizes cannot exceed this
    int maxTotalQueueCapacity,      // Sum of all executor queue capacities cannot exceed this (if queue-bound)
    AdjustmentPriority defaultPriority, // Default priority for members without explicit priority
    Map<String, AdjustmentPriority> memberPriorities, // executorId → priority
    long coordinationTimeoutMs,     // Max wait for coordination decision, default 5000
    boolean failOpen                // If true, uncoordinated adjustment allowed when coordinator unavailable
) {
    public static ExecutorGroupConfig defaults(String groupId, int maxTotalThreads) { ... }
}
```

### 7.2 GroupCoordinator — Coordination Interceptor

The `GroupCoordinator` sits between individual `AdjustmentLoop` instances and their `AdjustmentAdapter`. Each loop independently produces `AdjustmentDecision`; before execution, the decision passes through the coordinator, which evaluates it against the shared `ResourceBudget`.

```java
public final class GroupCoordinator {
    private final ExecutorGroupConfig config;
    private final ResourceBudget budget;
    private final GroupCoordinationHistory history;
    private final CrossExecutorOscillationDetector crossOscillationDetector;

    /**
     * Evaluate a pending adjustment against the group resource budget.
     * Called by AdjustmentLoop before applying a decision.
     *
     * @param decision  the pending adjustment decision from an executor's loop
     * @param source    the executor proposing the adjustment
     * @return coordination result (approved, modified, or rejected)
     */
    public GroupCoordinationResult coordinate(AdjustmentDecision decision, ManagedExecutor source);

    /** Release resources held by an executor (e.g., when it scales down). */
    public void release(ManagedExecutor executor);
}
```

**Coordination algorithm** (`coordinate()`):

```
1. If decision.isNoOp() → return APPROVED_AS_IS (no resource impact)
2. Determine the resource delta requested by this decision:
   a. Calculate targetPoolSize from decision.toCommand()
   b. delta = targetPoolSize - source.getCurrentPoolSize()
3. If delta <= 0 (scale-down or no change) → release |delta| to budget, return APPROVED_AS_IS
4. If delta > 0 (scale-up):
   a. Check budget.availableThreads() >= delta
   b. If sufficient → reserve delta in budget, return APPROVED_AS_IS
   c. If insufficient:
      i. Check if any lower-priority executor holds budget that can be preempted
      ii. If preemption possible → mark lower-priority for scale-down, allocate to requestor
      iii. If preemption not possible → return REJECTED (budget exhausted) or CAPPED (partial allocation)
5. Run cross-executor oscillation check:
   a. If source scales up and another executor recently scaled down by similar amount
      → potential cross-executor oscillation → flag in result
6. Record coordination entry in history
```

```java
public record GroupCoordinationResult(
    AdjustmentDecision originalDecision,      // The decision submitted by the loop
    AdjustmentDecision approvedDecision,      // The decision after coordination (may differ)
    CoordinationOutcome outcome,              // APPROVED_AS_IS, MODIFIED, REJECTED, CAPPED
    String rationale,                         // Human-readable explanation
    List<String> conflicts,                   // Conflicting executor IDs and their pending decisions
    boolean crossOscillationDetected,         // Cross-executor oscillation flag
    Instant coordinatedAt
) {
    public boolean isApproved() {
        return outcome == CoordinationOutcome.APPROVED_AS_IS
            || outcome == CoordinationOutcome.MODIFIED;
    }

    public enum CoordinationOutcome {
        APPROVED_AS_IS,    // No conflict, decision applied unchanged
        MODIFIED,          // Decision was adjusted (e.g., capped pool size)
        REJECTED,          // Cannot allocate resources, decision blocked
        CAPPED             // Partial allocation — granted less than requested
    }
}
```

### 7.3 ResourceBudget

```java
public final class ResourceBudget {
    private final int maxTotalThreads;
    private final int maxTotalQueueCapacity;

    // Current allocation per executor (executorId → allocatedPoolSize)
    private final Map<String, Integer> threadAllocations;
    private final Map<String, Integer> queueAllocations;

    public ResourceBudget(int maxTotalThreads, int maxTotalQueueCapacity);

    public int availableThreads();
    public int availableQueueCapacity();
    public int allocatedThreads(String executorId);
    public boolean canAllocate(String executorId, int threadDelta);
    public void reserve(String executorId, int threadDelta);
    public void release(String executorId, int threadDelta);
    public Map<String, Integer> getThreadAllocations();  // Read-only snapshot
}
```

**Budget invariants**:
- `sum(threadAllocations.values()) <= maxTotalThreads` — always true after any operation
- `threadAllocations.get(id) >= 0` — individual allocation never negative
- `availableThreads() = maxTotalThreads - sum(threadAllocations.values())`
- `reserve()` and `release()` are atomic with respect to each other (synchronized)

### 7.4 AdjustmentPriority

```java
public enum AdjustmentPriority {
    CRITICAL(4),  // Cannot be preempted; always gets resources first
    HIGH(3),      // Can only be preempted by CRITICAL
    NORMAL(2),    // Default; can be preempted by CRITICAL/HIGH
    LOW(1);       // Can be preempted by any higher priority

    private final int level;

    public boolean canPreempt(AdjustmentPriority other) {
        return this.level > other.level;
    }
}
```

**Priority-based allocation**: When `availableThreads() < requestedDelta`, the coordinator scans lower-priority executors to find preemptible allocation. Preemption means asking a lower-priority executor to scale down. Preemption is communicated back to the affected executor's `AdjustmentLoop` via a `PREEMPT` signal, which triggers an immediate adjustment cycle (skip samplingInterval wait).

**Preemption rules**:
- CRITICAL: never preempted
- HIGH: preempted only by CRITICAL
- NORMAL: preempted by CRITICAL or HIGH
- LOW: preempted by any higher priority
- Preemption amount: minimum needed to satisfy the higher-priority request
- Preempted executor receives `PREEMPT` signal with target reduction

### 7.5 GroupLoopOrchestrator

```java
public final class GroupLoopOrchestrator {
    private final ExecutorGroup group;
    private final Map<String, AdjustmentLoop> loops;  // executorId → loop

    public GroupLoopOrchestrator(ExecutorGroup group);

    /** Start all loops in the group with coordinated sequencing. */
    public Map<String, LoopSession> startAll(
        Map<String, List<ThresholdPolicyConfig>> policiesByExecutor
    );

    /** Start a single executor's loop within the group. */
    public LoopSession startLoop(String executorId, List<ThresholdPolicyConfig> policies);

    /** Pause all loops. */
    public void pauseAll();

    /** Resume all paused loops. */
    public void resumeAll();

    /** Stop all loops gracefully. */
    public Map<String, LoopSession> stopAll();

    /** Emergency stop all loops (e.g., cross-executor oscillation detected). */
    public void emergencyStopAll(String reason);

    /** Get group-level health status. */
    public GroupHealth getGroupHealth();
}
```

**GroupHealth**: Aggregated health status across all executors in the group:
```java
public record GroupHealth(
    int totalExecutors,
    int runningLoops,
    int pausedLoops,
    int stoppedLoops,
    int emergencyStoppedLoops,
    Map<String, PressureState> currentPressureStates,
    ResourceBudget budget,
    List<String> activeWarnings  // e.g., "budget 90% utilized", "cross-executor oscillation suspected"
) {}
```

### 7.6 Cross-Executor Oscillation Detection

Extension of v0.14.0's per-executor `OscillationDetector` to detect oscillation patterns **across** executors:

```java
public final class CrossExecutorOscillationDetector {
    private final int windowSize;

    /**
     * Detect if a pending adjustment on sourceExecutor would create a
     * cross-executor oscillation pattern with other executors in the group.
     */
    public boolean wouldCrossOscillate(
        AdjustmentDecision pending,
        ManagedExecutor source,
        GroupCoordinationHistory history
    );

    public Optional<String> detectedPattern(GroupCoordinationHistory history);
}
```

Detected cross-executor patterns:

1. **Lockstep counter-adjustment**: Exec-A scales up while Exec-B scales down, repeatedly. Detection: check if source's poolSize delta direction is opposite to another executor's last delta within the window.

2. **Resource ping-pong**: Exec-A claims budget → Exec-B gets preempted → Exec-B later claims budget → Exec-A gets preempted → repeat. Detection: check if the same resource units are being claimed and released between the same pair of executors within the window.

3. **Priority thrashing**: A high-priority executor repeatedly preempts a lower-priority one, causing the lower-priority executor to oscillate between scaled-down and recovery. Detection: check if preemption events on the same target occur more than `patternThreshold` times in the window.

### 7.7 GroupCoordinationHistory

```java
public final class GroupCoordinationHistory {
    private final List<GroupCoordinationEntry> entries; // Thread-safe (CopyOnWriteArrayList)

    public void record(GroupCoordinationEntry entry);
    public List<GroupCoordinationEntry> recent(int count);
    public List<GroupCoordinationEntry> byExecutor(String executorId);
    public int totalCoordinationCount();
    public int rejectedCount();
    public int modifiedCount();
    public int preemptionCount();
}

public record GroupCoordinationEntry(
    String executorId,
    AdjustmentDecision originalDecision,
    GroupCoordinationResult result,
    ResourceBudget budgetBefore,
    ResourceBudget budgetAfter,
    Instant timestamp
) {}
```

### 7.8 Integration with AdjustmentLoop

The `AdjustmentLoop` main loop is modified to include a coordination step between SafetyGate evaluation and adjustment application. **However**, to maintain zero code modification to v0.14.0 components, this integration uses a **decorator pattern** — `CoordinatedAdjustmentAdapter` wraps the existing `ExecutorAdjustmentAdapter`:

```java
public final class CoordinatedAdjustmentAdapter implements ExecutorAdjustmentAdapter {
    private final ExecutorAdjustmentAdapter delegate;
    private final GroupCoordinator coordinator;
    private final ManagedExecutor executor;

    @Override
    public AdjustmentResult apply(ScaleAdjustmentCommand command) {
        // Coordinator intercepts before delegation
        AdjustmentDecision decision = /* reconstruct from command */;
        GroupCoordinationResult result = coordinator.coordinate(decision, executor);
        if (!result.isApproved()) {
            return AdjustmentResult.rejected(command, result.rationale());
        }
        ScaleAdjustmentCommand approvedCommand = result.approvedDecision().toCommand(...);
        AdjustmentResult applied = delegate.apply(approvedCommand);
        if (command.targetPoolSize() < command.currentPoolSize()) {
            coordinator.release(executor); // Release budget on scale-down
        }
        return applied;
    }
}
```

This wrapper-based approach means **AdjustmentLoop does not need to be modified**. The `CoordinatedAdjustmentAdapter` is injected in place of the raw adapter when the executor belongs to a group.

### 7.9 Closed-Loop Validation

The validation subsystem answers: "Does closed-loop adjustment produce measurably better outcomes than the best static policy and no adjustment?"

```java
public final class ClosedLoopValidationRunner {
    private final ComparableScenarioRunner scenarioRunner; // from v0.12.0

    /**
     * Run the same workload through three modes and produce a comparison report.
     */
    public ValidationComparisonReport validate(ValidationScenario scenario);
}
```

**ValidationScenario**:
```java
public record ValidationScenario(
    String scenarioId,
    LoadScenario workload,                    // Identical workload for all three modes
    ManagedExecutorConfig executorConfig,     // Starting executor configuration
    List<ThresholdPolicyConfig> candidatePolicies, // For closed-loop mode
    ThresholdPolicyConfig bestStaticPolicy,        // For static-policy mode (pre-selected)
    Duration duration,                        // How long each mode runs
    int minIterations,                        // Minimum loop iterations for closed-loop mode
    int warmupPeriodMs                        // Warmup period before measurements start
) {}
```

**3-Mode Execution**:

```
Mode A — Closed-Loop:
  1. Create ManagedExecutor with executorConfig
  2. Create AdjustmentLoop with candidatePolicies
  3. Start LivePressureSampler
  4. Run workload via ComparableScenarioRunner
  5. Start AdjustmentLoop (autonomous adjustment)
  6. Run for 'duration'
  7. Stop loop, stop sampler, collect metrics

Mode B — Best Static Policy:
  1. Create ManagedExecutor with executorConfig
  2. Apply bestStaticPolicy at start (no further adjustments)
  3. Start LivePressureSampler
  4. Run workload via ComparableScenarioRunner
  5. Run for 'duration'
  6. Stop sampler, collect metrics

Mode C — Baseline (No Adjustment):
  1. Create ManagedExecutor with executorConfig
  2. No policy applied, no adjustments
  3. Start LivePressureSampler
  4. Run workload via ComparableScenarioRunner
  5. Run for 'duration'
  6. Stop sampler, collect metrics
```

**ValidationComparisonReport**:
```java
public record ValidationComparisonReport(
    String reportId,
    ValidationScenario scenario,
    ValidationRunResult closedLoopResult,
    ValidationRunResult staticPolicyResult,
    ValidationRunResult baselineResult,
    List<MetricComparison> comparisons,
    List<StatisticalSignificance> significanceTests,
    String overallConclusion,
    Instant generatedAt
) {}

public record MetricComparison(
    String metricName,              // e.g., "throughput", "meanLatency", "rejectionRate"
    double closedLoopValue,
    double staticPolicyValue,
    double baselineValue,
    double closedLoopVsStaticDelta,  // positive = closed-loop better
    double closedLoopVsBaselineDelta,
    double staticVsBaselineDelta
) {}

public record StatisticalSignificance(
    String metricName,
    double pValue,                  // From paired t-test or Mann-Whitney U
    double confidenceIntervalLow,   // 95% CI lower bound
    double confidenceIntervalHigh,  // 95% CI upper bound
    double effectSize,              // Cohen's d
    boolean isSignificant           // pValue < 0.05
) {}
```

**Metrics compared**:
- Throughput (tasks completed / second)
- Mean task latency (ms)
- P99 task latency (ms)
- Rejection rate (rejections / total tasks)
- Stability score (inverse of pool size variance over time)
- CPU utilization efficiency (throughput per CPU %)
- Queue saturation (average queue depth / capacity)

### 7.10 Statistical Significance Approach

Given the project constraint of no external dependencies, statistical tests are implemented in-project:

1. **Paired t-test** for throughput/latency (continuous metrics): compare the time series of metric snapshots across modes
2. **Cohen's d** for effect size: (mean_A - mean_B) / pooled_stddev
3. **Confidence intervals**: 95% CI using t-distribution with n-1 degrees of freedom
4. **Significance threshold**: p < 0.05 (standard)
5. **Minimum sample size**: 30 metric snapshots per mode (configurable via ValidationScenario.duration)

```java
public final class StatisticalSignificanceCalculator {
    public static StatisticalSignificance compare(
        double[] modeAValues,
        double[] modeBValues,
        String metricName
    );
}
```

## 8. Success Criteria (Draft)

- `ExecutorGroup` correctly manages membership and provides budget to coordinator
- `GroupCoordinator.coordinate()` correctly approves, caps, or rejects decisions based on budget availability
- `ResourceBudget` invariants hold under concurrent allocation requests (thread-safe)
- `AdjustmentPriority` preemption: CRITICAL executor can preempt LOW executor's allocation
- `CrossExecutorOscillationDetector` detects lockstep counter-adjustment (A↑B↓ repeated)
- `GroupLoopOrchestrator` correctly starts, pauses, resumes, stops all loops in a group
- `GroupCoordinationHistory` records every coordination decision with before/after budget snapshots
- `ClosedLoopValidationRunner` successfully executes all three modes
- `ValidationComparisonReport` shows closed-loop outperforming both static-policy and baseline on at least 2 metrics with statistical significance
- At least 1 end-to-end scenario: 2-executor group → concurrent workloads → at least 1 resource conflict resolved by priority → both executors reach stable states
- At least 1 cross-validation scenario: closed-loop achieves measurably better throughput and lower rejection rate than baseline
- Existing 857 tests zero regression

## 9. Candidate Change Decomposition

Confirmed during IR/SR:

| # | Change name | Scope | Dependencies |
|---|---|---|---|
| 1/2 | `multi-executor-coordination` | ExecutorGroup, ExecutorGroupConfig, GroupCoordinator, GroupCoordinationResult, ResourceBudget, AdjustmentPriority, GroupLoopOrchestrator, GroupCoordinationEntry, GroupCoordinationHistory, CrossExecutorOscillationDetector, CoordinatedAdjustmentAdapter, unit tests | AdjustmentLoop, DecisionOrchestrator, AdjustmentAdapter, SafetyGate, ExecutorRegistry |
| 2/2 | `closed-loop-validation-and-evidence` | ClosedLoopValidationRunner, ValidationScenario, ValidationComparisonReport, MetricComparison, StatisticalSignificance, StatisticalSignificanceCalculator, end-to-end cross-validation tests | Change 1, ComparableScenarioRunner, NormalizedComparisonMetrics, LivePressureSampler |

### Independent verifiability check (pre-check)

- Change 1 can be independently compiled and tested: `ExecutorGroup`, `GroupCoordinator`, `ResourceBudget`, and `GroupLoopOrchestrator` can be tested with mock `AdjustmentLoop` instances. Budget enforcement and priority preemption are algorithmic — no real executor threads needed.
- Change 2 depends on Change 1 for `ExecutorGroup` and `GroupCoordinator` types, and on v0.12.0 for `ComparableScenarioRunner`. The validation logic (3-mode execution, metric comparison, statistical tests) is algorithmically independent.
- Both changes can independently run `mvn test` and pass their respective test suites.

## 10. Current Phase Exit

Before entering IR, the following must be complete:

1. `README.md` version index
2. `00-objectives-and-scope.md` (this document)
3. `decision-log.md` recording key design decisions
4. `docs/00-project/current-state.md` reflecting v0.15.0 version design draft status
