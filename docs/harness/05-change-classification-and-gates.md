# Change Classification and Gates

## 1. Why Change Classification Exists

The project needs change classification so small governance work stays lightweight while core concurrency capabilities still receive proper design and review.

## 2. Change Types

| Type | Examples | Required Flow |
|---|---|---|
| Bootstrap / Governance Change | Harness, architecture entry points, toolchain alignment | Bounded docs edit -> validation -> commit/push -> remote review |
| Capability Change | Thread-pool registry, dynamic scheduling, recovery, distributed coordination | SuperSpec design -> review -> implementation -> verify -> remote review |
| Minor Correction | Spelling, links, or non-behavioral document cleanup | Direct bounded edit -> targeted validation -> commit |
| Emergency Correction | A blocking defect in later demo code | Minimal fix scope first, then determine whether a change is needed |

## 3. Mandatory Gates by Type

- Capability changes require design approval before implementation.
- If implementation requires an unapproved dependency, architecture widening, or capability expansion, Claude Code must stop and request a design revision.
- Redis, Kafka, database, frontend, authentication, and virtual-thread mode introduction are capability changes the first time they appear.
- Any change to concurrency semantics is a capability change.

## 4. Scope Escalation Rules

If a task starts as documentation cleanup but reveals real architectural or behavioral impact, it must be escalated rather than silently expanded.

## 5. Evidence Requirements

Every meaningful change should leave a validation trail and a commit trail. The project should not rely on memory to decide what is finished.

## 6. Current Planned Capability Changes

`establish-springboot-technical-foundation`
`establish-local-managed-executor-registry`
`expose-executor-runtime-metrics-and-workloads`
`support-dynamic-scheduled-task-reconfiguration`
`detect-and-rebuild-stalled-scheduling-chain`
`coordinate-single-execution-across-nodes`
`evaluate-virtual-thread-execution-mode`

Only one change should be the main design/implementation focus at a time.
