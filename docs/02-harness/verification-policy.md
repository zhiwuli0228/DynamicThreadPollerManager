# Verification Policy

## Required Evidence

- Confirm the file scope of the change.
- Run the repository validation commands requested by the task.
- Record the result of each check honestly.
- When another agent has already completed or archived work, verify the current repository state instead of relying only on that agent's report.

## Expected Checks for Documentation Work

- `git diff --name-only`
- `git diff --stat`
- `.\mvnw.cmd test`
- `openspec.cmd validate --all --json`
- `openspec.cmd schema validate superspec`

## Expected Checks After OpenSpec Archive or Handoff

- Confirm whether the active change still exists under `openspec/changes/<change>/`.
- Confirm whether the archived change exists under `openspec/changes/archive/<date>-<change>/`.
- Confirm whether synced main specs exist under `openspec/specs/<capability>/spec.md`.
- Run `openspec.cmd validate --all --json` after archive, because main specs have different structure from delta specs.
- Run `.\mvnw.cmd test` after code or test changes.
- Run `git status --short` and report whether the worktree is clean.
- Map each spec scenario to implementation and test evidence when verifying a completed change.
- For concurrent recorders, collectors, schedulers, and caches, inspect nested mutable state as well as outer container types.

## Handoff Evidence

Use this format when handing work to another agent:

```text
Final verification:
- Active change: none / <name>
- Archived change: <path or N/A>
- Main spec sync: <path or N/A>
- openspec validate: pass/fail, command used
- tests: pass/fail, command used, test count if available
- spec scenarios mapped: yes/no
- worktree clean: yes/no
- final commit: <sha>
- residual risks: <none or list>
```

## Rule

- Never claim validation that was not actually executed.
- Never treat task checkboxes alone as requirement coverage.
- Never treat a pre-archive `verify.md` as proof that post-archive main specs are valid.
