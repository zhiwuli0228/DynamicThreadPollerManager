# AI Delivery Workflow

## 1. Roles

| Role | Responsibility | Prohibited by Default |
|---|---|---|
| ChatGPT | Overall design, remote-state review, next-task framing, change/result review | Claiming repository state without checking it |
| Codex | Project governance documents, OpenSpec/SuperSpec design artifacts, architecture and scope decisions | Defaulting to business implementation |
| Claude Code | Implement approved work, run tests, verify, commit, and push | Expanding scope or changing boundaries unilaterally |
| User | Provides task files, approves the next phase, and handles external permissions when needed | Manually re-copying facts the remote repository can already provide |

## 2. Authoritative Inputs

The minimum trustworthy inputs are the harness documents, current approved change artifacts, and the live GitHub branch state. Roadmaps and notes are guidance, not implementation proof.

## 3. Delivery Workflow

Harness / Architecture baseline -> Codex creates bounded SuperSpec design artifacts for the next approved change or framework decision -> ChatGPT reviews pushed design artifacts from GitHub -> Claude Code implements approved change -> Claude Code verifies and pushes evidence -> ChatGPT reviews remote implementation and authorizes the next change.

## 4. Context Minimization Rules

- Implementation sessions should read only the harness and active change documents needed for the task.
- `openspec/config.yaml` should remain a compressed summary of high-leverage rules.
- One change should map to one capability.
- Long-term rules should not be copied into every change.
- The remote GitHub branch is the review source of truth; the current governance baseline is `claude_master`.
- First-version capability selection is deferred until the framework baseline is complete.

## 5. Remote Review and Git Rules

- Reviews should be based on pushed artifacts and commits.
- Push is part of the evidence trail, not an optional afterthought.
- If push cannot be completed non-interactively because of authentication or permissions, the workflow may stop.

## 6. Blocked Conditions

Blocking is acceptable only when one of the following is true:

- account login or authorization is missing,
- push permissions cannot be completed non-interactively,
- a branch conflict prevents safe fast-forward,
- an essential tool is truly unavailable and cannot be installed within the task permissions,
- approved design artifacts and actual implementation constraints conflict in a way that cannot be resolved without revising the design.

## 7. Anti-Drift Rules

- Do not promote a roadmap item into “implemented” status without evidence.
- Do not let the implementation session become a hidden architecture rewrite.
- Do not use a later phase to justify skipping the current phase's governance work.
