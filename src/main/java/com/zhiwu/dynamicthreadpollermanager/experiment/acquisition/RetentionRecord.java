package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import java.time.Instant;
import java.util.Objects;

/**
 * Explicit retention record for any non-default copy of raw
 * acquisition evidence. Raw evidence is not versioned by
 * default; if it is retained, the retention location and the
 * owner responsible for cleanup MUST be recorded in a
 * {@code RetentionRecord} and shipped alongside the run's
 * summaries.
 *
 * <p>The record is intentionally narrow: it never carries the
 * raw evidence payload itself, only metadata about where the
 * evidence lives and who is accountable for cleaning it up.
 */
public final class RetentionRecord {

    public static final String DEFAULT_POLICY = "non_versioned_no_retention";

    private final String runId;
    private final String retentionLocation;
    private final String responsibleOwner;
    private final Instant retainedAt;
    private final String cleanupPlan;

    public RetentionRecord(String runId,
                           String retentionLocation,
                           String responsibleOwner,
                           Instant retainedAt,
                           String cleanupPlan) {
        this.runId = requireNonBlank(runId, "runId");
        if (retentionLocation == null || retentionLocation.isBlank()) {
            this.retentionLocation = null;
        } else {
            this.retentionLocation = retentionLocation;
        }
        if (responsibleOwner == null || responsibleOwner.isBlank()) {
            this.responsibleOwner = null;
        } else {
            this.responsibleOwner = responsibleOwner;
        }
        this.retainedAt = retainedAt;
        if (cleanupPlan == null || cleanupPlan.isBlank()) {
            this.cleanupPlan = null;
        } else {
            this.cleanupPlan = cleanupPlan;
        }
    }

    public static RetentionRecord defaultNonVersioned(String runId) {
        return new RetentionRecord(runId, null, null, null, null);
    }

    public String runId() { return runId; }
    public String retentionLocation() { return retentionLocation; }
    public String responsibleOwner() { return responsibleOwner; }
    public Instant retainedAt() { return retainedAt; }
    public String cleanupPlan() { return cleanupPlan; }

    public boolean isRetained() {
        return retentionLocation != null;
    }

    /**
     * The default retention policy is non-versioned with no
     * retention; retained copies MUST carry a non-null
     * {@link #retentionLocation()}, {@link #responsibleOwner()},
     * {@link #retainedAt()}, and {@link #cleanupPlan()}.
     */
    public void validate() {
        if (isRetained()) {
            if (responsibleOwner == null) {
                throw new IllegalStateException(
                        "retained evidence for run " + runId + " must declare a responsibleOwner");
            }
            if (retainedAt == null) {
                throw new IllegalStateException(
                        "retained evidence for run " + runId + " must declare a retainedAt timestamp");
            }
            if (cleanupPlan == null) {
                throw new IllegalStateException(
                        "retained evidence for run " + runId + " must declare a cleanupPlan");
            }
        }
    }

    @Override
    public String toString() {
        if (!isRetained()) {
            return "RetentionRecord{runId='%s', policy=%s}".formatted(runId, DEFAULT_POLICY);
        }
        return "RetentionRecord{runId='%s', retentionLocation='%s', responsibleOwner='%s', retainedAt=%s}"
                .formatted(runId, retentionLocation, responsibleOwner, retainedAt);
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
