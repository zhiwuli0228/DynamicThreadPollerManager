# Lifecycle Rule

## Lifecycle Summary

1. Authorize a version design.
2. Decompose the version design into bounded change(s) only when the design says it is ready.
3. Apply and verify the approved changes.
4. Archive or sync the results according to the task.

## Current Stage Rule

- `DOCUMENT_FRAMEWORK_CONSTRUCTION_ONLY` means steps 1 to 4 are not yet open for capability work.
- The current stage label itself is dynamic and MUST be read from
  `docs/00-project/current-state.md` at runtime. No stage label is
  permitted to be hard-coded in any other document or tool
  configuration; only conditional rules keyed on stage values are
  allowed.

## Evidence Rule

- Artifact lifecycle statements must reflect actual repository state, not desired state.
- Archive completion requires the following four states to agree. A
  missing or divergent state is a hard fail; `mv
  openspec/changes/<name> openspec/changes/archive/...` alone is
  never sufficient:
  1. **Archive directory** at `openspec/changes/archive/<date>-<name>/`
     containing the change's artifacts.
  2. **Synced main spec** at `openspec/specs/<capability>/spec.md`
     for every capability listed in the proposal, containing both
     `## Purpose` and `## Requirements` headers.
  3. **`docs/00-project/current-state.md`** no longer lists the
     archived change as an active authorized change and no longer
     declares `Current stage: EXECUTION_AUTHORIZED`.
  4. **Clean worktree** — `git status --short` is empty, or its
     non-empty entries are items the current-state authority
     explicitly permits (e.g. in-flight untracked work that the
     current session is actively authoring). A dirty worktree with
     uncommitted implementation files is a hard fail.
- Use `scripts/openspec-archive-guard.ps1` as the default repository
  check before claiming a change is archive-ready or archived. The
  guard is a hard gate: a non-zero exit MUST be treated as evidence
  that archive is incomplete, never as a soft warning.
- The `openspec list --json` output MUST also agree with the four
  states above: it MUST NOT reference an archived change as active.
