package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import com.zhiwu.dynamicthreadpollermanager.experiment.scenario.ScenarioProfile;

import java.util.List;
import java.util.Objects;

/**
 * Result of running {@link AcquisitionDataQualityValidator}
 * over a candidate acquisition dataset. The result is
 * descriptive: it records which quality gates passed and
 * which failed, but never carries any mutation authorization.
 */
public final class AcquisitionDataQualityResult {

    public enum Status { VALID, INVALID }

    private final String datasetId;
    private final Status status;
    private final List<ScenarioProfile> evaluatedScenarioProfiles;
    private final List<ScenarioProfile> missingScenarioProfiles;
    private final List<String> passedGateCodes;
    private final List<String> failedGateCodes;
    private final List<String> blockingReasons;

    public AcquisitionDataQualityResult(String datasetId,
                                        Status status,
                                        List<ScenarioProfile> evaluatedScenarioProfiles,
                                        List<ScenarioProfile> missingScenarioProfiles,
                                        List<String> passedGateCodes,
                                        List<String> failedGateCodes,
                                        List<String> blockingReasons) {
        this.datasetId = requireNonBlank(datasetId, "datasetId");
        this.status = Objects.requireNonNull(status, "status");
        this.evaluatedScenarioProfiles = List.copyOf(Objects.requireNonNull(
                evaluatedScenarioProfiles, "evaluatedScenarioProfiles"));
        this.missingScenarioProfiles = List.copyOf(Objects.requireNonNull(
                missingScenarioProfiles, "missingScenarioProfiles"));
        this.passedGateCodes = List.copyOf(Objects.requireNonNull(
                passedGateCodes, "passedGateCodes"));
        this.failedGateCodes = List.copyOf(Objects.requireNonNull(
                failedGateCodes, "failedGateCodes"));
        this.blockingReasons = List.copyOf(Objects.requireNonNull(
                blockingReasons, "blockingReasons"));
        if (status == Status.VALID && !failedGateCodes.isEmpty()) {
            throw new IllegalArgumentException(
                    "VALID result must not carry failedGateCodes");
        }
        if (status == Status.INVALID && blockingReasons.isEmpty()) {
            throw new IllegalArgumentException(
                    "INVALID result must carry at least one blockingReason");
        }
    }

    public String datasetId() { return datasetId; }
    public Status status() { return status; }
    public List<ScenarioProfile> evaluatedScenarioProfiles() { return evaluatedScenarioProfiles; }
    public List<ScenarioProfile> missingScenarioProfiles() { return missingScenarioProfiles; }
    public List<String> passedGateCodes() { return passedGateCodes; }
    public List<String> failedGateCodes() { return failedGateCodes; }
    public List<String> blockingReasons() { return blockingReasons; }

    public boolean isValid() { return status == Status.VALID; }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
