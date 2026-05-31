# Project Constitution

## 1. Project Identity

DynamicThreadPollerManager is a Java 21 + Spring Boot exploratory demo / laboratory project. The governance and acceptance baseline branch is `claude_master`.

## 2. Mission

The project exists to explore and validate dynamic thread-pool management, including managed executor registration, runtime parameter adjustment, dynamic scheduling changes, execution observability, simulated workload experiments, and later experiments for multi-node uniqueness, recovery strategy, and virtual thread mode evaluation.

## 3. Problem Origin

The project is driven by recurring engineering questions: schedules can change at runtime, obsolete scheduling chains may linger after updates, stalled tasks require detection and rebuild, multiple nodes may compete to execute the same job, and concurrent remote I/O workloads need predictable resource control and observability. These are exploration inputs, not claims of current implementation.

## 4. Project Nature

This is a verification-oriented demo, not a production platform. The goal is to keep the code runnable, testable, and evolvable while using the project as a benchmark for structure and AI-assisted delivery practice.

## 5. Quality Objectives

- Traceable: design, implementation, and verification remain attributable to a specific change.
- Bounded: each change has a clear and limited scope.
- Testable: concurrent behavior and configuration updates can be validated.
- Observable: important runtime state can be inspected through evidence.
- Evolvable: the project can move from single-node experiments toward coordination and recovery experiments.
- Agent-safe: AI implementation work cannot expand scope silently after context compression.

## 6. Explicit Non-Goals

- Production-grade authentication in the current phase.
- A full management frontend.
- Redis, Kafka, or database introduction without explicit approved change scope.
- Premature heavyweight release pipelines or organization-wide gates.
- Treating the demo as a production component.

## 7. Governance Principles

- Stable rules live in Harness.
- Living system design lives in Architecture.
- Bounded feature decisions live in OpenSpec/SuperSpec change artifacts.
- Codex designs; Claude Code implements approved changes.
- No unapproved scope expansion.

## 8. Current Status Declaration

The project is in a governance asset enhancement phase. OpenSpec/SuperSpec tooling is initialized. Dynamic thread-pool business capabilities are not implemented yet. Architecture documentation will be established in the next phase.
