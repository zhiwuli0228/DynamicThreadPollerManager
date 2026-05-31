# V1 Testing and Acceptance Strategy

## 1. Test Layers

| Layer | Focus |
|---|---|
| Unit tests | Invariant checks, validation helpers, change semantics, error mapping. |
| Application tests | Use-case orchestration, registry updates, workload run coordination. |
| API integration tests | HTTP contract, status codes, request/response payloads, `ProblemDetail` behavior. |
| Context startup tests | Spring Boot wiring, bean availability, property binding, actuator exposure. |
| Deterministic concurrency tests | Atomic update behavior, snapshot consistency, bounded workload execution. |

## 2. Acceptance Criteria Mapping

| Success Criterion | Automatic Verification |
|---|---|
| Application starts | Context startup test and Maven test suite pass. |
| Executor CRUD works | API and application tests cover list/register/get/update/remove. |
| Runtime changes take effect | Unit and integration tests observe changed snapshots and metrics. |
| Controlled workloads run | Application tests execute bounded workload scenarios. |
| Invalid inputs are rejected | Validation and negative-path tests assert stable error responses. |
| Deterministic concurrency behavior | Concurrency tests use latches and bounded awaits, not long sleeps. |
| No excluded capability leaks in | Scope checks confirm no Redis, Kafka, DB, frontend, auth, or virtual-thread implementation. |

## 3. Concurrency Testing Rules

- Prefer deterministic coordination primitives such as latches or barriers.
- Use short, bounded timeouts.
- Do not rely on arbitrary long sleeps to prove correctness.
- Make the expected sequence observable in the test assertions.

## 4. Validation Commands

The future implementation mission should run at minimum:

```powershell
.\mvnw.cmd test
openspec.cmd validate --all --json
openspec.cmd schema validate superspec
```

Depending on the change, the mission may also run targeted test classes before the full suite.

## 5. Auto-Fix Policy

Within the active V1 autonomous implementation mission:

- Claude Code may fix compile errors, test failures, and design inconsistencies without waiting for human approval.
- Claude Code should rerun the affected validation commands after each fix.
- Claude Code should continue to the next authorized change only after the current change is verified and pushed.

## 6. Blocked Conditions

The mission must stop only for real external or scope barriers:

- missing login or push permissions,
- unsafe or destructive action not authorized,
- branch conflict that cannot be resolved safely,
- toolchain or plugin absence that cannot be remedied within the mission permissions,
- validation failure that cannot be repaired without leaving the approved V1 scope,
- any attempt to widen scope beyond the V1 exclusions.
