# Executor Group

## Overview
Group multiple ManagedExecutors under a shared resource budget with construction-time validation.

## ADDED Requirements

### Requirement: Group Construction
ExecutorGroup SHALL accept a config and map of named ManagedExecutors.
ExecutorGroup SHALL validate at construction that sum(member.corePoolSize) <= config.maxTotalThreads.
ExecutorGroup SHALL initialize ResourceBudget with current corePoolSize allocations.

#### Scenario: Valid construction
- GIVEN a group config with maxTotalThreads=10 and two executors (core=3, core=3)
- WHEN ExecutorGroup is constructed
- THEN construction succeeds
- AND budget reserves 6 threads (3 per executor)

#### Scenario: Budget exceeded at construction
- GIVEN a group config with maxTotalThreads=5 and two executors (core=4, core=4)
- WHEN ExecutorGroup is constructed
- THEN IllegalArgumentException is thrown with detail message including executor info

#### Scenario: Empty members rejected
- GIVEN a group config and empty members map
- WHEN ExecutorGroup is constructed
- THEN IllegalArgumentException is thrown

#### Scenario: Queue budget validation
- GIVEN a group config with maxTotalQueueCapacity=20 and two executors (queue=15, queue=10)
- WHEN ExecutorGroup is constructed
- THEN IllegalArgumentException is thrown (25 > 20)
