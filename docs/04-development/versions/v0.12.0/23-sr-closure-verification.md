# v0.12.0 SR Closure Verification

## Header

- Document type: SR closure verification
- Version name: `v0.12.0`
- Verified artifacts: `20-sr.md` (post-disposition), `21-sr-review.md`, `22-sr-review-disposition.md`
- Verification date: `2026-06-14`
- Verifier role: 独立 SR closure verifier（非 disposition 作者）
- Verification basis: `docs/02-harness/managed-change-standard.md` §3（SR 出口条件）

## 1. P0/P1 Finding Closure Verification

| Finding | 级别 | 处置 | 20-sr.md 更新验证 | 状态 |
|---|---|---|---|---|
| F01 | P0 | FIX | `parseArtifact()` 修正为 `AcquisitionJsonWriter.parse(json)` | **CLOSED** |
| F02 | P0 | FIX | `comparisonReportFileName()` 修正为 `{comparisonId}-comparison.json`；`writeComparisonReport()` 委托给 `comparisonReportFileName()` | **CLOSED** |
| F03 | P1 | FIX | §4.8 `ComparisonJsonWriter` 改用 toMap/fromMap + render/parse（~40 行）；各 record 类型新增 toMap/fromMap；移除直接 StringBuilder 序列化 | **CLOSED** |
| F04 | P1 | FIX | `ComparableScenarioRunner.compare()` 中 `mRejected = mOutcome.rejectedTaskCount()` | **CLOSED** |
| F05 | P1 | FIX | `fromSnapshots()` 文档明确 `fallbackPoolSize` 仅在空 snapshot 列表时使用 | **CLOSED** |

P2 findings deferred（F06 v0.11.0 hardcoded path, F07 conclusion null）— 符合 managed-change-standard §3 出口条件。

## 2. SR 结构完整性验证

| 检查项 | 状态 |
|---|---|
| 模块边界（§3）— 包、变更类型、职责、禁止事项 | **PASS** |
| 数据模型（§4.1-4.5, 4.7）— 7 个 record + class + Builder | **PASS** |
| 接口/类/组件设计（§4.1-4.11）— 11 个组件 | **PASS** |
| 状态枚举和失败语义 — `MetricDelta.direction` (IMPROVED/REGRESSED/NEUTRAL) | **PASS** |
| 依赖方向和禁止依赖（§3） | **PASS** |
| 安全、并发、资源、观测边界 — `AtomicLong` rejection counting, 顺序执行 | **PASS** |
| 测试映射（§6.1-6.4） | **PASS** |
| 非范围再次声明（§8） | **PASS** |
| 任务切分明确性（§5 Change Decomposition — 2 changes） | **PASS** |

## 3. SR 伪代码强制验证规则 — 通过

3 个随机 API 调用点已验证签名匹配（21-sr-review.md §Review Method）:
- `ScenarioExperimentRunner` 6-arg constructor — **PASS**
- `ManagedExecutorScenarioRunner.run(ScenarioDefinition, ManagedExecutorConfig)` — **PASS**
- `AcquisitionReportPaths.forVersion(String)` — **PASS**

## 4. IR → SR FIX 落地验证

| IR Fix | SR 落地位置 | 落地方式 | 状态 |
|---|---|---|---|
| F01 精确数据流 | §4.6 `compare()` 8-step flow | `recorder.snapshots(outcome.runId())` 路径 | **VERIFIED** |
| F02 JSON 格式 | §4.8 `managedConfigToMap()` | 6 字段 map + `TimeUnit.name()` + `ThreadMode.name()` | **VERIFIED** |
| F03 rejection counting | §4.10 `ManagedExecutor` + §4.11 `ScenarioRunOutcome` | `AtomicLong` + handler wrapper + outcome field | **VERIFIED** |
| F04 queueCapacity 转换 | §4.1 `toBaselinePreset()` | -1→MAX_VALUE, 0→0, >0→direct | **VERIFIED** |
| F05 runner 实例化 | §4.6 `compare()` 内 `new` | 每次调用动态创建 runner 实例 | **VERIFIED** |
| F06 wall-clock 计时 | §4.6 `clock.get()` before/after run | `totalDurationMs = endMs - startMs` | **VERIFIED** |

## 5. Managed Change Standard 出口条件验证

| 出口条件 | 状态 |
|---|---|
| 独立功能设计评审完成 | **PASS** — `21-sr-review.md` 完成，7 findings |
| 所有 P0/P1 findings 已处置 | **PASS** — 2 P0 + 3 P1 → FIX → verified in 20-sr.md |
| P2 残余风险有非阻塞理由 | **PASS** — F06 (DEFER), F07 (DEFER_TO_IMPLEMENTATION) |
| 明确允许创建或授权 OpenSpec change | **PASS** — 本文结论明确 |

## 6. 残余风险登记

| ID | 级别 | 描述 | 记录位置 | 触发条件 |
|---|---|---|---|---|
| RR-SR-01 | P2 | v0.11.0 evidence/session 路径硬编码 vs v0.12.0 comparison 路径分离 | SR F06 | 跨版本路径统一需求出现时 |
| RR-SR-02 | P2 | ComparisonReportArtifact.conclusion null 处理 | SR F07 | 实现 agent 确保反序列化一致 |

## 7. Verification Conclusion

**SR closure verified.** All 2 P0 + 3 P1 findings are FIXED and verified in the updated `20-sr.md`. SR structure is complete with 11 component designs, full module boundaries, dependency directions, testing strategy, and change decomposition. All 6 IR FIX items are verifiably incorporated. Two P2 residual risks are recorded with non-blocking rationale.

**Gate status: PASS** — v0.12.0 SR phase is closed. Authorized to proceed to `READY_FOR_CHANGE_DECOMPOSITION` and OpenSpec change creation.

Next step: Update `docs/00-project/current-state.md` to `READY_FOR_CHANGE_DECOMPOSITION`, then create OpenSpec changes using `/opsx:new`.
