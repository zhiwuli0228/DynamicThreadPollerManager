# Resource Budget

## Overview
Thread-safe shared resource tracking with atomic reserve/release and invariant enforcement.

## ADDED Requirements

### Requirement: Budget Reserve and Release
ResourceBudget SHALL atomically reserve and release thread allocations.
ResourceBudget SHALL enforce that total allocated <= maxTotalThreads at all times.
ResourceBudget SHALL support negative delta on reserve() for release.

#### Scenario: Reserve reduces available threads
- GIVEN a budget with maxTotalThreads=10, no allocations
- WHEN reserve("exec-A", 3) is called
- THEN availableThreads() returns 7
- AND allocatedThreads("exec-A") returns 3

#### Scenario: Reserve rejects over-allocation
- GIVEN a budget with maxTotalThreads=10, allocated 8
- WHEN reserve("exec-B", 5) is called
- THEN IllegalStateException is thrown

#### Scenario: Release increases available threads
- GIVEN a budget with allocated 6 for "exec-A"
- WHEN release("exec-A", 2) is called
- THEN allocatedThreads("exec-A") returns 4
- AND availableThreads() increases by 2

#### Scenario: Concurrent reserve/release maintains invariants
- GIVEN a budget with maxTotalThreads=100
- WHEN 10 threads each perform 100 reserve/release operations concurrently
- THEN availableThreads() is always >= 0
- AND totalAllocatedThreads() <= 100

#### Scenario: Snapshot preserves state
- GIVEN a budget with allocations
- WHEN snapshot() is called
- THEN returned budget has same allocations as original
- AND modifying snapshot does not affect original
