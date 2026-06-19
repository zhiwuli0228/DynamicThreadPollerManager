# v0.8.0 IR Closure Verification

## Header

- Document type: IR closure verification
- Version name: `v0.8.0`
- Verified artifact: `docs/04-development/versions/v0.8.0/10-ir.md` (IR corrected per disposition)
- References: `11-ir-review.md`, `12-ir-review-disposition.md`
- Verification date: `2026-06-12`
- Verifier role: independent closure verifier (not the IR reviewer, not the disposition author)

## 1. Verification Scope

Verify that all P0 blocking findings and P1 critical findings from `11-ir-review.md` have been properly closed per the disposition in `12-ir-review-disposition.md`.

## 2. P0 Closure Verification

### F01 — Registry lifecycle ownership → FIX

**Disposition**: Amend IR-v0.8-002 with registry internal creation clauses.

**Verification**: Line 108 of `10-ir.md` now reads:

> `ExecutorRegistry` 和 `AtomicDeletionSafety` 由 runner 内部创建，不作为构造参数注入。调用方不感知 registry 的存在。

Phase 1 now reads "注册到内部 `ExecutorRegistry`" (line 111). Phase 6 now specifies "先确认 `isTerminated()`" before registry.remove().

**Status**: `CLOSED`. Registry lifecycle ownership is unambiguously assigned to runner internals.

### F02 — Runner-to-Report bridge missing → FIX

**Disposition**: New IR-v0.8-007 "Runner-to-Report Bridge".

**Verification**: `10-ir.md` now contains IR-v0.8-007 (lines 205-239) defining:

- `EvidenceRecorder.snapshots(runId)` extraction → `PressureSummary` aggregation
- `ManagedExecutorConfig` + `ScenarioDefinition` → `RunManifest` construction
- `AcquisitionReportWriter.writeAll()` invocation
- Default values for `ReplaySummary` and `ReadinessSummary` in runner-only mode
- SR ownership decision for bridge location

AC-v0.8-008 in the acceptance matrix (line 253) maps to IR-v0.8-007.

**Status**: `CLOSED`. Full bridge specification is present.

### F03 — G7 extendedFieldPresence unspecified → FIX

**Disposition**: Specify data structure in IR-v0.8-006.

**Verification**: G7 clause in `10-ir.md` now specifies:

> `AcquisitionDataSet.RunSnapshot` 新增可选字段 `Map<String, Boolean> extendedFieldPresence`，默认值为空 Map

Key names: `"poolSize"`, `"completedTaskCount"`, `"keepAliveTimeSeconds"`, `"largestPoolSize"`, `"taskCount"`. Skip behavior for empty/null (backward compatibility with v0.6.0 data). Failure condition: non-empty but required key missing or false.

**Status**: `CLOSED`. G7 data structure is concrete and implementable.

## 3. P1 Closure Verification

### F04 — sleep(100ms) determinism → DEFER_TO_SR

**Disposition**: DEFER_TO_SR with risk record.

**Verification**: IR-v0.8-002 Phase 3 now reads:

> `Thread.sleep(100)` 或等效同步机制；SR 可选择 `startedLatch` 等确定性屏障替代

The risk table (line 267) records "线程调度不确定性导致 G8 偶发失败" as P1 risk with SR trigger.

**Status**: `CLOSED`. IR leaves mechanism choice to SR; risk is recorded.

### F05 — G8 STEADY false positive → FIX

**Disposition**: Per-profile G8 thresholds.

**Verification**: G8 clause in `10-ir.md` now specifies:

> `STEADY` profile 不要求 `queueSize > 0`。`RAMP` profile 至少 1 个 snapshot `queueSize > 0`。`BURST` profile 至少 2 个 snapshot `queueSize > 0`。

Fallback to 1 snapshot if profile info unavailable.

**Status**: `CLOSED`. G8 no longer penalizes correct STEADY behavior.

### F06 — fromExecutorState() cross-package dependency → DEFER_TO_SR

**Disposition**: DEFER_TO_SR with architecture note.

**Verification**: IR-v0.8-004 (line 170) retains the constraint:

> 不得在 `SnapshotAssembler` 中引入 `ManagedExecutor` 依赖（只依赖 `ExecutorStateSnapshot`，它在 `experiment.adjustment` 包中）。

The cross-package dependency direction (`experiment.metrics` → `experiment.adjustment`) is implicit in the method signature. SR must explicitly permit this direction. Risk table is not directly affected (the dependency is on a pure data class).

**Status**: `CLOSED`. IR correctly constrains the dependency; SR resolves permissibility.

