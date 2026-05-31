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

The V1 plan selects a single implementation branch for traceability and low automation cost:

```text
Approved V1 autonomous implementation mission
  -> create `ai/v1-implementation` from `claude_master`
  -> Claude Code produces bounded SuperSpec artifacts in the authorized sequence
  -> Claude Code implements, tests, verifies and commits continuously
  -> Claude Code pushes evidence after each verified change
  -> Claude Code may use `gh` for PR creation, review, and merge if the mission selects that closeout path
  -> accepted results integrate back toward `claude_master`
  -> automation pauses only on documented BLOCKED conditions
```

## Lifecycle Rules

- `claude_master` is the integration and review baseline for framework work.
- The V1 branch naming, PR policy, and merge policy are decided by the V1 mission rather than by phase-by-phase human approval.
- `gh` may be used for remote inspection, branch verification, PR operations, and merge operations when the active mission allows it.
- This document describes process mechanics only; it does not create branches or PRs.
