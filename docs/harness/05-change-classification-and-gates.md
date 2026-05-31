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

- Capability changes require an active authorized mission and an approved V1 scope before implementation.
- If implementation requires an unapproved dependency, architecture widening, or capability expansion, Claude Code must stop and request a design revision.
- Redis, Kafka, database, frontend, authentication, and virtual-thread mode introduction are capability changes the first time they appear.
- Any change to concurrency semantics is a capability change.

## 4. Scope Escalation Rules

If a task starts as documentation cleanup but reveals real architectural or behavioral impact, it must be escalated rather than silently expanded.

## 5. Evidence Requirements

Every meaningful change should leave a validation trail and a commit trail. The project should not rely on memory to decide what is finished.

## 6. Autonomous Execution Boundary and Gates

| Gate | Required Evidence | Stops Automation When |
|---|---|---|
| Scope Gate | The change is within the active mission IN scope and does not touch excluded capabilities. | The change would add excluded middleware, platforms, or roadmap scope. |
| Architecture Gate | Package boundaries, dependency choices, and system behavior remain aligned with the Living Architecture and V1 design. | A long-lived architecture change is needed outside the active mission. |
| Test Gate | Targeted tests, build commands, and failure evidence are recorded. | Validation fails and cannot be repaired within the active mission boundary. |
| Traceability Gate | Branch, commit, push, and remote state are recorded. | The evidence trail cannot be persisted or pushed. |
| External Risk Gate | Authentication, permissions, and destructive-action limits are respected. | A real external authorization barrier or unsafe action is encountered. |

An active autonomous mission may proceed from one authorized change to the next without waiting for per-change human approval, provided the gates above continue to pass.

## 7. V1 Authorized Change Set Reference

The V1 authorized change set is defined in `docs/v1/05-v1-change-decomposition-and-autonomous-execution-plan.md`. This file governs classification and gates; it does not replace the V1 plan.
