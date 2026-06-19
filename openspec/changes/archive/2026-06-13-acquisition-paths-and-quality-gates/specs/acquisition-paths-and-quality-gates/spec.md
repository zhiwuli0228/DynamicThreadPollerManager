## Purpose

Define the behavior of versioned acquisition report paths, extended data quality gates (G7-G9) for real ThreadPoolExecutor data, and the runner-to-report bridge.

## Requirements

### Requirement: AcquisitionReportPaths versioned factory
The system MUST provide a `forVersion(String)` static factory that produces version-tagged output directory configuration, while retaining backward-compatible defaults.

#### Scenario: forVersion produces correct output directory
- **WHEN** `AcquisitionReportPaths.forVersion("v0.8.0")` is called
- **THEN** `outputDirectory()` MUST return `"outputs/reports/v0.8.0"`

#### Scenario: forVersion rejects path traversal
- **WHEN** `forVersion()` is called with "../escape"
- **THEN** an `IllegalArgumentException` MUST be thrown

#### Scenario: forVersion rejects path separators
- **WHEN** `forVersion()` is called with "a/b"
- **THEN** an `IllegalArgumentException` MUST be thrown

#### Scenario: Backward compatible defaults preserved
- **WHEN** existing code references `AcquisitionReportPaths.OUTPUT_DIRECTORY`
- **THEN** the value MUST still be `"outputs/reports/v0.6.0"`

#### Scenario: reportDirectory resolves versioned path
- **WHEN** `reportDirectory(Path.of("/data"))` is called on a paths instance for "v0.8.0"
- **THEN** the result MUST be `/data/outputs/reports/v0.8.0`

---

### Requirement: RunSnapshot extended fields for TPE quality assessment
The system MUST extend `RunSnapshot` with nullable fields for extended field presence tracking and thread leak status.

#### Scenario: extendedFieldPresence defaults to empty map
- **WHEN** a `RunSnapshot` is created without setting `extendedFieldPresence`
- **THEN** `extendedFieldPresence()` MUST return an empty Map

#### Scenario: threadLeakFree defaults to null
- **WHEN** a `RunSnapshot` is created without setting `threadLeakFree`
- **THEN** `threadLeakFree()` MUST return null

#### Scenario: Extended fields are settable
- **WHEN** a `RunSnapshot` is created with extendedFieldPresence=Map.of("poolSize", true) and threadLeakFree=true
- **THEN** the getters MUST return those values

---

### Requirement: G7 gate validates extended field presence
The system MUST check that all required extended ThreadPoolExecutor fields are present (non-null) in each run's snapshots.

#### Scenario: G7 passes when all required fields present
- **WHEN** a run has extendedFieldPresence with poolSize=true, completedTaskCount=true, keepAliveTimeSeconds=true, largestPoolSize=true, taskCount=true
- **THEN** G7 MUST be in the passed set

#### Scenario: G7 fails when a required field is missing
- **WHEN** a run has extendedFieldPresence with poolSize=false
- **THEN** G7 MUST be in the failed set with a blocking message containing the runId

#### Scenario: G7 skipped for pre-v0.8.0 data
- **WHEN** a run has an empty extendedFieldPresence
- **THEN** G7 MUST NOT appear in either passed or failed sets

---

### Requirement: G8 gate validates per-profile queue pressure evidence
The system MUST check that each run shows queue pressure consistent with its scenario profile.

#### Scenario: G8 STEADY exempt from queue pressure requirement
- **WHEN** a STEADY run has 0 snapshots with queueSize > 0
- **THEN** G8 MUST pass (STEADY is exempt)

#### Scenario: G8 RAMP requires at least 1 queue pressure snapshot
- **WHEN** a RAMP run has at least 1 snapshot with queueSize > 0
- **THEN** G8 MUST pass

#### Scenario: G8 RAMP fails with zero queue pressure
- **WHEN** a RAMP run has 0 snapshots with queueSize > 0
- **THEN** G8 MUST fail with a blocking message

#### Scenario: G8 BURST requires at least 2 queue pressure snapshots
- **WHEN** a BURST run has at least 2 snapshots with queueSize > 0
- **THEN** G8 MUST pass

#### Scenario: G8 BURST fails with insufficient queue pressure
- **WHEN** a BURST run has only 1 snapshot with queueSize > 0
- **THEN** G8 MUST fail with a blocking message

---

### Requirement: G9 gate validates no thread leaks
The system MUST check that runner cleanup did not leave leaked threads.

#### Scenario: G9 passes when thread leak free
- **WHEN** a run has threadLeakFree=true
- **THEN** G9 MUST be in the passed set

#### Scenario: G9 fails when thread leak detected
- **WHEN** a run has threadLeakFree=false
- **THEN** G9 MUST be in the failed set with a blocking message

#### Scenario: G9 skipped when not checked
- **WHEN** a run has threadLeakFree=null
- **THEN** G9 MUST NOT appear in either passed or failed sets

---

### Requirement: AcquisitionReportBridge produces versioned report artifacts
The system MUST provide a bridge that aggregates runner output into versioned acquisition report artifacts.

#### Scenario: Bridge produces 4 artifacts for a valid run
- **WHEN** `bridge()` is called with a valid run outcome, scenario definition, config, and snapshots
- **THEN** 4 JSON files (runManifest, pressureSummary, replaySummary, evidenceIndex) MUST be written to the versioned output directory

#### Scenario: Bridge does not produce ReadinessSummary
- **WHEN** `bridge()` is called in acquisition-only mode
- **THEN** NO `readinessSummary` file MUST be written

#### Scenario: Bridge RunManifest contains correct metadata
- **WHEN** `bridge()` is called
- **THEN** the written `runManifest` JSON MUST contain the correct runId, scenarioId, profile, seed, and baselinePreset

#### Scenario: Bridge PressureSummary aggregates correctly
- **WHEN** `bridge()` is called with snapshots where queueSize values are [0, 2, 5]
- **THEN** the `pressureSummary` JSON MUST show maxQueue=5, meanQueue=2.33...
