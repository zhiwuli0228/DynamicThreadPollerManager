# pressure-data-acquisition-and-baseline verify 门禁格式复盘

## 交付对象

- Change: `pressure-data-acquisition-and-baseline`
- 日期: `2026-06-06`
- 阶段: `apply` 完成后的 `verify` 收尾

## 现象

在执行 `/opsx:verify` 时，系统要求先处理以下事项：

- 更新 `docs/00-project/current-state.md` 第 16 行，使其使用门禁脚本可识别的授权格式。
- 提交 `apply.md` 与这次格式修复，然后再重新执行 `/opsx:verify`。

表面上看，这像是在“强迫先提交”，但本质上是在要求把 **状态、证据和提交历史** 对齐后再继续验证。

## 为什么会发生

### 1. current-state 的授权写法和脚本正则不一致

`scripts/openspec-archive-guard.ps1` 只识别以下两种授权格式：

- `Change name: <name>`
- `Authorized OpenSpec change: <name>`

而 `docs/00-project/current-state.md` 第 16 行曾写成：

- ``- `pressure-data-acquisition-and-baseline`. ``

这对人可读，但对门禁脚本不可识别，所以 `pre-finalize` 失败，`verify` 只能返回修复要求。

### 2. apply.md 是收尾证据，不是可忽略的背景文件

`apply.md` 记录了本次实现的任务完成情况、提交范围和下一步动作。
如果它仍处于未提交状态，verify 就无法把“实现已完成”与“工作区状态已落盘”对应起来。

### 3. verify 不是只看实现，还要看仓库状态一致性

这个仓库的验证策略要求：

- 真实执行 `openspec validate --all --json`
- 真实执行 `git status --short`
- 真实确认 `current-state.md` 与实际 change 状态一致

因此，`verify` 提示先提交，并不是多此一举，而是在阻止“状态文件已改、证据还悬着”的半成品收尾。

## 影响

- 弱 agent 会把“可读但不可识别”的状态写法误当成已完成。
- 用户会看到类似“先提交再 verify”的提示，误以为系统流程卡死。
- 如果不统一门禁格式，后续每次收尾都可能重复出现同类问题。

## 已采纳的修复

- 将 [current-state.md](/E:/009workspace/claudecode/DynamicThreadPollerManager/docs/00-project/current-state.md) 第 16 行改为：
  - `Authorized OpenSpec change: \`pressure-data-acquisition-and-baseline\``
- 保留同一节里的 change 路径说明，避免失去语义信息。
- 计划把 `apply.md` 与这次修复一起纳入提交，让 verify 看到的是统一状态。

## 后续改进

### 1. 所有授权行必须采用脚本识别格式

以后凡是会被门禁读取的授权字段，必须使用固定格式，不再只写人类可读的简写。

### 2. 收尾时先做状态对齐，再做验证

agent 在执行 verify 前，必须先确认：

- `current-state.md` 的授权格式是否被门禁识别；
- `apply.md`、`verify.md`、治理修正是否已经进入同一收尾提交；
- `git status --short` 是否反映了真实的收尾状态。

### 3. 复盘必须落到治理文档或模板

如果复盘结论是“门禁格式不一致”，后续必须同步到：

- `docs/02-harness/managed-change-standard.md`
- `docs/08-retrospectives/agent-handoff-closeout-standard.md`
- 相关脚本或模板

不能只保留在本复盘正文里。

## 结论

这次 `verify` 要求“先提交”，不是流程故障，而是门禁在阻止不一致状态继续向下游传播。  
根因是授权写法没有使用脚本认可的固定格式。  
修复方式是：统一 current-state 授权格式、把 apply / fix 一起提交、再重新执行 verify。
