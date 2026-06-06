# v0.6.0 补救措施目标与范围

## Header

- Package name: `v0.6.0-remediation`
- Status: `DRAFT`
- Current phase: `REMEDIATION_DESIGN_DRAFT`
- Requirement theme: remedial real-data acquisition for the v0.6.0 pressure data gap
- Current conclusion: `v0.6.0` 的能力已完成，但真实实验输出未在仓库中落盘；本包只定义补救设计，不修改原始能力文档

## 1. 问题陈述

`v0.6.0` 已经形成了 pressure acquisition 的能力闭环，但当前仓库里没有可见的真实实验输出文件，因此无法证明“已经采集到并保存了真实数据”。

## 2. 目标

本补救包的目标是定义一套独立、完整的补救流程，用于：

- 明确要补采哪些数据；
- 明确数据输出应落到哪里；
- 明确数据质量门禁；
- 明确证据如何被审阅与保留；
- 明确补救完成后的验收条件。

## 3. 范围内

- 真实数据补采需求；
- 输出目录与 artifact 契约；
- 数据质量门禁；
- evidence retention 规则；
- 补救流程的 review / disposition / closure verification。

## 4. 范围外

- 修改 `v0.6.0` 原始 IR / SR / 计划正文；
- 新能力设计；
- queue resizing；
- 生产 `ThreadPoolExecutor` 集成；
- closed-loop controller；
- REST/API/UI；
- 新依赖；
- 性能提升承诺。

## 5. 成功标准

- 能明确说明真实数据应该如何采集并落盘；
- 能明确说明输出文件的最小集合；
- 能明确说明缺失数据时如何阻断后续推进；
- 能明确说明补救完成后如何验证其真实性与一致性。

## 6. 当前阶段出口

进入 IR review 前必须完成：

1. 明确真实数据缺口；
2. 明确补救范围与非范围；
3. 明确输出 artifact 约定；
4. 明确数据质量门禁；
5. 明确补救不会覆盖原始 `v0.6.0` 设计文档。
