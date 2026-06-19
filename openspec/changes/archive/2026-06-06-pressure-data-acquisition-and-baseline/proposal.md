## Why

当前项目已经具备 scenario、metrics、policy、replay/readiness 和 adjustment 的基础能力，但还缺少一个受管理的 pressure data acquisition 基线。没有统一的 acquisition 契约，后续压测数据就容易变成临时脚本结果，无法稳定比较，也无法为下一阶段的 executor mutation 或 queue resizing 提供可信输入。

## What Changes

**Pressure Data Acquisition**
- From: 压测数据获取依赖临时流程和零散证据。
- To: 建立一个受控、可复现的数据获取能力，统一生成 manifest、summary、readiness 和 evidence index。
- Reason: 让后续数据收集可审计、可比较、可复核。
- Impact: 新增一个 bounded OpenSpec capability，不改变已有 runtime mutation 边界。

**Report Hygiene**
- From: 原始 evidence 是否保留、如何命名和归档没有统一约束。
- To: 默认只输出受控摘要报告，raw evidence 需要显式声明保留位置和责任。
- Reason: 避免版本控制膨胀和证据语义混乱。
- Impact: 影响未来实现的输出目录和证据处理方式。

## Capabilities

### New Capabilities
- `pressure-data-acquisition-and-baseline`: 受控 acquisition 编排、manifest、summary、readiness、evidence index 与 report hygiene。

### Modified Capabilities
- None.

## Impact

未来实现会新增一个围绕压力数据获取的 bounded capability、对应的报告输出目录和验证规则，但不会修改现有 executor mutation、queue resizing、production executor integration 或任何已归档 capability 的 requirements。
