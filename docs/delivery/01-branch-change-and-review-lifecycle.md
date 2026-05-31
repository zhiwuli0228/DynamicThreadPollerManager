# Branch, Change, and Review Lifecycle

## A. Framework / Governance Work

```text
Work on claude_master
  -> change only authorized framework files
  -> run validation
  -> commit with docs/chore message
  -> push origin claude_master
  -> ChatGPT reviews remote commit through GitHub
```

This flow applies to Harness, Architecture, Delivery Framework, Agent/OpenSpec context alignment, and Bootstrap ledger work.

## B. Future V1 Capability Work

SuperSpec v4 recommends a feature branch as the normal starting point for future approved changes. A future approved V1 design would therefore use a branch-oriented flow such as:

```text
Approved V1 unified design
  -> create a dedicated feature/spec branch from claude_master
  -> Codex produces bounded SuperSpec design artifacts on that branch
  -> optional pre-review PR using gh
  -> ChatGPT/user approves design
  -> Claude Code executes apply in SuperSpec-managed worktree
  -> verify and finalize
  -> gh-backed PR/review or approved closeout path
  -> merge accepted result toward claude_master
```

## Lifecycle Rules

- `claude_master` is the integration and review baseline for framework work.
- The future feature branch naming, PR policy, and merge policy for V1 are not decided in this phase.
- `gh` may be used for remote inspection, branch verification, and future PR operations.
- This document describes process mechanics only; it does not create branches or PRs.
