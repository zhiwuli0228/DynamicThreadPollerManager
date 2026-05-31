# V1 API, Observability, and Experiment Design

## 1. API Surface

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/executors` | List registered executors. |
| `POST` | `/api/v1/executors` | Register a managed executor. |
| `GET` | `/api/v1/executors/{executorId}` | Inspect a single executor snapshot. |
| `PUT` | `/api/v1/executors/{executorId}` | Update allowed runtime parameters. |
| `DELETE` | `/api/v1/executors/{executorId}` | Remove an executor from the registry. |
| `GET` | `/api/v1/workloads/scenarios` | List repeatable workload scenarios. |
| `POST` | `/api/v1/workloads/{scenarioId}/runs` | Run a controlled workload. |
| `GET` | `/api/v1/workloads/{scenarioId}/runs/{runId}` | Inspect the latest workload result. |

## 2. Request and Response Contracts

- Requests use compact JSON DTOs with bean validation annotations.
- Success responses return a snapshot or receipt object that includes the current version.
- Validation failures and domain failures use `ProblemDetail`.
- The API layer must not leak infrastructure types such as executor internals or raw metrics registries.

## 3. Observability Minimum Loop

V1 uses a single observable feedback loop:

1. inspect executor status,
2. run a controlled workload,
3. apply a runtime update,
4. run the workload again,
5. compare status and metrics before and after the change.

### Selected Metrics Examples

- executor registration count,
- executor update count,
- workload run count,
- workload rejection count,
- current active executor count,
- queue pressure or saturation gauge where available.

### Actuator Exposure Strategy

- expose `health`, `info`, and `metrics`,
- optionally expose Prometheus if the implementation chooses that actuator path,
- keep the demo surface minimal and local.

## 4. Experiment Steps

1. start the application,
2. create a named executor,
3. run a bounded workload scenario,
4. observe current metrics and executor snapshot,
5. update the executor runtime parameters,
6. rerun the workload,
7. verify the effect through response data and metrics.

## 5. Dynamic Scheduling Exclusion

Dynamic scheduled task reconfiguration is not part of V1. Therefore:

- no scheduling update endpoint is defined,
- no scheduling reconfiguration metrics are required,
- the experiment design focuses on managed executors only.

## 6. Demo Configuration Strategy

- use a single local Spring Boot application,
- keep actuator exposure limited to the minimal observable endpoints,
- keep request payloads small and explicit,
- avoid any dependency on external systems.
