# CLAUDE.md

## Project State

- Authoritative branch for reviewed framework assets: `claude_master`.
- Current repository state: governance and architecture framework construction.
- No V1 capability implementation is currently approved.
- Do not implement dynamic thread-pool behavior without an approved active OpenSpec/SuperSpec change and an explicit implementation task.

## Mandatory Reading Before Any Future Implementation

1. `docs/harness/project-harness.md`
2. `docs/harness/02-architecture-and-dependency-rules.md`
3. `docs/harness/03-engineering-and-testing-rules.md`
4. `docs/harness/04-ai-delivery-workflow.md`
5. `docs/harness/05-change-classification-and-gates.md`
6. `docs/delivery/README.md`
7. Relevant `docs/architecture/*.md` documents referenced by the approved change.
8. All approved artifacts under the active `openspec/changes/<change-name>/`.

## Implementation Gate

Implementation is authorized only when all are true:

- V1 or later design has been reviewed.
- A bounded change exists and its required design artifacts are approved.
- The task explicitly instructs Claude Code to run the implementation flow.
- Required SuperSpec/Superpowers skills have been verified as available or an approved fallback path is documented.

## Execution Rules

- Implement only approved tasks.
- Use tests and verification required by the active change.
- Do not expand scope, add dependencies, alter architecture boundaries, or begin a neighboring capability without design revision.
- Record actual verification evidence and push only under task authorization.

## Engineering Baseline

- Java 21
- Maven Wrapper
- JUnit 5 and Mockito
- No PowerMock
- Deterministic concurrency testing rules are defined in `docs/harness/03-engineering-and-testing-rules.md`.
