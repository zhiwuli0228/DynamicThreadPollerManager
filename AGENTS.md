# AGENTS.md

## Project State

- Authoritative branch: `claude_master`.
- Current phase: framework construction and alignment.
- Harness Constitution and Living Architecture are established.
- V1 unified design has not started.
- No capability change may be created unless explicitly authorized by a later task document.

## Mandatory Reading Order

For governance or framework work:

1. `docs/harness/project-harness.md`
2. `docs/delivery/README.md`
3. Task-specific referenced files.

For V1 unified design work, only after explicitly authorized:

1. `docs/harness/project-harness.md`
2. all `docs/harness/00-*.md` through `05-*.md`
3. `docs/architecture/README.md`
4. all detailed `docs/architecture/*.md`
5. `docs/delivery/README.md`
6. `openspec/config.yaml`

For a future bounded change design:

1. Harness rules relevant to scope and gates.
2. Architecture documents relevant to the capability.
3. Delivery workflow rules.
4. `openspec/config.yaml`.
5. Existing specs and active change artifacts, if any.

## Codex Responsibility

- Codex is the design and governance execution agent.
- Codex may create or revise architecture, workflow, and future approved SuperSpec design artifacts only within an explicit task authorization.
- Codex does not implement application code by default.

## Scope Guardrails

- Do not begin V1 unified design during framework-alignment tasks.
- Do not create OpenSpec changes without explicit authorization.
- Do not combine multiple roadmap capabilities implicitly.
- Do not add dependencies or application code during documentation/tooling work.
- Do not modify generated OpenSpec/agent assets unless a task explicitly authorizes regeneration.

## GitHub and Review Source

- GitHub branch `claude_master` is the review source of truth.
- `gh` CLI may be used for remote inspection, branch verification and future PR operations.
- Push completed bounded documentation work directly to `claude_master` only when the task authorizes it.
