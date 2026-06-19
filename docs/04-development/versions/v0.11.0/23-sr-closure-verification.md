# v0.11.0 SR Closure Verification

## Header

- Document type: SR closure verification
- Version name: `v0.11.0`
- Verified artifacts: `21-sr-review.md`, `22-sr-review-disposition.md`, `20-sr.md` (updated)
- Verification date: `2026-06-13`
- Verifier: SR author (post-disposition verification)

## Closure Verification

### P0 Findings

| Finding | Description | Disposition | Verified |
|---|---|---|---|
| F01 | `record()` 在 session CLOSED 后抛异常 | FIX — `record()` 增加 session 状态检查 | [x] |
| F02 | 文件命名约定内部不一致 | FIX — 统一 `{runId}-{type}.{ext}` 模式 + 委托路径 | [x] |

### P1 Findings

| Finding | Description | Disposition | Verified |
|---|---|---|---|
| F03 | 硬编码版本路径 + 重复路径逻辑 | FIX — 构造器接受 `(Path, String)` + 委托 `AcquisitionReportPaths` | [x] |
| F04 | `start()` TOCTOU 竞态 | FIX — `AtomicBoolean.compareAndSet()` | [x] |
| F05 | 采样失败静默吞异常 | FIX — log + 连续失败计数器 + 熔断 (`MAX_CONSECUTIVE_FAILURES=10`) | [x] |

### P2 Findings

| Finding | Description | Disposition | Verified |
|---|---|---|---|
| F06 | `.replace()` 模式脆弱 | DEFER_TO_IMPLEMENTATION (F02 已覆盖新增方法) | [x] |
| F07 | `RecordingSessionMetadata` JSON 往返测试缺失 | DEFER_TO_IMPLEMENTATION (change 1 实现时增加) | [x] |

## SR 正向检查复核

- [x] SR 功能设计覆盖全部 IR 需求（IR-v0.11-001 到 IR-v0.11-006）
- [x] 10 个组件设计均有明确的包、职责、API 契约
- [x] IR FIX 项 (F01-F04) 全部在 SR 组件设计中落地
- [x] IR DEFER 项 (F06/F07) 在 SR 中有明确决策
- [x] 架构约束满足：依赖方向无循环，模块边界清晰
- [x] `FileBackedEvidenceRecorder` 放入 `experiment.acquisition`——避免 metrics → acquisition 循环依赖
- [x] `EvidenceRecorder` 接口不变——`FileBackedEvidenceRecorder` 实现同一接口
- [x] `PressureSampler` 接口不变——`LivePressureSampler` 实现同一接口
- [x] `ManagedExecutorScenarioRunner` 向后兼容——现有 5-arg 构造器不变
- [x] `ManagedExecutor`、`ManagedExecutorConfig` 不修改
- [x] 现有 535 测试零回归约束明确
- [x] 新增测试分层清晰：单元 → 集成 → 端到端 → 回归
- [x] 19 个 AC 覆盖 P0 关键路径 + P1 边界保护
- [x] 不涉及新依赖、外部 API、REST/UI
- [x] Change decomposition 独立可验证——change 1 可独立编译和测试
- [x] 5 个端到端测试场景覆盖持久化、采样、集成、并发、停止
- [x] SR FIX 项 (F01-F05) 全部在 20-sr.md 中落地

## SR Review Disposition 落地验证

逐项验证 22-sr-review-disposition.md 的 FIX 项是否已在 20-sr.md 中正确应用：

| FIX | SR 修改位置 | 验证 |
|---|---|---|
| F01 | 4.6 `record()` line 401-404: `session.status() == SessionStatus.ACTIVE` 检查 | [x] |
| F02 | 4.6 `evidenceFilePath()`/`sessionMetadataPath()` 委托给 `AcquisitionReportPaths` | [x] |
| F02 | 4.7 四个新方法改用直接拼接 `runId + "-evidence.jsonl"` | [x] |
| F02 | 4.7 设计决策注释修正为 `{runId}-evidence.jsonl` | [x] |
| F03 | 4.6 构造器签名 `(Path outputRoot, String versionTag)` + `AcquisitionReportPaths.forVersion()` | [x] |
| F04 | 4.8 `AtomicBoolean running` + `compareAndSet()` in `start()`/`stop()` | [x] |
| F05 | 4.8 `AtomicInteger consecutiveFailures` + `MAX_CONSECUTIVE_FAILURES=10` + 熔断 | [x] |
| F05 | 4.8 成功采样后 `consecutiveFailures.set(0)` | [x] |
| F07 | 6.1 分层测试表 `FileBackedEvidenceRecorder` 行补充 "session metadata JSON 往返" | [x] |

## Deferred to Implementation

以下事项已明确推迟到实现阶段处理：

| 事项 | 来源 | 处置 |
|---|---|---|
| 现有 `AcquisitionReportPaths` 6 个旧方法的 `.replace()` 模式 | F06 | 不在 v0.11.0 scope；新方法已改用直接拼接 |
| `RecordingSessionMetadata` JSON 往返测试 | F07 | change 1 `FileBackedEvidenceRecorder` 测试中增加 |
| I/O 错误注入测试（磁盘满、权限拒绝） | 21-sr-review 测试评估 | 实现阶段按需增加 |

## 验证结论

**All P0/P1 findings CLOSED.** SR review 发现的 7 个 findings 已全部处置（5 FIX + 2 DEFER_TO_IMPLEMENTATION）。两个 P0 阻断项——`record()` session 状态检查（F01）和文件命名一致性（F02）——已在 SR 文档中修正。三个 P1 关键项——路径管理（F03）、TOCTOU 竞态（F04）、异常处理（F05）——已在 SR 文档中修正。P2 findings（F06/F07）有明确的实现阶段处置方案。

**SR closure verified. 可以进入 `READY_FOR_CHANGE_DECOMPOSITION` 阶段。**
