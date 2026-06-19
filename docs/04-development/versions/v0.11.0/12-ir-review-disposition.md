# v0.11.0 IR Review Disposition

## Header

- Document type: IR review disposition
- Version name: `v0.11.0`
- Reviewed artifact: `docs/04-development/versions/v0.11.0/11-ir-review.md`
- Disposition date: `2026-06-13`
- Disposition by: IR author, responding to independent review

## Disposition Summary

| Total Findings | FIX | DEFER_TO_SR | ACCEPT | CLOSED |
|---|---|---|---|---|
| 7 | 4 | 2 | 0 | 1 |

## Per-Finding Disposition

### F01 [P0] JSON 反序列化无法正确推断 Long 类型 → **FIX**

**处置**: IR 明确类型策略——选择方案 B（专用工厂方法），放弃泛型类型推断。

**具体内容**:

`PressureSnapshot` JSON 反序列化：
- `AcquisitionJsonWriter` 新增 `parse(String json)` → `Map<String, Object>` 方法（仅此一个反序列化入口）
- `PressureSnapshot` 新增 `toMap()` 实例方法 → `Map<String, Object>`（含所有 6 个字段）
- `PressureSnapshot` 新增 `static fromMap(Map<String, Object>)` 工厂方法——根据 map 中的值类型精确构造：整数值→int（activeThreads/poolSize/queueSize）、长整数值→long（completedTaskCount）、浮点值→double（cpuUtilization）。若 map 不包含 `poolSize` 和 `completedTaskCount`，调用 4-arg 构造器并补 0
- `RuntimeObservation` 新增 `toMap()` 实例方法——每个 MetricValue 转换为 `{"status": "PRESENT"/"ABSENT", "value": ...}`
- `RuntimeObservation` 新增 `static fromMap(Map<String, Object>)` 工厂方法——根据字段名已知目标类型（activeThreads→Integer, completedTaskCount→Long 等），从 value 中精确转型
- `ObservedSnapshot` 新增 `toMap()` 和 `static fromMap(Map<String, Object>)`，内嵌 PressureSnapshot 和 RuntimeObservation 的 Map 表示

反序列化流程：
```
JSON string → AcquisitionJsonWriter.parse() → Map<String, Object>
    → ObservedSnapshot.fromMap() → 内嵌调用 PressureSnapshot.fromMap() + RuntimeObservation.fromMap()
```

IR 更新：IR-v0.11-002 的验收语义从"泛型类型推断"改为"专用工厂方法 + Map 中间层"。`MetricValue` 反序列化不需要泛型推断——每个使用 `MetricValue` 的快照类型内部已知字段的目标类型。

---

### F02 [P0] AcquisitionJsonWriter 扩展方案缺失 → **FIX**

**处置**: IR 选择方案 A——快照类型提供 `toMap()`/`fromMap()`，`AcquisitionJsonWriter` 只负责 Map↔JSON 转换。

**具体内容**:

`AcquisitionJsonWriter` 变更：
- 保持 package-private（不提升可见性——metrics 包不需要直接调用它）
- 新增 `static Map<String, Object> parse(String json)` 方法：将 JSON 字符串解析为 Map/List/String/Number/Boolean/null 的 Java 对象图（仅此一个新增方法）
- 现有 `render(Object)` 方法不变

快照类型的序列化/反序列化方法放在各自类型自身（`PressureSnapshot`/`RuntimeObservation`/`ObservedSnapshot`），而非放在 `AcquisitionJsonWriter` 中。

IR 更新：IR-v0.11-002 的 6 个方法不再要求添加到 `AcquisitionJsonWriter`，改为要求各快照类型提供 `toMap()`/`fromMap()` 方法。`AcquisitionJsonWriter` 只需新增 `parse()` 一个方法。

---

### F03 [P1] buildObservation() 逻辑无法复用 → **FIX**

**处置**: IR 推荐在 `RuntimeObservation` 上添加静态工厂方法 `fromExecutor()`。

**具体内容**:

`RuntimeObservation` 新增：
```java
public static RuntimeObservation fromExecutor(ManagedExecutor executor, Instant timestamp) {
    // 读取 7 个指标，cpuUtilization 设为 absent
}
```

