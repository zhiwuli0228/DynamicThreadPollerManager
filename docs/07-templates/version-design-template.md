# Version Design Template

## Header

- Version name:
- Authoring date:
- Status:
- Authoritative branch:

## Sections

### 1. Background

#### 1.1 Capability baseline
Summarize what previous versions have already delivered. Use a table:

| Version | Capability | Status |
|---|---|---|
| vX.Y.Z | ... | IMPLEMENTED |

#### 1.2 Current gap
Describe what is still missing and why it matters now.

#### 1.3 JDK API feasibility assessment (REQUIRED)

Before designing any new dynamic configuration dimension that touches `ThreadPoolExecutor`, answer these questions:

| Question | Answer |
|---|---|
| Does TPE provide a public mutator for this property? | yes/no |
| If yes, is the mutator thread-safe? | yes/no/unknown |
| If yes, does the mutator require executor rebuild? | yes/no |
| If no, what is the minimum decommission/commission strategy needed? | ... |

**Rule**: If TPE provides a thread-safe public mutator for the target property, the design must default to direct delegation (no executor rebuild). Only deviate from this default when the mutator has documented thread-safety limitations or side effects that conflict with the system's concurrency model.

Examples:
- `setCorePoolSize` / `setMaximumPoolSize` — public, thread-safe, no rebuild needed (v0.7.0)
- `setRejectedExecutionHandler` — public, thread-safe (`volatile` field), no rebuild needed (v0.10.0)
- Queue capacity — no public mutator exists; requires `ExecutorRebuildStrategy` decommission→commission (v0.9.0)

#### 1.4 Why now
Justify why this version should be implemented now rather than deferred.

### 2. Objectives

Numbered list of concrete, verifiable goals for this version.

### 3. In Scope

Bulleted list of specific deliverables. Be precise about what components, classes, and behaviors are included.

### 4. Out of Scope

Bulleted list of explicitly excluded items. Reference deferred decision log entries where applicable.

### 5. Architecture Alignment

| Architecture document | How this version addresses it |
|---|---|

### 6. Module Boundaries

| Module | Change type | Description |
|---|---|---|

Include a dependency direction diagram:

```text
module.a (new component X, new component Y)
    ├── module.b (new component Z)
    └── module.c (modified component W)
```

### 7. Core Technical Design

#### 7.1 Key technical approach

Describe the core mechanism. Include pseudocode where helpful.

#### 7.2 ManagedExecutor extension pattern

When adding a new property to `ManagedExecutor`, apply this decision rule:

| TPE provides public getter? | Action | Example |
|---|---|---|
| Yes | Delete cache field; getter delegates to `executor.getXxx()` | `getRejectionPolicy()` → `executor.getRejectedExecutionHandler()` |
| No | Keep cache field; initialize in constructor | `getQueueCapacity()` → `this.queueCapacity` (TPE has no `getQueue()` getter) |

**Rationale**: Caching a value that TPE already provides adds consistency risk (setter must update cache) with no benefit. Only cache when TPE cannot provide the value on demand.

#### 7.3 Surrounding infrastructure

Describe how existing components (SafetyGate, AdjustmentAdapter, EvidenceRecorder, ScenarioRunner) are reused or extended.

### 8. Success Criteria (Draft)

- Verifiable, testable outcomes
- Include regression constraint (existing N tests must continue to pass)

### 9. Candidate Change Decomposition

Draft decomposition — final count confirmed during IR/SR:

| # | Change name | Scope | Dependencies |
|---|---|---|---|

### 10. Current Phase Exit

Checklist of artifacts that must be complete before entering the next phase (IR).
