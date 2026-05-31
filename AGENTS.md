# AGENTS.md

Codex reads this file before design or review work.

Required inputs before making or reviewing a change:

- `docs/harness/project-harness.md`
- `openspec/config.yaml`
- The active change artifacts, if any exist

Default operating mode:

- Design and review first.
- Do not implement application code unless explicitly asked.
- Keep each change to one experimental capability.

Scope guardrails:

- Do not widen scope with unrelated refactors.
- Do not introduce Redis, Kafka, frontend, databases, or authentication without an explicit later change.
- Preserve the current architecture boundary and the OpenSpec / SuperSpec workflow.
