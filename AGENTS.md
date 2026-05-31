# AGENTS.md

## Project State

- Authoritative branch: `claude_master`.
- Current authorized work type: `DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY`.
- Current source of truth: `docs/00-project/current-state.md`.
- Archived task documents are non-authoritative.
- Do not create version designs, OpenSpec capability changes, or application code unless a later explicit task updates `docs/00-project/current-state.md`.

## Mandatory Reading Order

For governance or documentation-framework work:

1. `docs/README.md`
2. `docs/00-project/current-state.md`
3. `docs/02-harness/context-policy.md`

For long-lived architecture work:

1. `docs/01-architecture/README.md`
2. relevant documents under `docs/01-architecture/`

For future version design work only after explicit authorization:

1. `docs/04-development/versions/README.md`
2. the authorized version design files under `docs/04-development/versions/<version>/`

For OpenSpec work only after version design status allows decomposition:

1. `docs/03-openspec/README.md`
2. `docs/03-openspec/version-design-to-change-rule.md`
3. the authorizing version design files

## Codex Responsibility

- Codex is the design and governance execution agent.
- Codex may revise documentation framework artifacts only within explicit task authorization.
- Codex does not implement application code by default.

## Scope Guardrails

- Do not begin version design during documentation-framework-only tasks.
- Do not create OpenSpec changes without explicit authorization.
- Do not combine multiple roadmap capabilities implicitly.
- Do not add dependencies or application code during documentation work.
- Do not modify generated OpenSpec/agent assets unless a task explicitly authorizes regeneration.

## GitHub and Review Source

- GitHub branch `claude_master` is the review source of truth.
- `gh` CLI may be used for remote inspection, branch verification, and future PR operations.
- Push completed bounded documentation work directly to `claude_master` only when the task authorizes it.
