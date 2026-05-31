# v0.1.0 Decision Log

## Header

- Version name: `v0.1.0`
- Purpose: record the main design decisions made during exploration
- Status: `DRAFT`

## Decisions

### 1. Research-first framing

Decision: define v0.1.0 as a research experiment platform rather than a production thread-pool manager.

Reason: the current goal is to validate control capability, not to ship a production service.

### 2. Two-layer control model

Decision: keep the budget layer and control layer idea from the older design.

Reason: it gives the experiment a stable boundary while still allowing online adaptation.

### 3. Queue as a first-class resource

Decision: treat queue capacity as a controllable dimension alongside thread count.

Reason: thread scaling alone is not enough to study the full tradeoff.

### 4. Event-based observability

Decision: make structured run records the primary output instead of a UI-first design.

Reason: experiments need replayable evidence, not just visual inspection.

### 5. Safety gates remain in the experiment design

Decision: keep cooldown, deadband, continuous-window triggering, and step limits.

Reason: unsafe experiments are not useful experiments.

## Open questions

- Which pressure signal should become the default control signal?
- Should the first implementation prioritize core scaling or queue scaling?
- What scenario set best exposes oscillation risk?
- How much runtime observability should be exposed through HTTP, if any?
