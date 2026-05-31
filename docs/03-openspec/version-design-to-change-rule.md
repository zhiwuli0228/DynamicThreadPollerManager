# Version Design to Change Rule

## Rule

- A future version design may authorize OpenSpec change decomposition only when it explicitly reaches `READY_FOR_CHANGE_DECOMPOSITION` or `EXECUTION_AUTHORIZED`.
- Before that state, do not create `openspec/changes/**`.

## Boundary

- Archive files and bootstrap history do not authorize a change.
- Candidate roadmaps do not authorize a change.
- A task book alone does not authorize a change.
