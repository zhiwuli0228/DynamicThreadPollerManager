# Domain and Experiment Scope

## 1. Domain Problem Statement

The domain concerns managing thread-pool and scheduling capabilities as explicit experimental objects: definition, registration, observation, controlled updates, and later recovery. The project should model runtime behavior without pretending future capabilities already exist.

## 2. Experiment Strategy

The roadmap is staged. First establish the Spring Boot technical foundation that can host API, validation, observability, and test boundaries. Then introduce local managed executor behavior, then metrics and workload simulation, then scheduled task reconfiguration, then stalled-chain recovery, then cross-node coordination, and finally virtual-thread mode evaluation.

## 3. Ubiquitous Language

| Term | Definition | Current/Future |
|---|---|---|
| Managed Executor | A project-registered and project-controlled executor abstraction | Future capability |
| Executor Definition | A static or desired configuration description for an executor | Future capability |
| Runtime Snapshot | A point-in-time view of actual executor state | Future capability |
| Configuration Update | A controlled command that changes allowed runtime parameters | Future capability |
| Managed Scheduled Task | A periodic task whose cadence, status, and execution history are managed | Future capability |
| Schedule Version | A version marker that prevents stale scheduling chains from remaining active | Future capability |
| Execution Record | The result and timing data for one task run | Future capability |
| Stall Detection Policy | The rule set that decides a task is no longer healthy and should be rebuilt | Future capability |
| Coordination Lease | The abstraction for unique execution authority across nodes | Deferred future capability |

## 4. Capability Roadmap

0. `establish-springboot-technical-foundation`
1. `establish-local-managed-executor-registry`
2. `expose-executor-runtime-metrics-and-workloads`
3. `support-dynamic-scheduled-task-reconfiguration`
4. `detect-and-rebuild-stalled-scheduling-chain`
5. `coordinate-single-execution-across-nodes`
6. `evaluate-virtual-thread-execution-mode`

The first step establishes the Web/API, validation, observability, and test-bearing foundation. The second step introduces the first thread-pool capability. Each capability must be handled as an independent change. If a change causes the route to shift, Architecture and Harness must be updated in sync.

## 5. Current Scope

The current scope is limited to governance and bootstrap assets plus future design boundaries. No dynamic thread-pool capability has been implemented yet.

## 6. Deferred Scope

- Coordination lease implementation.
- Redis-backed state.
- Kafka-backed eventing.
- Database persistence.
- Frontend surfaces.
- Authentication and authorization.
- Production-style orchestration.

## 7. Scope Control Rules

- Do not treat future roadmap items as current implementation.
- Do not introduce a new technology or dependency without a specific approved change.
- Keep each change to one capability.
- Revisit scope only through approved design artifacts and review.
