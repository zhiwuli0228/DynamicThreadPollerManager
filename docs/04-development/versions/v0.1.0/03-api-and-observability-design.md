# v0.1.0 API and Observability Design

## Header

- Version name: `v0.1.0`
- Document purpose: define the minimal surfaces needed to run and inspect experiments
- Status: `DRAFT`

## 1. API principles

- Keep the control surface small.
- Prefer read-mostly inspection endpoints.
- Separate strategy selection from executor mutation.
- Record reasons for decisions, not just the result values.

## 2. Control surfaces

The first version should support these internal actions:

- start an experiment run
- stop an experiment run
- select the active policy
- read the current pressure snapshot
- propose a target executor state
- apply a target executor state
- query recent decisions and adjustment events

## 3. Observability surfaces

The minimum observability set should include:

- scenario identifier
- policy identifier
- time series of pressure snapshots
- time series of control decisions
- time series of adjustment events
- final run summary

## 4. Metrics set

The first version should track:

- queue depth
- queue wait latency
- task execution latency
- active thread count
- current core size
- current queue capacity
- rejection count or rate
- heap usage ratio
- GC pause time
- CPU utilization when available

## 5. Output formats

Preferred output formats:

- JSON for structured runtime records
- CSV for summary comparison
- append-only event logs for replay

## 6. Inspection priority

The project should make it easy to answer:

- what happened;
- why it happened;
- what the policy saw;
- what it decided;
- what the executor actually applied.
