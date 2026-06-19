# v0.6.0 补救措施 IR Review

## 输入包

- IR: `10-ir.md`
- 当前状态: `docs/00-project/current-state.md`
- 相关 specs: `openspec/specs/pressure-data-acquisition-and-baseline/spec.md`
- 相关代码: `src/main/java/com/zhiwu/dynamicthreadpollermanager/experiment/acquisition/`

## Findings

| ID | Priority | Finding | Impact | Recommended correction |
| --- | --- | --- | --- | --- |
| REM-V060-001 | P1 | 真实数据缺口已经识别，但 IR 还需要更明确的输出目录契约。 | 若不明确目录，后续执行 agent 仍可能把数据写散。 | 在 SR 中固定补救输出目录和文件命名。 |
| REM-V060-002 | P1 | IR 明确了补救不是重写原始设计，但还需要更严格的非范围说明。 | 若不加严边界，补救设计可能被误读成新版本。 | 在 SR 再次声明不修改原始 v0.6.0 文档，不新增能力。 |
| REM-V060-003 | P2 | IR 已识别 raw evidence 风险，但未最终定义清理责任。 | 若不明确责任，补救执行后可能留下不可控输出。 | 在 SR 与后续执行计划中固定 retention / cleanup 责任。 |

## 结论

- `ready for disposition`

