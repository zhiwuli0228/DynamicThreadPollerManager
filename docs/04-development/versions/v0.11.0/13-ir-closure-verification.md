# v0.11.0 IR Closure Verification

## Header

- Document type: IR closure verification
- Version name: `v0.11.0`
- Verified artifacts: `11-ir-review.md`, `12-ir-review-disposition.md`
- Verification date: `2026-06-13`
- Verifier: IR author (post-disposition verification)

## Closure Verification

### P0 Findings

| Finding | Description | Disposition | Verified |
|---|---|---|---|
| F01 | JSON 反序列化无法正确推断 Long 类型 | FIX — 专用工厂方法 + Map 中间层 | [x] |
| F02 | AcquisitionJsonWriter 扩展方案缺失——无 JSON parser | FIX — toMap/fromMap + parse() 一个方法 | [x] |

### P1 Findings

| Finding | Description | Disposition | Verified |
|---|---|---|---|
| F03 | buildObservation() 是 private，无法复用 | FIX — RuntimeObservation.fromExecutor() 静态工厂 | [x] |
| F04 | FileBackedEvidenceRecorder I/O 异常类型未定义 | FIX — 先写内存再写文件 + UncheckedIOException | [x] |
| F05 | LivePressureSampler 线程安全假设需验证 | CLOSED — 已验证，无需求变更 | [x] |

### P2 Findings

| Finding | Description | Disposition | Verified |
|---|---|---|---|
| F06 | 端到端测试采样数断言可能 flaky | DEFER_TO_SR（宽松断言或 CountDownLatch） | [x] |
| F07 | AcquisitionReportPaths 命名约定一致性 | DEFER_TO_SR（按现有模式新增 4 个方法） | [x] |

## IR 正向检查复核

- [x] IR 只做需求分析，不隐含实现授权
- [x] 6 条 IR 覆盖 FileBackedEvidenceRecorder → 端到端验证完整链路
- [x] Scope 边界明确：排除跨 run 聚合、压缩、CPU 数据源、数据库存储
- [x] 序列化架构明确：toMap/fromMap + parse()（F02 处置）
- [x] JSON 类型处理明确：专用工厂方法（F01 处置）
- [x] buildObservation() 复用方案明确：RuntimeObservation.fromExecutor()（F03 处置）
- [x] I/O 错误处理策略明确：先写内存再写文件 + UncheckedIOException（F04 处置）
- [x] LivePressureSampler 线程安全已逐方法验证（F05 处置）
- [x] 复用现有基础设施：AcquisitionJsonWriter、AcquisitionReportPaths、EvidenceRecorder、PressureSampler
- [x] EvidenceRecorder 接口不变——FileBackedEvidenceRecorder 实现同一接口
- [x] PressureSampler 接口不变——LivePressureSampler 实现同一接口
- [x] ManagedExecutorScenarioRunner 向后兼容——现有构造器不变
- [x] 端到端测试覆盖持久化 + 自主采样 + 并发 + 异常路径
- [x] 19 个 AC 覆盖 P0 关键路径 + P1 边界保护
- [x] 不涉及新的 executor mutation、外部依赖、REST/API/UI
- [x] 现有 535 测试零回归约束已明确

## Deferred to SR

以下事项已明确推迟到 SR 阶段决策：

| 事项 | 来源 | 推荐方向 |
|---|---|---|
| 端到端测试采样数断言策略 | F06 | 宽松断言（>= 3）或 CountDownLatch 确定性测试 |
| AcquisitionReportPaths 新增方法 | F07 | evidenceFileName/sessionMetadataFileName + 完整路径方法 |
| JSON parser 实现细节 | F01/F02 | 支持 Map/List/String/Number/Boolean/null 的轻量解析器 |
| LivePressureSampler 与 executor shutdown 的交互 | P1 风险 | 读取 post-shutdown 状态，不抛异常 |

## 验证结论

**All P0/P1 findings CLOSED.** IR review 发现的 7 个 findings 已全部处置（4 FIX + 2 DEFER_TO_SR + 1 CLOSED）。两个 P0 阻断项——JSON 类型推断（F01）和 AcquisitionJsonWriter 扩展方案（F02）——通过 toMap/fromMap + parse() 架构方案解决。三个 P1 关键项——buildObservation() 复用（F03）、I/O 异常类型（F04）、线程安全验证（F05）——已处置或确认。P2 findings（F06/F07）有明确的 SR 推荐方向。

**IR closure verified. 可以进入 SR（功能设计）阶段。**
