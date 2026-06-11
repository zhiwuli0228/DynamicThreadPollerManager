# closed-loop-experiment-verification Design

## Scope

A single focused end-to-end test class (`ClosedLoopExperimentTest`) that directly orchestrates the full pipeline on a real `ManagedExecutor`. No new source types.

## Test flow

```
1. ManagedExecutor(core=2, max=4, queue=10) created
2. Registered in ExecutorRegistry("experiment-executor")
3. ManagedExecutorAdjustmentAdapter created with safety gate + READY assessment
4. Long-running tasks submitted to consume core threads
5. Additional tasks submitted to fill queue → queue pressure
6. ExecutorStateSnapshot collected via adapter.currentState()
7. PressureSnapshot constructed from ExecutorStateSnapshot
8. ThresholdPolicyConfig with low queue threshold created
9. ThresholdPolicyEvaluator.evaluate() → PolicyDecision
10. PolicyDecision.toScaleDecision() → ScaleDecision
11. ScaleAdjustmentCommand created from ScaleDecision
12. adapter.apply(command)
13. Assert: status == APPLIED
14. Assert: afterState.corePoolSize() == targetPoolSize
15. Assert: afterState.maximumPoolSize() >= afterState.corePoolSize()
```

## Design decisions

1. **Direct orchestration**: The test directly calls `ManagedExecutor.submit()`, adapter methods, and policy evaluator — no `ScenarioExperimentRunner` or `BaselineWorkloadExecutor`. This follows SR F05.

2. **Real ThreadPoolExecutor**: No mocking. The test creates actual threads that do real work (sleep briefly to simulate load, then exit).

3. **Queue pressure generation**: Submit tasks that block briefly, filling the core threads and queue. This creates measurable queue pressure that triggers the scale-up policy.

4. **Policy config**: Use a `ThresholdPolicyConfig` with `scaleUpQueueSizeThreshold=1` and `scaleStep=3` so that a small amount of queue pressure triggers a scale-up to core=5 (from core=2 + step=3).

5. **Safety gate**: Use `SafetyGateConfig.defaults()` (cooldown=2, maxAdjustments=5). The gate should allow the first adjustment with READY status.

6. **Snapshot → PressureSnapshot bridge**: The `PressureSnapshot` constructor accepts (timestamp, activeThreads, poolSize, queueSize, completedTaskCount, cpuUtilization). The test maps `ExecutorStateSnapshot` fields directly.

7. **Deterministic verification**: Policy decision is deterministic given the same input (same snapshot values → same decision). The test verifies structural properties (status, pool size direction) rather than exact timing-dependent values.
