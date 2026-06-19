# v0.13.0 IR Closure Verification

## Header

- Document type: IR closure verification
- Version name: `v0.13.0`
- Verified artifacts: `10-ir.md` (post-disposition), `11-ir-review.md`, `12-ir-review-disposition.md`
- Verification date: `2026-06-14`
- Verifier role: 独立 IR closure verifier（非 disposition 作者）
- Verification basis: `docs/02-harness/managed-change-standard.md` §2（IR 出口条件）

## 1. P0/P1 Finding Closure Verification

| Finding | 级别 | 处置 | 10-ir.md 更新验证 | 状态 |
|---|---|---|---|---|
| F01 | P0 | FIX | IR-v0.13-004 已添加 `withRejectedTaskCount(long)` 方法和 rejectedTaskCount 数据流澄清 | **CLOSED** |
| F02 | P0 | FIX | IR-v0.13-002 RECOVERY 条件改为纯趋势特征（queueGrowthRate < -threshold + utilization < 0.5 + maxQueueDepth > 0）；无状态要求不变 | **CLOSED** |
| F03 | P0 | FIX | IR-v0.13-003 ClassifierConfig 添加 queueCapacity 字段（默认 Integer.MAX_VALUE）；IR-v0.13-002 OVERLOAD 条件补充无界队列退化逻辑 | **CLOSED** |
| F04 | P1 | FIX | IR-v0.13-005 Safety 评分规则改为检查容量充足性/步长合理性/边界合理性；IR-v0.13-008 测试 5.5 同步修正 | **CLOSED** |
| F05 | P1 | FIX | IR-v0.13-005 Efficiency 术语统一为 `metrics().maxPoolSize()` | **CLOSED** |
| F06 | P1 | FIX | IR-v0.13-007 添加 `fromExecutor(ManagedExecutor, Instant, SystemCpuProbe)` 重载方法；原方法内部委托到重载 | **CLOSED** |

P2 findings deferred to SR（F07 compositeScore 构造器校验、F08 分类器不依赖 executor 配置）— 符合 managed-change-standard §2 出口条件（P2 可作为残余风险保留）。

## 2. IR 结构完整性验证

| 检查项 | 状态 |
|---|---|
| 需求来源明确（§1） | **PASS** — 引用 roadmap + 00-objectives-and-scope + 现有代码基线 + v0.12.0 DFR-01 |
| 范围内/范围外明确（§2） | **PASS** — 范围外 12 项与 decision-log DFR 一致 |
| 术语定义完整（§2.3） | **PASS** — 15 个术语定义（含新增：PressureState, PressureClassifier, PressureClassification, ClassifierConfig, NormalizedPressureMetrics, queueGrowthRate, threadUtilizationRatio, PolicyScore, PolicyScorer, ThresholdPolicyScorer, PolicyRanker, SystemCpuProbe 等） |
| IR 条目（§3） | **PASS** — 8 条（001-008），覆盖 state → classifier → classification/config → metrics → scorer → ranker → cpu probe → e2e |
| 验收条件草案（§4） | **PASS** — 28 条 AC（20 P0 + 8 P1） |
| 初步追踪矩阵（§5） | **PASS** — 8 行覆盖所有 IR |
| 风险和延期项（§6） | **PASS** — 5 项风险记录（分类准确性 P2、字段同步 P2、跨平台 P2、回归精度 P3、拒绝检测 P2、参数依赖 P3） |
| IR Review 输入包（§7） | **PASS** — 23 个文件引用完整 |
| 出口条件（§8） | **PASS** — 不再授权实现或 OpenSpec change |
| 当前结论（§9） | **PASS** |

## 3. 语义一致性验证

| 验证点 | 10-ir.md | decision-log | 00-objectives-and-scope | 一致性 |
|---|---|---|---|---|
| 压力状态数量 | 6 个（IR-v0.13-001） | D1: 6 个 | §7.1: 6 个 | **CONSISTENT** |
| 分类器输入 | List\<ObservedSnapshot\>（IR-v0.13-002） | D2: 时间序列 | §7.2: 时间序列 | **CONSISTENT** |
| 归一化指标数量 | 11 个（IR-v0.13-004） | D3: 11 个 | §7.3: 11 个 | **CONSISTENT** |
| 评分维度 | 4 维度 + 综合（IR-v0.13-005） | D4: 规则式启发 | §7.4: 4 维度 | **CONSISTENT** |
| 评分权重 | 0.35/0.30/0.20/0.15 | D4: 同上 | §7.4: 同上 | **CONSISTENT** |
| CPU probe 方案 | JDK ManagementFactory（IR-v0.13-007） | D5: JDK 内置 | §7.5: ManagementFactory | **CONSISTENT** |
| Change 分解 | 2 changes（§9） | D6: 双 change | §9: 候选双 change | **CONSISTENT** |
| NormalizedPressureMetrics 关系 | 独立 record（IR-v0.13-004） | D3: 独立 record | §7.3: 独立 record | **CONSISTENT** |
| 范围外: 闭环调整 | 明确排除 | DFR-01 | §1.5: 明确排除 | **CONSISTENT** |

