# v0.6.0 补救措施包

## Header

- Package name: `v0.6.0-remediation`
- Package purpose: 为 `v0.6.0` 的“真实实验数据未落盘”问题提供一套独立、完整、可审阅的补救设计流程
- Authoring date: `2026-06-06`
- Status: `DRAFT`
- Current phase: `REMEDIATION_DESIGN_DRAFT`
- Authoritative branch: `claude_master`

## Package Intent

本补救包不修改 `v0.6.0` 原始 IR / SR / 计划文档，不覆盖原设计结论，也不把“能力闭环”改写成“真实数据已产出”。它只解决一个问题：

> `v0.6.0` 的能力已经完成，但仓库里没有可见的真实实验输出，必须有一套独立的补救设计来补齐这个缺口。

## Document Set

Current:

- [00-objectives-and-scope.md](./00-objectives-and-scope.md)
- [10-ir.md](./10-ir.md)
- [11-ir-review.md](./11-ir-review.md)
- [12-ir-review-disposition.md](./12-ir-review-disposition.md)
- [13-ir-closure-verification.md](./13-ir-closure-verification.md)
- [20-sr.md](./20-sr.md)
- [21-sr-review.md](./21-sr-review.md)
- [22-sr-review-disposition.md](./22-sr-review-disposition.md)
- [23-sr-closure-verification.md](./23-sr-closure-verification.md)
- [decision-log.md](./decision-log.md)
- [60-retrospective.md](./60-retrospective.md)
- [30-execution-task.md](./30-execution-task.md)
- [61-rectification-report.md](./61-rectification-report.md)
- [preflight-note.md](./preflight-note.md)
- [62-dispatch-process.md](./62-dispatch-process.md)
- [dispatch-checklist.md](./dispatch-checklist.md)

## Process

This package follows a complete remediation flow:

1. Problem framing and scope definition
2. Independent IR review
3. IR review disposition
4. IR closure verification
5. SR functional design
6. Independent SR review
7. SR review disposition
8. SR closure verification
9. Decision log and handoff

## Boundaries

Allowed:

- Define how the missing real data should be collected, validated, and reported.
- Define output artifacts, data quality gates, and evidence retention rules.
- Define a separate execution-ready design path for the corrective campaign.

Not allowed:

- Modify the original `v0.6.0` design docs.
- Pretend real data already exists when it has not been observed in the repository.
- Expand scope into queue resizing, production executor integration, or closed-loop controller work.

## Current Conclusion

This package is the independent design path for the corrective campaign. It exists to make the data gap explicit and to provide a complete process for the corrective work without rewriting the original `v0.6.0` narrative.
