# Verification Policy

## Required Evidence

- Confirm the file scope of the change.
- Run the repository validation commands requested by the task.
- Record the result of each check honestly.
- When another agent has already completed or archived work, verify the current repository state instead of relying only on that agent's report.
- For managed changes, verify the stage gate required by `docs/02-harness/managed-change-standard.md` before accepting handoff or advancing stages.

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
- Run `scripts/openspec-archive-guard.ps1 -Mode post-archive -ChangeName <change>` and record the result. A non-zero exit is a hard gate, not a soft warning.
- Run `openspec.cmd validate --all --json` after archive, because main specs have different structure from delta specs.
- Run `.\mvnw.cmd test` after code or test changes.
- Run `git status --short` and report whether the worktree is clean.
- Confirm that every synced main spec contains both `## Purpose` and `## Requirements`.
- Confirm that `docs/00-project/current-state.md` no longer claims the archived change is still active or execution-authorized.
- Confirm that any active change authorization line in `docs/00-project/current-state.md` uses a script-recognized fixed format before relying on it in verify or archive guard checks.
- Confirm that `openspec list --json` does not still reference the archived change.
- Map each spec scenario to implementation and test evidence when verifying a completed change.
- For concurrent recorders, collectors, schedulers, and caches, inspect nested mutable state as well as outer container types.
- Confirm that a retrospective record exists under `docs/08-retrospectives/` for the completed demand/change, or record that retrospective creation is still pending and the closeout is not fully complete.

## Archive Completion Gate (Hard Rule)

Archive is complete only when all of the following four states agree.
A missing or divergent state is a hard fail; `mv
openspec/changes/<name> openspec/changes/archive/...` alone is never
sufficient. The `scripts/openspec-archive-guard.ps1` script
encapsulates this gate and MUST be used as the canonical check.

1. Archive directory present at
   `openspec/changes/archive/<date>-<name>/` containing the change's
   artifacts.
2. Synced main spec at `openspec/specs/<capability>/spec.md` for
   every capability, containing both `## Purpose` and
   `## Requirements`.
3. `docs/00-project/current-state.md` no longer lists the archived
   change as active and no longer declares
   `Current stage: EXECUTION_AUTHORIZED`.
4. Clean worktree — `git status --short` is empty, or its non-empty
   entries are items current-state explicitly permits.

`openspec list --json` MUST also agree: it MUST NOT reference an
archived change as active.

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
- managed-change gate satisfied: yes/no/N/A
- worktree clean: yes/no
- final commit: <sha>
- residual risks: <none or list>
```

## Rule

- Never claim validation that was not actually executed.
- Never treat task checkboxes alone as requirement coverage.
- Never treat a pre-archive `verify.md` as proof that post-archive main specs are valid.
- Never treat file moves into `openspec/changes/archive/**` as archive completion without a green post-archive guard.
- Never treat review disposition as closure unless a closure verification record exists or the current task explicitly defines a lighter gate.
- Never treat archive completion as final closeout if the mandatory retrospective step is still missing.
