# Coordinated Adapter

## Overview
Decorator implementing ExecutorAdjustmentAdapter that injects GroupCoordinator coordination before delegation.

## ADDED Requirements

### Requirement: Implements ExecutorAdjustmentAdapter
CoordinatedAdjustmentAdapter SHALL implement ExecutorAdjustmentAdapter without modifying the interface.

#### Scenario: Interface compliance
- GIVEN a CoordinatedAdjustmentAdapter instance
- THEN it is an instanceof ExecutorAdjustmentAdapter
- AND currentState() delegates to wrapped adapter
- AND apply() calls coordinator before delegation

### Requirement: Coordination Rejection Returns REJECTED Result
CoordinatedAdjustmentAdapter SHALL return an AdjustmentResult with status REJECTED when coordinator rejects.

#### Scenario: Rejected adjustment
- GIVEN coordinator returns REJECTED
- WHEN apply(command) is called
- THEN returned AdjustmentResult has status REJECTED
- AND failureCode is COORDINATION_REJECTED

### Requirement: CAPPED Applies Modified Command
CoordinatedAdjustmentAdapter SHALL apply the approved (capped) command when coordinator caps the request.

#### Scenario: Capped adjustment
- GIVEN coordinator returns CAPPED with approvedCommand having target=5 (original target=10)
- WHEN apply(command) is called
- THEN delegate receives approvedCommand with targetPoolSize=5
- AND returned AdjustmentResult reflects the capped outcome

### Requirement: Scale-Down Releases Budget
CoordinatedAdjustmentAdapter SHALL release budget on successful scale-down.

#### Scenario: Scale-down releases budget
- GIVEN a scale-down command (target < current), coordinator approves
- WHEN apply(command) returns APPLIED
- THEN budget.release() is called with the delta
