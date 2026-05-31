# Toolchain Readiness and Command Map

## Readiness Table

| Capability | Evidence to collect this phase | Status vocabulary |
|---|---|---|
| OpenSpec CLI installed | `openspec.cmd --version` | VERIFIED / FAILED |
| SuperSpec schema recognized | `openspec.cmd schemas` / schema validation | VERIFIED / FAILED |
| Codex entrypoint assets exist | inspect `.codex/skills/` only | PRESENT / ABSENT |
| Claude entrypoint assets exist | inspect `.claude/` only | PRESENT / ABSENT |
| `gh` usable for remote inspection | `gh auth status`, `gh repo view` or public API | VERIFIED / PARTIAL / FAILED |
| Superpowers skills required by apply | inspect actual Claude Code available skills/config paths without modifying them | VERIFIED_PRESENT / NOT_FOUND / NOT_VERIFIABLE_FROM_CODEX_ENVIRONMENT |
| Generated skills need refresh after context change | compare behavior/CLI guidance; do not refresh this phase | REQUIRED_LATER / NOT_REQUIRED / UNDETERMINED |

## SuperSpec v4 Flow

```text
brainstorm -> proposal -> specs -> tasks -> plan -> apply -> verify -> finalize
design is optional but expected for non-trivial architecture-sensitive change.
```

## Apply Dependencies

Apply depends on these Superpowers skills in the upstream workflow:

```text
superpowers:using-git-worktrees
superpowers:subagent-driven-development
superpowers:test-driven-development (transitively expected)
superpowers:requesting-code-review (transitively expected)
superpowers:executing-plans (fallback only)
```

## Current Environment Check

This phase only records whether the Codex-visible environment can confirm the presence of those skills and command pathways. It does not run a fake apply, create a test change, or modify `.claude/` or `.codex/`.

### Superpowers evidence from the current environment

- `C:\Users\18811\.claude\settings.json` shows `superpowers@claude-plugins-official` enabled.
- `C:\Users\18811\.claude\skills\` does not contain the named Superpowers apply-related skills.
- `C:\Users\18811\.codex\skills\` does not contain the named Superpowers apply-related skills.
- Result for the required apply skills: `NOT_VERIFIABLE_FROM_CODEX_ENVIRONMENT`.

## Command Map

```powershell
openspec.cmd --version
openspec.cmd schemas
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
gh auth status
gh repo view zhiwuli0228/DynamicThreadPollerManager
gh api repos/zhiwuli0228/DynamicThreadPollerManager/branches/claude_master
```

OpenSpec slash commands are recorded for reference only:

```text
/opsx:new
/opsx:continue
/opsx:apply
/opsx:verify
/opsx:archive
```
