# Bootstrap Ledger

## Baseline Observed

- Repository: `DynamicThreadPollerManager`
- Branch: `main`
- Starting commit: `55e3203def02617c27ed013ab409ac771e84dc77`
- Spring Boot version: `4.0.6`
- Java version: `21`
- Build tool: `Maven`
- Initial test result: PASS (`.\mvnw.cmd test`)

## Toolchain Installed

- Node.js version: `v24.15.0`
- OpenSpec version: `1.3.1`
- OpenSpec tool integrations enabled: `Codex`, `Claude Code`
- Workflow/profile enabled: `superspec` schema copied into `openspec/schemas/superspec`
- SuperSpec source and detected schema/version information: upstream `danielhanold/superspec`, schema `SuperSpec` v4

## Files Added or Changed

- `openspec/config.yaml`
- `openspec/schemas/superspec/`
- `AGENTS.md`
- `CLAUDE.md`
- `docs/harness/project-harness.md`
- `docs/bootstrap/bootstrap-ledger.md`
- `.claude/`
- `.codex/`

## Validation Results

- Command: `openspec.cmd init --tools codex,claude --force`
  - Result: PASS
- Command: `.\mvnw.cmd test`
  - Result: PASS
- Command: `openspec.cmd validate --all --json`
  - Result: PASS
- Command: `openspec.cmd schema validate superspec`
  - Result: PASS

## Experience for Future Scaffold

### Stable template content

- `schema: superspec`
- a short `context:` block describing the repo, scope boundary, architecture, and engineering baseline
- root `AGENTS.md` and `CLAUDE.md`
- one concise `docs/harness/project-harness.md`
- one short bootstrap ledger in `docs/bootstrap/bootstrap-ledger.md`

### Dynamic detection required

- installed OpenSpec version
- upstream schema version
- current Spring Boot and Java versions
- active branch and starting commit
- initial test result

### Removed complexity

- No business change was created.
- No Redis, Kafka, database, frontend, or authentication dependency was added.
- No `/opsx:apply` flow was run.
