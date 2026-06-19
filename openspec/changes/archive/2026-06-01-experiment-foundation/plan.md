# Experiment Foundation Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development
> to implement this plan task-by-task.

**Goal:** Establish the smallest possible experiment runtime foundation that later changes can reuse for metrics, scenarios, policy, and executor work.

**Architecture:** The first change is intentionally orchestration-first. It introduces a small experiment coordinator plus immutable domain contracts and lifecycle state, while leaving metrics collection, policy evaluation, and executor mutation to later changes. The implementation should stay explicit and low-coupling so the current codebase can absorb it without a rewrite.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Maven

---

## Task 1: Foundation Model

- [ ] **Step 1:** Create the `experiment-foundation` package structure under `src/main/java/com/zhiwu/dynamicthreadpollermanager/`.
- [ ] **Step 2:** Add the immutable value objects for run, scenario, snapshot, policy, decision, event, series, and summary.
- [ ] **Step 3:** Add the lifecycle state enum or value type that represents created, running, stopped, and finalized states.
- [ ] **Step 4:** Add focused unit tests for object construction and state representation.

## Task 2: Runtime Coordination

- [ ] **Step 1:** Implement the experiment coordinator that creates a run from scenario and policy identifiers.
- [ ] **Step 2:** Add start, stop, and finalize operations with explicit lifecycle transitions only.
- [ ] **Step 3:** Add summary assembly from the stored run metadata.
- [ ] **Step 4:** Add tests for run identity, lifecycle transitions, and summary output.

## Task 3: Boundary and Verification

- [ ] **Step 1:** Verify that the foundation package has no dependency on metrics sampling or executor mutation code.
- [ ] **Step 2:** Run the targeted test suite for the new foundation package.
- [ ] **Step 3:** Run the full Maven test suite before closing the change.
- [ ] **Step 4:** Prepare the change for the next phase, which will define metrics and recording behavior.
