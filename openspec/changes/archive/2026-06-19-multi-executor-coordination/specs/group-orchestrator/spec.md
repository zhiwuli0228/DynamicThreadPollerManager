# Group Orchestrator

## Overview
Manages lifecycle of multiple AdjustmentLoop instances as a coordinated group with aggregated health status.

## ADDED Requirements

### Requirement: Start All Loops
GroupLoopOrchestrator SHALL create and start AdjustmentLoop instances for all executors in the group.

#### Scenario: Start all loops
- GIVEN a group with 2 executors and their LoopComponents
- WHEN startAll() is called
- THEN 2 AdjustmentLoop instances are created
- AND both are started (state = RUNNING)
- AND each loop uses CoordinatedAdjustmentAdapter
- AND returned map contains 2 LoopSessions

### Requirement: Lifecycle Operations
GroupLoopOrchestrator SHALL support pauseAll, resumeAll, stopAll, emergencyStopAll.

#### Scenario: Pause and resume all
- GIVEN 2 running loops
- WHEN pauseAll() then resumeAll() is called
- THEN both loops transition: RUNNING → PAUSED → RUNNING

#### Scenario: Stop all
- GIVEN 2 running loops
- WHEN stopAll() is called
- THEN both loops are STOPPED
- AND returned sessions have endTime present

#### Scenario: Emergency stop all
- GIVEN 2 running loops
- WHEN emergencyStopAll("cross-oscillation") is called
- THEN both loops are EMERGENCY_STOPPED

### Requirement: Group Health
GroupLoopOrchestrator SHALL provide aggregated GroupHealth with loop states and budget snapshot.

#### Scenario: Group health reflects current state
- GIVEN 3 executors: 2 running, 1 paused, budget 70% utilized
- WHEN getGroupHealth() is called
- THEN runningLoops=2, pausedLoops=1, stoppedLoops=0
- AND budgetSnapshot reflects current allocations
- AND activeWarnings is empty (below 90% threshold)

#### Scenario: Budget utilization warning
- GIVEN budget is 95% utilized
- WHEN getGroupHealth() is called
- THEN activeWarnings contains "budget >= 90%" message
