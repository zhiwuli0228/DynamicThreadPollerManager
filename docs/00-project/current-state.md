# Current State

## Authoritative Status

- Current stage: `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
- Authoritative branch: `claude_master`
- Source of truth for execution authority: this file
- Version design status: `v0.1.0` is ready for change decomposition
- OpenSpec capability changes: `experiment-foundation` has been archived and the baseline implementation is present on `claude_master`
- Java implementation status: the experiment foundation package and tests are present on the main working branch

## What Is Allowed Now

- Maintain the documentation framework.
- Organize project facts, architecture, harness, OpenSpec rules, version design carriers, domain terms, operations notes, templates, and archive material.
- Update cross-links so future work can discover the correct authority sequence quickly.
- Continue bounded development from the delivered experiment foundation baseline.
- Keep the current-state record synchronized with the actual repository state.

## What Is Not Allowed Now

- No unreviewed scope expansion.
- No branch-state mismatch between the workspace and the authoritative branch.
- No archive or finalize event without synchronized authority records.
- No capability change outside the approved version-design path.

## Future Gate Sequence

1. `CAPABILITY_BASELINE_DELIVERED_AND_MAIN_SYNCED`
2. version design draft
3. version design baseline
4. `READY_FOR_CHANGE_DECOMPOSITION`
5. `EXECUTION_AUTHORIZED`
6. capability change execution

## Current Reference

The repository currently preserves earlier bootstrap and framework history in `docs/99-archive/`. Those files are historical evidence only.
