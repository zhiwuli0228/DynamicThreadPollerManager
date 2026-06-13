# acquisition-paths-and-quality-gates Finalize

## Header

- Change identifier: `acquisition-paths-and-quality-gates`
- Finalize date: (filled after implementation and verification)
- Finalizer: (filled after implementation)

## Pre-Archive State Check

1. [ ] `openspec validate --all --json` is fully green.
2. [ ] Active change directory `openspec/changes/acquisition-paths-and-quality-gates/` exists.
3. [ ] Synced main spec at `openspec/specs/acquisition-paths-and-quality-gates/spec.md` contains `## Purpose` and `## Requirements`.
4. [ ] `docs/00-project/current-state.md` lists this change as active with `EXECUTION_AUTHORIZED`.
5. [ ] `mvn test` exits 0 with all tests passing.
6. [ ] `scripts/openspec-archive-guard.ps1 -Mode pre-finalize -ChangeName acquisition-paths-and-quality-gates` exits 0.

## Post-Archive Checklist

1. [ ] `openspec/changes/acquisition-paths-and-quality-gates/` no longer exists (moved to archive).
2. [ ] Archive directory `openspec/changes/archive/<date>-acquisition-paths-and-quality-gates/` exists.
3. [ ] `openspec/specs/acquisition-paths-and-quality-gates/spec.md` exists with `## Purpose` and `## Requirements`.
4. [ ] `docs/00-project/current-state.md` no longer lists this change as active.
5. [ ] `openspec list --json` does not reference this change.
6. [ ] `scripts/openspec-archive-guard.ps1 -Mode post-archive -ChangeName acquisition-paths-and-quality-gates` exits 0.
7. [ ] `git status --short` is clean or contains only items current-state authorizes.
8. [ ] Branch restored to `claude_master`.
9. [ ] Also sync `pressure-data-acquisition-and-baseline` spec if modified.

## Archive Receipt

(Filled after archive — confirms synchronization of spec, current-state, and worktree.)
