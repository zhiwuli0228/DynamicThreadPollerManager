# AI Delivery Workflow

## 1. Roles

| Role | Responsibility | Default Stop Condition |
|---|---|---|
| User | Defines the bounded mission, exclusions, and final acceptance goals. | A real external permission barrier or a scope change outside the active mission. |
| ChatGPT | Sets mission boundaries, performs remote review, and improves the design when asked. | Claiming repository state without checking it. |
| Codex | Produces governance, V1 planning, and approved design artifacts within the active mission. | Expanding scope, skipping evidence, or changing long-lived boundaries unilaterally. |
| Claude Code | Implements, tests, verifies, commits, pushes, and performs approved GitHub operations inside an active autonomous mission. | Scope expansion, unsafe conflict, or a genuine external block. |

## 2. Authoritative Inputs

The minimum trustworthy inputs are the harness documents, the Living Architecture, the Delivery Framework, the V1 design package, the current approved change artifacts, and the live GitHub branch state. Roadmaps and notes are guidance, not implementation proof.

## 3. Delivery Workflow

The user authorizes a bounded AI-executed mission by approving its objective, scope, exclusions, and acceptance rules.

Within that mission boundary:

- Codex may complete governance, V1 planning, and other required design artifacts.
- Claude Code may complete implementation, tests, verification, commits, pushes, and approved GitHub operations.
- AI execution does not pause solely for human phase-by-phase approval.
- ChatGPT remote review is an oversight and design-improvement mechanism, not a mandatory blocking step between already-authorized substeps.

Framework / architecture baseline -> authorized mission boundary -> design and change planning -> implementation -> verification -> commit and push -> optional remote review -> next authorized change, until the mission is complete or BLOCKED.

## 4. Context Minimization Rules

- Read only the harness, architecture, delivery, V1, and active change documents needed for the current step.
- `openspec/config.yaml` should remain a compressed summary of the high-leverage rules.
- One change should map to one capability or one tightly coupled capability slice.
- Long-term rules should not be copied into every change artifact.
- The remote GitHub branch is the review source of truth; the current governance baseline is `claude_master`.
- First-version capability selection must be explicit in the V1 design package.

## 5. Remote Review and Git Rules

- Reviews should be based on pushed artifacts and commits.
- Push is part of the evidence trail, not an optional afterthought.
- `gh` may be used for remote inspection, branch verification, PR creation, and merge operations when the active mission allows it.
- If push cannot be completed non-interactively because of authentication or permissions, the workflow may stop.

## 6. Blocked Conditions

Blocking is acceptable only when one of the following is true:

- account login or authorization is missing,
- push permissions cannot be completed non-interactively,
- a branch conflict prevents safe fast-forward or safe merge,
- an essential tool is truly unavailable and cannot be installed within the task permissions,
- approved design artifacts and actual implementation constraints conflict in a way that cannot be resolved without revising the design,
- validation fails and cannot be repaired within the active mission boundary,
- a destructive or otherwise unsafe external action is not authorized.

## 7. Anti-Drift Rules

- Do not promote a roadmap item into `implemented` status without evidence.
- Do not let the implementation session become a hidden architecture rewrite.
- Do not use a later phase to justify skipping the current phase's governance work.
- Do not reintroduce per-change human approval as the default gate when an autonomous mission is active.