### F07 — toPresetSummary() semantic drift → DEFER_TO_SR

**Disposition**: DEFER_TO_SR with decision pending.

**Verification**: IR-v0.8-001 (line 97) retains `toPresetSummary()` returning `RunManifest.BaselinePresetSummary`. The semantic drift (policyId field) is a known tradeoff recorded in the disposition. SR decision log will document the `"managed-executor-v0.8.0"` placeholder value.

**Status**: `CLOSED`. IR preserves backward compatibility; SR makes the explicit mapping decision.

### F08 — RAMP capping unspecified → FIX

**Disposition**: Add concrete capping semantics.

**Verification**: RAMP clause in `10-ir.md` now reads:

> 当 `2 + i > queueCapacity + maximumPoolSize` 时，提交 `queueCapacity + maximumPoolSize` 个任务（不抛 `RejectedExecutionException`）。默认配置（core=2, max=4, queue=10）下 `2+7=9 < 14`，cap 不会被触发。

**Status**: `CLOSED`. Capping behavior is concrete and includes the default-config safe case.

## 4. P2 Closure Verification

### F09 — Permissive safety gate undefined → FIX

**Disposition**: Remove adapter dependency; use `executor.toSnapshot()` directly.

**Verification**: IR-v0.8-002 (line 109) now states:

> Runner 不创建 `ManagedExecutorAdjustmentAdapter`——runner 只采样不调整，通过 `executor.toSnapshot()` 直接读取 TPE 状态。

Phase 3 reads "通过 `executor.toSnapshot()` 读取真实 TPE 状态". Phase count reduced from 8 to 7 (no Phase for adapter creation).

**Status**: `CLOSED`. Adapter dependency eliminated; runner simplifies to direct snapshot access.

### F10 — Idle state standard undefined → DEFER_TO_SR

**Disposition**: DEFER_TO_SR.

**Verification**: IR-v0.8-002 Phase 3 (line 122) now reads:

> 等待 executor 回到空闲状态（SR 定义空闲判断标准）

**Status**: `CLOSED`. IR defers concrete idle condition to SR; disposition recommends `queueSize==0 && activeCount==0`.

## 5. Cross-Check: No New Issues Introduced

The following were verified after all FIX amendments:

- [x] IR-v0.8-002 constructor parameters unchanged — `ExperimentCoordinator`, `ScenarioPlanner`, `PressureSampler`, `EvidenceRecorder`, `Supplier<Instant>` clock remain the only constructor args.
- [x] IR-v0.8-007 does not introduce new dependencies — it references existing types only (`EvidenceRecorder`, `PressureSummary`, `RunManifest`, `AcquisitionReportWriter`).
- [x] F09 fix (remove adapter) does not contradict IR-v0.8-004 (SnapshotAssembler integration) — these are independent concerns (sampling vs. pipeline transformation).
- [x] AC matrix updated correctly — AC-v0.8-008 maps to IR-v0.8-007; AC-v0.8-009 (formerly AC-v0.8-008) retains global regression check.
- [x] Tracking matrix updated with IR-v0.8-007 row.
- [x] Conclusion section updated to reflect 7 IR items and review completion.
- [x] No scope creep — all amendments stay within the original `00-objectives-and-scope.md` boundaries.
- [x] No implementation authorization implied — all IR items use "候选验收语义" and "必须" language consistent with requirements phase.

## 6. Verification Summary

| Finding | Level | Disposition | Status |
|---|---|---|---|
| F01 | P0 | FIX | CLOSED |
| F02 | P0 | FIX | CLOSED |
| F03 | P0 | FIX | CLOSED |
| F04 | P1 | DEFER_TO_SR | CLOSED |
| F05 | P1 | FIX | CLOSED |
| F06 | P1 | DEFER_TO_SR | CLOSED |
| F07 | P1 | DEFER_TO_SR | CLOSED |
| F08 | P1 | FIX | CLOSED |
| F09 | P2 | FIX | CLOSED |
| F10 | P2 | DEFER_TO_SR | CLOSED |

All 3 P0 findings closed via FIX amendments to `10-ir.md`. All 5 P1 findings closed (2 FIX + 3 DEFER_TO_SR with risk records). Both P2 findings closed (1 FIX + 1 DEFER_TO_SR).

## 7. Conclusion

IR v0.8.0 is **ready for SR transition**. All P0 blockers are resolved. P1 deferred items carry clear SR triggers. No new issues introduced by amendments. Recommend updating `docs/00-project/current-state.md` to authorize `v0.8.0` SR functional design.
