# Architecture and Dependency Rules

## 1. Architectural Style

The project uses a layered style: `api -> application -> domain`, with `infrastructure` providing concrete integrations, and approved experiments wiring through application/domain ports as needed.

## 2. Dependency Direction

- `api -> application -> domain`
- `infrastructure -> domain`
- `experiment -> application/domain ports as approved by change design`

## 3. Layer Responsibilities

| Layer | Responsibility | Must Not Do |
|---|---|---|
| `api` | HTTP contract, DTOs, request validation, response mapping | Directly manipulate executor internals |
| `application` | Use-case orchestration, command/query coordination | Own low-level thread-pool state algorithms |
| `domain` | Core model, invariants, ports, and policies | Depend on Spring MVC DTOs, Redis/Kafka clients, or concrete observability adapters |
| `infrastructure` | JDK executor, metrics, and future Redis/Kafka adapters | Redefine domain rules |
| `experiment` | Workload and experiment scenarios | Become a production business entrypoint |

## 4. Forbidden Dependencies

The initial and current benchmark phases must not introduce Redis, Kafka, database, frontend, or authentication as implied defaults. Queue-capacity replacement, cross-node coordination, and other advanced behaviors must not be implemented before their change design is approved.

## 5. Infrastructure Introduction Rules

Infrastructure is allowed only when a change explicitly needs a concrete adapter for an approved capability. The domain defines the rule; infrastructure supplies the mechanism. Infrastructure may not become the place where domain decisions are invented after the fact.

## 6. Concurrency Boundary Rules

Thread-pool state changes, schedule versioning, cancellation, and rebuild decisions must be expressed through domain/application boundaries. They must not be scattered across controllers or hidden in infrastructure-only behavior.

## 7. Architecture Change Governance

Architecture changes require explicit review through approved change artifacts. No agent may silently widen the architecture or add a new layer role because it seems convenient.
