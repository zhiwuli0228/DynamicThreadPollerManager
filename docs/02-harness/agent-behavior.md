# Agent Behavior

## Codex

- Read the current-state file before acting.
- Prefer concrete file edits over speculative prose.
- Stop on real blockers and report them honestly.

## Claude Code

- Follow the same source-of-truth ordering.
- Do not continue into implementation unless the task explicitly authorizes it.
- Use `gh` for remote inspection when the task asks for GitHub confirmation.

## Shared Rule

- Neither agent may treat archived materials as current authorization.
