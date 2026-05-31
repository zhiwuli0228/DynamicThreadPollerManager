# V1 Technical Architecture Decisions

## Decision Table

| Decision | Selected Option | Alternatives Rejected | Rationale | Impact on Implementation |
|---|---|---|---|---|
| Application stack | Spring Boot Web + Validation + Actuator | Full MVC plus frontend, Kafka, or distributed infrastructure | The V1 objective only needs a small management API and observable runtime surface. | Adds REST endpoints, validation annotations, actuator exposure, and runtime metrics. |
| Package map | `api`, `application`, `domain`, `infrastructure`, `support` | Flat package structure or premature module splitting | A layered package map keeps boundaries clear without over-engineering the demo. | Controllers stay thin, domain stays transport-free, infrastructure stays adapter-oriented. |
| API style | REST with JSON request/response and `ProblemDetail` for errors | Custom error envelope or GraphQL | REST is the quickest path to a bounded management surface and standard error behavior. | Endpoints and tests can remain simple and explicit. |
| Configuration strategy | Typed configuration properties plus runtime update commands | Ad-hoc static fields or external config service | V1 must support safe runtime changes without introducing distributed configuration systems. | A properties object and update use case will own the allowed fields. |
| Observability | Actuator health/info/metrics plus custom Micrometer metrics | Separate metrics pipeline or logging-only observation | The demo needs a measurable feedback loop with minimal infrastructure. | Custom counters/gauges will track executor and workload activity. |
| Error handling | Spring `ProblemDetail` for validation and domain errors | Raw `RuntimeException` responses | Standard error semantics keep client behavior predictable and testable. | Validation and domain failures return stable HTTP error responses. |
| Documentation model | V1 design package instead of a new ADR series | Creating a separate ADR for every tactical decision | The design package is sufficient for a first-version benchmark demo. | Reduces overhead while preserving traceability. |
| Persistence | In-memory state only | Database-backed state | V1 proves runtime behavior, not durable storage. | Simplifies the registry and keeps tests deterministic. |
| Scheduling capability | Not included | Add task reconfiguration now | Scheduling reconfiguration is a separate capability slice and would widen scope. | No scheduling endpoints or scheduling-specific domain objects are introduced in V1. |
| Concurrency model | Single-process managed executors with bounded synchronization | Multi-node coordination or distributed locking | V1 should first prove local runtime mutation before distributed behavior. | Concurrency tests stay deterministic and local. |

## Architectural Boundaries

- `api` owns request parsing, response shaping, and HTTP status mapping.
- `application` owns use cases, orchestration, and transactional ordering.
- `domain` owns executor rules, invariants, and result models.
- `infrastructure` owns the concrete executor adapters and metrics wiring.
- `support` owns shared error and utility types.

## Long-Lived Decision Notes

- No new long-lived architecture boundary is introduced beyond the existing layered model.
- No new middleware platform is required for V1.
- No ADR file is required for this phase because the design package documents the tactical first-version decision set directly.
