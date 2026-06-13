# real-executor-data-acquisition Finalize

## Header

- Change identifier: `real-executor-data-acquisition`
- Finalize date: (filled after implementation and verification)
- Finalizer: (filled after implementation)

## Pre-Archive State Check

1. [ ] `openspec validate --all --json` is fully green.
2. [ ] Active change directory `openspec/changes/real-executor-data-acquisition/` exists.
3. [ ] Synced main spec at `openspec/specs/real-executor-data-acquisition/spec.md` contains `## Purpose` and `## Requirements`.
4. [ ] `docs/00-project/current-state.md` lists this change as active with `EXECUTION_AUTHORIZED`.
5. [ ] `mvn test` exits 0 with all tests passing.
6. [ ] `scripts/openspec-archive-guard.ps1 -Mode pre-finalize -ChangeName real-executor-data-acquisition` exits 0.

## Post-Archive Checklist

1. [ ] `openspec/changes/real-executor-data-acquisition/` no longer exists (moved to archive).
2. [ ] Archive directory `openspec/changes/archive/<date>-real-executor-data-acquisition/` exists.
3. [ ] `openspec/specs/real-executor-data-acquisition/spec.md` exists with `## Purpose` and `## Requirements`.
4. [ ] `docs/00-project/current-state.md` no longer lists this change as active.
5. [ ] `openspec list --json` does not reference this change.
6. [ ] `scripts/openspec-archive-guard.ps1 -Mode post-archive -ChangeName real-executor-data-acquisition` exits 0.
7. [ ] `git status --short` is clean or contains only items current-state authorizes.
8. [ ] Branch restored to `claude_master`.

## Archive Receipt

(Filled after archive — confirms synchronization of spec, current-state, and worktree.)
