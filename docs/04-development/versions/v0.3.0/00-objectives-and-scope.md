# v0.3.0 Objectives and Scope

## Header

- Version name: `v0.3.0`
- Status: `IMPLEMENTED`
- Change candidate: `adaptive-policy-and-control-gate`

## 1. Purpose

The project can now execute deterministic baseline scenarios and record pressure snapshots. The next missing capability is a policy layer that can inspect pressure evidence and produce a reasoned scaling recommendation without applying it.

`v0.3.0` is therefore limited to policy evaluation and safety gating.

## 2. Objectives

- Define a policy input model that references a run and a pressure snapshot.
- Define a policy configuration model for threshold-based decisions.
- Evaluate pressure snapshots into candidate pool-size recommendations.
- Apply safety gates before producing an accepted decision.
- Produce explicit hold/reject decisions when gates block a change.
- Keep policy deterministic for the same inputs.
- Keep policy code independent from scenario generation and executor mutation.

## 3. In Scope

- Policy package under the experiment boundary.
- Threshold policy configuration.
- Policy evaluator interface.
- Baseline adaptive policy implementation.
- Safety gate evaluator.
- Decision result object if the existing `ScaleDecision` is too narrow.
- Tests for deterministic decisions, gate behavior, and boundary isolation.

## 4. Out of Scope

- Applying decisions to an executor.
- Queue capacity mutation.
- Scenario runner changes.
- New workload profiles.
- External metrics registry.
- Persistence.
- UI, REST API, CLI.
- New dependencies.

## 5. Success Criteria

- Given high pressure evidence, the policy proposes a bounded scale-up decision.
- Given low pressure evidence, the policy proposes a bounded scale-down or hold decision according to configuration.
- Given unsafe inputs, gates reject or hold with an explicit reason.
- The same input always produces the same decision result.
- The policy package has no dependency on scenario runner or executor mutation types.
- OpenSpec validation and Maven tests pass after implementation.
