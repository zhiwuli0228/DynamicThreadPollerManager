# CLAUDE.md

## Project State

- Authoritative branch for reviewed framework assets: `claude_master`.
- Current repository state: `EXECUTION_AUTHORIZED` for the `metrics-snapshot-and-recording` change.
- Current implementation authorization: `metrics-snapshot-and-recording` only; do not begin a neighboring capability.
- Do not implement code outside the active authorized change, modify dependencies, or create new capability changes without updating the version design and `docs/00-project/current-state.md`.
- Only `docs/00-project/current-state.md` and an active authorized task may permit implementation.

## Mandatory Reading Before Any Future Implementation

1. `docs/README.md`
2. `docs/00-project/current-state.md`
3. `docs/02-harness/context-policy.md`
4. `docs/02-harness/task-execution-policy.md`
5. `docs/03-openspec/version-design-to-change-rule.md`
6. `docs/04-development/versions/README.md`
7. the active version design files, if and only if a future task authorizes them

## Implementation Gate

Implementation is authorized only when all are true:

- `docs/00-project/current-state.md` explicitly authorizes a later implementation stage.
- A bounded version design exists and is marked ready for change decomposition or execution.
- The task explicitly instructs Claude Code to run the implementation flow.
- Required SuperSpec/Superpowers skills have been verified as available or an approved fallback path is documented.

## Execution Rules

- Implement only approved tasks.
- Use tests and verification required by the active mission and each approved change.
- Do not expand scope, add dependencies, alter architecture boundaries, or begin a neighboring capability without design revision.
- Record actual verification evidence and push only under task authorization.
- Under an active mission, Claude Code may create, apply, verify, finalize, commit, push, and perform approved `gh` actions without waiting for phase-by-phase human approval.
- Stop only for documented BLOCKED conditions or mission-scope expansion.

## Engineering Baseline

- Java 21
- Maven Wrapper
- JUnit 5 and Mockito
- No PowerMock
- Deterministic concurrency testing rules are defined in `docs/04-development/testing-guide.md`.
