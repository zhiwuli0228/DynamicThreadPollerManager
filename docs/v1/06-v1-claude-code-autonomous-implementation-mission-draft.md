# V1 Claude Code Autonomous Implementation Mission Draft

## 1. Mission Objective

Implement the V1 dynamic executor management experiment as a continuous autonomous mission.

The mission is to create, apply, verify, commit, push, and close out the authorized V1 changes without pausing for per-change human approval. The mission must stop only for documented BLOCKED conditions or scope expansion beyond the V1 package.

## 2. Repository and Branch Strategy

- Repository: `DynamicThreadPollerManager`
- Baseline branch: `claude_master`
- Selected execution branch: `ai/v1-implementation`
- Closeout strategy: `gh`-backed PR or merge path chosen by the mission, with accepted results integrated back toward `claude_master`

## 3. Scope

### In Scope

- Spring Boot Web/API foundation for the management surface,
- bean validation for request and configuration safety,
- Actuator and Micrometer observability,
- in-memory managed executor registry,
- runtime executor update use cases,
- controlled workload scenarios,
- status and metrics feedback for the demo loop,
- tests and verification for all implemented behavior,
- commit, push, and GitHub closeout steps allowed by the mission.

### Explicit Exclusions

- dynamic scheduled task reconfiguration,
- stall detection and recovery,
- Redis, Kafka, database, frontend, authentication, multi-node deployment,
- virtual-thread mode,
- any capability outside the V1 package,
- any unapproved OpenSpec change outside the mission scope.

## 4. Mandatory Reading List

Before any implementation work begins, read:

1. `docs/harness/project-harness.md`
2. `docs/harness/04-ai-delivery-workflow.md`
3. `docs/harness/05-change-classification-and-gates.md`
4. `docs/architecture/README.md`
5. `docs/architecture/06-v1-unified-design-planning-framework.md`
6. `docs/delivery/README.md`
7. `docs/v1/README.md`
8. `docs/v1/00-v1-product-scope-and-success-criteria.md`
9. `docs/v1/01-v1-technical-architecture-decisions.md`
10. `docs/v1/02-v1-domain-capability-design.md`
11. `docs/v1/03-v1-api-observability-and-experiment-design.md`
12. `docs/v1/04-v1-testing-and-acceptance-strategy.md`
13. `docs/v1/05-v1-change-decomposition-and-autonomous-execution-plan.md`

## 5. Authorized Change Set

| Order | Change Name | Purpose | Depends On | Proceed Automatically After Verification |
|---|---|---|---|---|
| 1 | `establish-springboot-management-foundation` | Add the management API, validation, actuator, and shared error model. | None | YES |
| 2 | `establish-local-managed-executor-registry` | Add the in-memory registry, snapshot model, and runtime update behavior. | 1 | YES |
| 3 | `expose-experiment-workloads-and-observability` | Add controlled workload scenarios, metrics, and the demo feedback loop. | 2 | YES |

## 6. Autonomous Execution Loop

For each authorized change:

1. create the required OpenSpec/SuperSpec artifacts for that change,
2. implement the change,
3. run the required tests and validation,
4. fix failures within scope and rerun validation,
5. commit the verified result,
6. push the commit to `origin`,
7. confirm remote state with `gh`,
8. continue immediately to the next authorized change unless BLOCKED.

Do not pause for human approval between authorized changes.

## 7. Allowed Implementation Changes

The mission may modify only the files and dependencies needed to realize the V1 scope:

- Java source under `src/main/**`,
- tests under `src/test/**`,
- `pom.xml` entries required for the approved V1 stack,
- OpenSpec/SuperSpec change artifacts for the authorized V1 changes,
- docs or config files required to keep the delivery evidence aligned.

The mission must not introduce:

- Redis,
- Kafka,
- database persistence,
- frontend or authentication layers,
- multi-node coordination,
- virtual-thread execution mode,
- scheduling reconfiguration,
- or any out-of-scope capability.

## 8. Verification Commands

Run at minimum:

```powershell
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

Additional targeted tests are allowed and encouraged before the full suite.

## 9. Commit and Push Rules

- Commit after each verified authorized change.
- Push every verified commit to the remote branch.
- Do not batch unverified changes into a single push.
- Do not stop after push if another authorized change remains.

## 10. GitHub Usage Rules

- Use `gh` for remote inspection, branch confirmation, PR creation, and merge operations when the selected closeout path requires them.
- Confirm the remote branch SHA before and after push.
- If the selected closeout path is PR-based, use `gh` to create and merge the PR according to repository permissions and mission instructions.

## 11. Auto-Fix and Retry Policy

Within the mission boundary:

- fix compile errors, test failures, and validation drift automatically,
- rerun the affected commands after each fix,
- keep retrying until the change verifies or a real BLOCKED condition appears,
- do not widen the scope to make a failing test disappear.

## 12. BLOCKED Conditions

Return `BLOCKED` only when one of the following is true:

- missing login, auth, or push permissions,
- remote branch conflict that cannot be safely resolved,
- toolchain or required plugin absence that cannot be corrected within the mission permissions,
- a destructive or unsafe external action is required but not authorized,
- validation failure cannot be resolved without leaving the V1 scope,
- the implementation would need to widen beyond the authorized V1 exclusions.

## 13. Final Report Format

Return only the mission summary in this shape:

```text
STATUS: COMPLETED | BLOCKED_...
PHASE: 05-autonomous-delivery-policy-and-v1-unified-design
EXECUTOR: Claude Code
REPOSITORY: DynamicThreadPollerManager
BRANCH: ai/v1-implementation
START_HEAD:
END_HEAD:
PUSHED: YES | NO
GH_REMOTE_CONFIRMED: YES | NO

AUTONOMOUS_EXECUTION:
- per-change human approval blocked by default: NO
- autonomous continue-to-next-change enabled: YES
- BLOCKED conditions preserved: YES
- evidence/test/scope gates preserved: YES

V1_IMPLEMENTATION:
- change 1: ...
- change 2: ...
- change 3: ...

FILES_CHANGED:
- ...

VALIDATION:
- .\mvnw.cmd test: PASS | FAIL
- openspec.cmd validate --all --json: PASS | FAIL
- openspec.cmd schema validate superspec: PASS | FAIL
- git diff scope check: PASS | FAIL

SCOPE_CHECK:
- pom.xml changed: YES | NO
- src/main changed: YES | NO
- src/test changed: YES | NO
- openspec/changes created: YES | NO
- openspec/schemas changed: YES | NO
- .codex/.claude changed: YES | NO
- business implementation added: YES | NO

NEXT_ACTION:
- ...
```
