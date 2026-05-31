# CLAUDE.md

## Project State

- Authoritative branch for reviewed framework assets: `claude_master`.
- Current repository state: governance framework, architecture baseline, delivery framework, and Phase 05 autonomous delivery policy alignment.
- V1 unified design is in progress and documented separately.
- No V1 capability implementation is currently approved by this Phase 05 task.
- Do not implement dynamic thread-pool behavior without an active authorized V1 autonomous implementation mission.

## Mandatory Reading Before Any Future Implementation

1. `docs/harness/project-harness.md`
2. `docs/harness/02-architecture-and-dependency-rules.md`
3. `docs/harness/03-engineering-and-testing-rules.md`
4. `docs/harness/04-ai-delivery-workflow.md`
5. `docs/harness/05-change-classification-and-gates.md`
6. `docs/delivery/README.md`
7. `docs/v1/README.md` and the active V1 mission draft when mission execution is authorized.
8. Relevant `docs/architecture/*.md` documents referenced by the approved change.
9. All approved artifacts under the active `openspec/changes/<change-name>/`.

## Implementation Gate

Implementation is authorized only when all are true:

- A bounded V1 autonomous implementation mission exists.
- The mission explicitly authorizes Claude Code to run the implementation flow continuously.
- The mission scope, exclusions, verification commands, and closeout path are documented.
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
- Deterministic concurrency testing rules are defined in `docs/harness/03-engineering-and-testing-rules.md`.
