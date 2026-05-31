# v0.1.0 Testing and Acceptance Design

## Header

- Version name: `v0.1.0`
- Document purpose: define how the design will be validated
- Status: `DRAFT`

## 1. Testing strategy

The first version should be validated through scenario replay and strategy comparison.

Testing should focus on:

- repeatability
- boundary safety
- oscillation resistance
- metric completeness
- comparability between strategies

## 2. Scenario matrix

Required scenarios:

- steady low load
- step increase and decrease
- periodic tide load
- short burst load
- sustained high load

Optional later scenarios:

- mixed latency distribution
- burst plus recovery
- multi-phase traffic with idle gaps

## 3. Acceptance criteria

The version is acceptable when:

- the same scenario can be replayed with comparable output structure;
- at least one fixed baseline and one adaptive policy can be compared;
- control decisions are recorded together with the snapshots that triggered them;
- adjustment events are bounded and explainable;
- results can be summarized after the run without manual reconstruction.

## 4. Risks to test

- small-sample noise causing false scale-up
- scale oscillation under tide-like traffic
- queue adjustment behaving differently from thread adjustment
- unsafe behavior under heap or GC pressure
- missing data causing incomplete analysis
