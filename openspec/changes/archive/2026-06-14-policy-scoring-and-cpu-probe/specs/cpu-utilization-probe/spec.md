# cpu-utilization-probe

## ADDED Requirements

### Requirement: SystemCpuProbe SHALL read real CPU utilization via JDK ManagementFactory

The `SystemCpuProbe` class MUST provide `sampleProcessCpuLoad()` returning process-level CPU utilization [0.0-1.0] via `com.sun.management.OperatingSystemMXBean`, and `sampleSystemCpuLoad()` returning system load average. Both methods MUST gracefully degrade to 0.0 when the underlying API is unavailable.

#### Scenario: sampleProcessCpuLoad returns non-negative value
- **GIVEN** a SystemCpuProbe instance on a Sun JDK / OpenJDK
- **WHEN** `probe.sampleProcessCpuLoad()` is called
- **THEN** the result is >= 0.0 (may be 0.0 on first call if not yet initialized)

#### Scenario: sampleSystemCpuLoad does not throw
- **GIVEN** a SystemCpuProbe instance
- **WHEN** `probe.sampleSystemCpuLoad()` is called
- **THEN** no exception is thrown; result is >= -1.0 (may be -1.0 if unavailable)

#### Scenario: No external dependencies required
- **WHEN** SystemCpuProbe is compiled
- **THEN** no new Maven dependencies are added beyond JDK standard library

### Requirement: RuntimeObservation SHALL integrate CPU probe via overloaded fromExecutor

`RuntimeObservation.fromExecutor()` MUST provide a 3-argument overload accepting `SystemCpuProbe`. The original 2-argument method MUST delegate internally. CPU read failure MUST result in `MetricValue.absent()`.

#### Scenario: fromExecutor with probe provides real cpuUtilization
- **GIVEN** a mock SystemCpuProbe returning 0.75 from sampleProcessCpuLoad()
- **WHEN** `RuntimeObservation.fromExecutor(executor, now, mockProbe)` is called
- **THEN** the returned RuntimeObservation has `cpuUtilization().isPresent() == true` with value 0.75

#### Scenario: fromExecutor with null probe uses absent
- **GIVEN** a null SystemCpuProbe
- **WHEN** `RuntimeObservation.fromExecutor(executor, now, null)` is called
- **THEN** the returned RuntimeObservation has `cpuUtilization().isPresent() == false`

#### Scenario: Original 2-arg signature unchanged and backward-compatible
- **GIVEN** the existing 2-arg `fromExecutor(ManagedExecutor, Instant)` signature
- **WHEN** the method is called
- **THEN** it compiles and runs successfully; existing callers are unaffected
