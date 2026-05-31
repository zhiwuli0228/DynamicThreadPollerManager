# Project Harness

## Project Purpose

DynamicThreadPollerManager is a Java 21 + Spring Boot exploratory demo for validating dynamic thread-pool management. The project exists to test runtime registration and adjustment of managed executors, execution metrics, simulated load, and later distributed recovery strategies.

## Delivery Boundary

- Start with single-node in-memory behavior only.
- Add Redis, Kafka, multi-node coordination, frontend, and authentication only through later explicit changes.
- Keep the demo lightweight; do not add production-grade governance too early.

## Architecture Rules

- `api -> application -> domain`.
- `infrastructure` provides concrete implementations for domain needs.
- Domain code must not depend on Web DTOs, Redis/Kafka clients, or Spring MVC details.
- Do not expand scope or stack across changes without approval.

## Engineering Rules

- Java 21 and Maven.
- JUnit 5 and Mockito only; no PowerMock.
- Every functional change must include tests.
- Avoid unrelated refactoring.
- Concurrency state transitions and thread-pool configuration validation must be testable.

## AI Collaboration Model

- Codex and ChatGPT handle requirements analysis, design, OpenSpec/SuperSpec artifacts, and scope review.
- Claude Code implements approved tasks, tests, verification, and commits.
- Claude Code must not expand beyond the approved change.

## Roadmap

1. Local managed executor registry.
2. Runtime metrics and workload simulation.
3. Dynamic scheduled task reconfiguration.
4. Stalled task detection and recovery.
5. Distributed coordination experiment.
