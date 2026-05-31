# v0.1.0 Requirements and Use Cases

## Header

- Version name: `v0.1.0`
- Document purpose: describe the minimum behavior and the main research scenarios
- Status: `DRAFT`

## 1. Functional requirements

- The system must generate repeatable load patterns.
- The system must sample JVM and executor pressure at runtime.
- The system must evaluate at least one fixed baseline and one adaptive strategy.
- The system must record each scale decision and each applied adjustment.
- The system must keep scenario, policy, and result data replayable.

## 2. Non-functional requirements

- Reproducibility must be high enough for comparison runs.
- Control actions must be bounded by safety rules.
- Experiment output must be analysis-friendly and not dependent on a UI.
- Strategy behavior must be deterministic for the same inputs.

## 3. Use cases

### 3.1 Baseline comparison

Compare a fixed executor against an adaptive executor under the same traffic shape.

### 3.2 Tide load experiment

Run a periodic ramp-up and ramp-down pattern to observe whether the adaptive policy follows demand without oscillating too much.

### 3.3 Burst recovery experiment

Inject a short high-pressure burst and measure how quickly the executor recovers after the burst ends.

### 3.4 High-pressure safety experiment

Hold the system under sustained pressure and observe whether the control policy avoids unsafe aggressive scaling.

### 3.5 Policy comparison

Run the same scenario with different policies and compare latency, throughput, rejection, and adjustment frequency.
