# Version Design to Change Rule

## Rule

- A future version design may authorize OpenSpec change decomposition only when it explicitly reaches `READY_FOR_CHANGE_DECOMPOSITION` or `EXECUTION_AUTHORIZED`.
- Before that state, do not create `openspec/changes/**`.

## Change Naming Rule

Future changes use action-oriented kebab-case capability names, for example:

- `establish-local-managed-executor-registry`
- `support-runtime-executor-configuration-update`
- `expose-experiment-workloads-and-observability`

Do not use broad names such as `v1`, `complete-platform` or `all-features`.

## Boundary

- Archive files and bootstrap history do not authorize a change.
- Candidate roadmaps do not authorize a change.
- A task book alone does not authorize a change.

## Closeout and Synchronization Rule

After a future change is implemented and verified:

1. finalized change evidence remains under OpenSpec's archive lifecycle;
2. verified implemented behavior is synchronized to `openspec/specs/`;
3. if long-lived architecture changed, update `docs/01-architecture/`;
4. if a long-lived decision was accepted, add or update its ADR;
5. if the change only realizes the approved version scope without changing
   long-lived architecture, no new ADR is required.