`ManagedExecutorScenarioRunner.buildObservation()` 改为委托给 `RuntimeObservation.fromExecutor()`。

`LivePressureSampler` 直接调用 `RuntimeObservation.fromExecutor()` 构建观测值。

IR 更新：IR-v0.11-004 的验收语义从"复用 ManagedExecutorScenarioRunner.buildObservation() 的读取逻辑"改为"调用 RuntimeObservation.fromExecutor(executor, timestamp) 构建观测值"。

---

### F04 [P1] FileBackedEvidenceRecorder I/O 异常类型 → **FIX**

**处置**: IR 补充推荐异常策略。

**具体内容**:

- `record()` 的写入顺序：先更新内存缓冲（`CopyOnWriteArrayList.add()`），再追加写入文件。若文件写入失败（`IOException`），抛出 `UncheckedIOException`——内存中数据仍可用
- `flush()` 将当前缓冲中尚未写入磁盘的数据刷入文件，失败时抛出 `UncheckedIOException`
- `closeSession()` 内部调用 `flush()`——若 flush 失败，session 状态仍为 ACTIVE（不清除缓冲，不写入 metadata 文件）
- 不引入自定义异常类型（使用标准库 `UncheckedIOException`）

IR 更新：IR-v0.11-001 补充 I/O 错误处理段落。

---

### F05 [P1] LivePressureSampler 线程安全分析 → **CLOSED**

**处置**: 接受 IR review 的分析。IR review 已逐方法验证了 `ManagedExecutor` 7 个 getter 的线程安全性——均为无锁近似值或 volatile 读，在多线程环境下安全。没有需求变更。

对于 `getQueueSize()`：`ManagedExecutor` 使用 `LinkedBlockingQueue`，`size()` 方法是线程安全的。确认无风险。

IR 更新：IR-v0.11-004 补充线程安全逐方法分析表（引用 IR review 的验证结果）。

---

### F06 [P2] 端到端测试采样数断言可能 flaky → **DEFER_TO_SR**

**处置**: DEFER_TO_SR。IR 推荐 SR 使用宽松断言（`>= 3`）或基于 `CountDownLatch` 的确定性测试。

IR 更新：无（SR 决定具体测试策略）。

---

### F07 [P2] AcquisitionReportPaths 命名约定一致性 → **DEFER_TO_SR**

**处置**: DEFER_TO_SR。SR 中按 `AcquisitionReportPaths` 现有模式新增 4 个方法（`evidenceFileName`/`sessionMetadataFileName`/`evidenceFile`/`sessionMetadataFile`）。`.jsonl` 后缀保持不变。

IR 更新：无（SR 设计具体 API）。

---

## 修改后的 IR 更新计划

| IR 条目 | 变更 |
|---|---|
| IR-v0.11-001 | 补充 I/O 错误处理策略（先写内存再写文件，`UncheckedIOException`） |
| IR-v0.11-002 | 序列化方案从"泛型推断 + 6 个 AcquisitionJsonWriter 方法"改为"toMap/fromMap + parse()"；`AcquisitionJsonWriter` 只需新增一个 `parse()` 方法 |
| IR-v0.11-004 | `LivePressureSampler` 使用 `RuntimeObservation.fromExecutor()` 替代复制 buildObservation()；补充线程安全逐方法分析表 |

## 处置后状态

| Finding | 原始级别 | 处置 | 状态 |
|---|---|---|---|
| F01 | P0 | FIX (专用工厂方法 + Map 中间层，放弃泛型推断) | CLOSED |
| F02 | P0 | FIX (toMap/fromMap + parse，AcquisitionJsonWriter 只加一个方法) | CLOSED |
| F03 | P1 | FIX (RuntimeObservation.fromExecutor() 静态工厂) | CLOSED |
| F04 | P1 | FIX (先写内存再写文件，UncheckedIOException) | CLOSED |
| F05 | P1 | CLOSED (已验证线程安全，无需求变更) | CLOSED |
| F06 | P2 | DEFER_TO_SR (宽松断言或 CountDownLatch 测试) | CLOSED |
| F07 | P2 | DEFER_TO_SR (AcquisitionReportPaths 命名约定) | CLOSED |

## 出口条件

所有 P0/P1 findings 已处置。可进入 IR closure verification。
