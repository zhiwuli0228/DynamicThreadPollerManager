# 管理变更阶段包模板

本文是后续版本或能力变更的最小阶段包模板。复制到 `docs/04-development/versions/<version>/` 后按实际版本改名和填写。

## 00 元信息

- Version:
- Capability / Change:
- Authoring date:
- Current stage:
- Authoritative branch:
- Source of truth:
- Related OpenSpec change:
- Current conclusion:

## 10 IR 需求分析

### 需求来源

- 用户确认项：
- 项目问题：
- 前置能力：

### 范围

范围内：

- 

范围外：

- 

### 验收草案

| AC ID | 验收语义 | 优先级 | 默认证据 |
| --- | --- | --- | --- |
| AC-001 |  | P0 |  |

### 风险和延期项

| 风险 | 当前判断 | 后续触发条件 |
| --- | --- | --- |
|  |  |  |

## 11 IR Review

### 输入包

- IR:
- 当前状态:
- 相关 specs:
- 相关代码:

### Findings

| ID | Priority | Finding | Impact | Recommended correction |
| --- | --- | --- | --- | --- |
|  |  |  |  |  |

### 结论

- `ready for disposition` / `blocked`

## 12 IR Review Disposition

| Finding ID | Decision | 修订内容 | 验证 | 残余风险 |
| --- | --- | --- | --- | --- |
|  | Accepted / Rejected / Deferred |  |  |  |

## 13 IR Closure Verification

| Finding ID | 闭环结论 | 核验证据 | 残余风险 |
| --- | --- | --- | --- |
|  | Closed / Open |  |  |

结论：

- `closed`
- `closed with recorded residual risk`
- `blocked`

## 20 SR 功能设计

### 设计目标

- 

### 模块边界

| 模块 | 职责 | 禁止事项 |
| --- | --- | --- |
|  |  |  |

### 数据模型 / 接口 / 状态

| 项 | 定义 | 失败语义 |
| --- | --- | --- |
|  |  |  |

### 追踪矩阵

| 用户确认项 / IR | SR 条目 | Spec 场景 | 实现文件 | 测试 ID | Evidence | 状态 | 残余风险 |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  | planned |  |

## 21 SR Review

| ID | Priority | Finding | Impact | Recommended correction |
| --- | --- | --- | --- | --- |
|  |  |  |  |  |

结论：

- `ready for disposition` / `blocked`

## 22 SR Review Disposition

| Finding ID | Decision | 修订内容 | 验证 | 残余风险 |
| --- | --- | --- | --- | --- |
|  | Accepted / Rejected / Deferred |  |  |  |

## 23 SR Closure Verification

| Finding ID | 闭环结论 | 核验证据 | 残余风险 |
| --- | --- | --- | --- |
|  | Closed / Open |  |  |

结论：

- `closed`
- `closed with recorded residual risk`
- `blocked`

## 30 Implementation Record

### 输入基线

- IR closure:
- SR closure:
- OpenSpec change:
- Current-state authorization:

### 实现范围

| 文件 | 变更摘要 | 对应 SR / Spec |
| --- | --- | --- |
|  |  |  |

### 验证命令

| 命令 | 结果 | 备注 |
| --- | --- | --- |
|  |  |  |

### 已知偏差和残余风险

| 项 | 判断 | 后续要求 |
| --- | --- | --- |
|  |  |  |

## 31 Implementation Review

| ID | Priority | Finding | Current behavior | Impact | Recommended correction |
| --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |

结论：

- `ready for disposition`
- `blocked`

## 32 Implementation Review Disposition

| Finding ID | Decision | 修订内容 | 验证 | 残余风险 |
| --- | --- | --- | --- | --- |
|  | Accepted / Rejected / Deferred |  |  |  |

## 33 Implementation Closure Verification

| Finding ID | 闭环结论 | 核验证据 | 残余风险 |
| --- | --- | --- | --- |
|  | Closed / Open |  |  |

结论：

- `closed`
- `closed with recorded residual risk`
- `blocked`

## 40 Test Design And Evidence

### 测试分层

| 层级 | 目标 | 测试资产 |
| --- | --- | --- |
| Unit |  |  |
| Integration |  |  |
| Acceptance / Evidence |  |  |

### 覆盖矩阵

| Spec / SR | 验收语义 | Test ID | 自动化状态 | Evidence |
| --- | --- | --- | --- | --- |
|  |  |  | automated / manual / deferred |  |

### 执行证据

| 命令 | 结果 |
| --- | --- |
|  |  |

## 41 Test Review

| ID | Priority | Finding | Impact | Recommended correction |
| --- | --- | --- | --- | --- |
|  |  |  |  |  |

## 42 Test Review Disposition

| Finding ID | Decision | 修订内容 | 验证 | 残余风险 |
| --- | --- | --- | --- | --- |
|  | Accepted / Rejected / Deferred |  |  |  |

## 43 Test Closure Verification

| Finding ID | 闭环结论 | 核验证据 | 残余风险 |
| --- | --- | --- | --- |
|  | Closed / Open |  |  |

## 50 Acceptance Precheck

### 反向核查

| 用户确认项 / AC | IR | SR | Spec | Implementation | Test | Evidence | Result |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  | pass / fail / risk |

### 仓库状态

- Active OpenSpec changes:
- Archived change:
- Main spec sync:
- `openspec.cmd validate --all --json`:
- tests:
- `git status --short`:
- final commit:

### 结论

- `acceptance candidate`
- `blocked`
- `closed with recorded residual risk`

## 51 Acceptance Precheck Verification

| 核查项 | 结论 | 证据 |
| --- | --- | --- |
| 需求到证据追踪完整 |  |  |
| 主 spec 已同步 |  |  |
| 当前状态已同步 |  |  |
| worktree 干净 |  |  |

最终结论：

- 

## 60 Retrospective

### 基本信息

- 交付对象：
- 完成日期：
- 归档或收尾提交：
- 参与 agent：

### 计划与实际

| 项 | 计划 | 实际 | 偏差 |
| --- | --- | --- | --- |
| 阶段推进 |  |  |  |
| 验证/归档 |  |  |  |
| 提交/交接 |  |  |  |

### 问题与根因

| 问题 | 影响 | 根因 | 是否已修复 |
| --- | --- | --- | --- |
|  |  |  | yes / no |

### 已采纳改进

| 改进项 | 落盘位置 | 状态 |
| --- | --- | --- |
|  |  | done / deferred |

### 后续要求

- 
