# Artifact Boundary

## OpenSpec Artifact Meaning

- `openspec/specs/**` records verified implemented behavior. Every
  spec at this level MUST contain both `## Purpose` and
  `## Requirements` headers; the `openspec validate` pipeline and
  the `openspec-archive-guard.ps1` post-archive mode both fail when
  either is missing.
- `openspec/changes/**` records bounded proposed capability work
  before archive. The change directory MUST be removed (moved to
  `openspec/changes/archive/<date>-<name>/`) before archive is
  considered complete.
- `openspec/changes/archive/<date>-<name>/**` records the historical
  artifacts of an archived change. Presence here is one of the four
  required states for archive completion.
- `openspec/config.yaml` records stable repository-level execution
  rules. It does not own dynamic authorization state; that stays in
  `docs/00-project/current-state.md`. No stage label, current
  authorized change, or other dynamic fact is permitted to be
  hard-coded in `openspec/config.yaml`; only conditional rules keyed
  on those dynamic values are allowed.

## Boundary Rule

- Do not use OpenSpec artifacts as a substitute for current-state authority.
- Do not write implementation claims into spec artifacts before the behavior is actually implemented and verified.
- Archive completion requires the four states enumerated in
  `docs/02-harness/verification-policy.md` to agree. The
  `scripts/openspec-archive-guard.ps1` script is the canonical
  machine-checkable gate.