## 4. Managed Change Standard 出口条件验证

| 出口条件 | 状态 |
|---|---|
| 独立需求评审完成 | **PASS** — `11-ir-review.md` 完成，8 个 findings |
| 所有 P0/P1 findings 已处置 | **PASS** — 3 P0 + 3 P1 → FIX → verified in disposition |
| 残余风险已记录 | **PASS** — F07 (P2), F08 (P2), 分类准确性 (P2), 字段同步 (P2), 跨平台 (P2), 回归精度 (P3), 拒绝检测 (P2), 参数依赖 (P3) |
| 明确允许进入 SR 功能设计 | **PASS** — 本文结论明确 |

## 5. 残余风险登记

| ID | 级别 | 描述 | 记录位置 | 触发条件 |
|---|---|---|---|---|
| RR-01 | P2 | NormalizedPressureMetrics 与 NormalizedComparisonMetrics 9 字段同步 | IR-v0.13-004 | NormalizedComparisonMetrics 增加字段时需同步 |
| RR-02 | P2 | 分类器准确性与人工标注的一致性未验证 | IR-v0.13-002 | 需要实际运行数据做对比验证 |
| RR-03 | P2 | 非 Sun JDK 上 CPU probe 返回 0.0 | IR-v0.13-007 | 使用非 OpenJDK 发行版时 |
| RR-04 | P2 | compositeScore 构造器校验移除——依赖 scorer 实现保证 | IR-v0.13-005 (F07) | scorer 实现不一致时 |
| RR-05 | P3 | queueGrowthRate 线性回归对短窗口极值敏感 | IR-v0.13-004 | trendWindowSize < 5 且数据波动大时 |
| RR-06 | P3 | OVERLOAD 条件在无界队列时退化为绝对阈值 | IR-v0.13-002 | 无界队列场景下 OVERLOAD 判定可能偏高 |
| RR-07 | P2 | REJECTION_ACTIVE 依赖 rejectedTaskCount 外部注入 | IR-v0.13-004 (F01) | 消费方未注入 rejectedTaskCount 时分类器无法检测拒绝 |

## 6. FIX Disposition 交叉验证

对 6 个 FIX 处置进行交叉验证：

| FIX | 修改的 IR 条目 | 是否引入新矛盾 | 是否影响其他 IR 条目 |
|---|---|---|---|
| F01: withRejectedTaskCount() | IR-v0.13-004 | 无 | IR-v0.13-002（REJECTION_ACTIVE 条件现在可达）— 正向影响 |
| F02: RECOVERY 纯趋势 | IR-v0.13-002 | 无——纯趋势检测与无状态要求完全一致 | IR-v0.13-008 测试 2.6——同步修正 |
| F03: ClassifierConfig.queueCapacity | IR-v0.13-003 | 无 | IR-v0.13-002（OVERLOAD 条件可用）— 正向影响 |
| F04: Safety 规则重定义 | IR-v0.13-005 | 无 | IR-v0.13-008 测试 5.5——同步修正 |
| F05: 术语统一 | IR-v0.13-005 | 无 | 无 |
| F06: fromExecutor 重载 | IR-v0.13-007 | 无——向后兼容，原方法签名不变 | 无 |

## 7. Verification Conclusion

**IR closure verified.** All 6 P0/P1 findings are FIXED and verified in the updated `10-ir.md`. IR structure is complete with 8 entries, 28 acceptance criteria, and a full traceability matrix. Seven residual risks are recorded with trigger conditions. Cross-validation confirms no FIX introduces new contradictions.

Key structural improvements from disposition:
1. `rejectedTaskCount` data flow is now explicit: `fromSnapshots()` → 0 → `withRejectedTaskCount()` → classifier
2. RECOVERY detection is now purely trend-based (queueGrowthRate < -threshold), compatible with stateless classifier
3. `ClassifierConfig` now includes `queueCapacity` making OVERLOAD condition computable
4. Safety scoring rules are now testable against realistic config scenarios
5. CPU probe injection is testable via `fromExecutor()` overload

**Gate status: PASS** — v0.13.0 IR phase is closed. Authorized to proceed to SR functional design phase.

Next step: Create `20-sr.md` — v0.13.0 SR functional design.
