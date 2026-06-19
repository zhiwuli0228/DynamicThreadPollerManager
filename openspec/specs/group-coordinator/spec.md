# Group Coordinator

## Overview
Centralized coordination interceptor: evaluates scale-up commands against shared budget, resolves conflicts by priority, returns approved/rejected/capped decisions.

## ADDED Requirements

### Requirement: Scale-Down and No-Op Fast Path
GroupCoordinator SHALL approve scale-down and no-op commands immediately without budget check.

#### Scenario: Scale-down approved immediately
- GIVEN a command with targetPoolSize < currentPoolSize
- WHEN coordinate(command, executorName) is called
- THEN outcome is APPROVED_AS_IS
- AND budget releases the delta

#### Scenario: No-op passes through
- GIVEN a no-op command (currentPoolSize == targetPoolSize)
- WHEN coordinate(command, executorName) is called
- THEN outcome is APPROVED_AS_IS

### Requirement: Budget-Based Approval
GroupCoordinator SHALL approve scale-up when budget is sufficient.

#### Scenario: Scale-up approved when budget available
- GIVEN budget has 5 available threads, command requests delta=3
- WHEN coordinate(command, executorName) is called
- THEN outcome is APPROVED_AS_IS
- AND budget reserves 3 threads

### Requirement: Rejection When Budget Exhausted
GroupCoordinator SHALL reject scale-up when budget insufficient and no preemptible executors exist.

#### Scenario: Rejection when no budget
- GIVEN budget has 0 available, no lower-priority executors
- WHEN coordinate(command, executorName) with delta=3 is called
- THEN outcome is REJECTED

### Requirement: Priority-Based Preemption
GroupCoordinator SHALL preempt lower-priority executors to satisfy higher-priority requests.
GroupCoordinator SHALL apply preemption directly through the preempted executor's adapter.

#### Scenario: CRITICAL preempts LOW
- GIVEN exec-A (CRITICAL) requests delta=3, exec-B (LOW) has 5 allocated
- WHEN budget has 0 available
- THEN coordinate() for exec-A preempts 3 from exec-B
- AND outcome is MODIFIED
- AND exec-B's adapter receives a scale-down command

#### Scenario: Partial preemption yields CAPPED
- GIVEN exec-A (HIGH) requests delta=5, exec-B (LOW) has only 3 allocated
- WHEN budget has 0 available
- THEN coordinate() collects 3 from exec-B
- AND outcome is CAPPED with capped command of delta=3

#### Scenario: Equal priority uses first-come-first-served
- GIVEN exec-A and exec-B both NORMAL priority, both request delta=3, budget has 3 available
- WHEN both coordinate concurrently
- THEN first caller gets APPROVED_AS_IS, second gets REJECTED

### Requirement: Cross-Executor Oscillation Detection (Advisory)
GroupCoordinator SHALL run cross-oscillation check for every non-no-op coordination.

#### Scenario: Cross-oscillation flag set
- GIVEN history shows lockstep counter-adjustment pattern
- WHEN coordinate() is called
- THEN result.crossOscillationDetected is true
- AND adjustment is NOT blocked (advisory only)
