package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import java.util.Objects;
import java.util.Optional;

/**
 * Explicitly represents an observation value that may be present or absent.
 * Used by the metrics layer to preserve missing-value semantics instead of
 * silently substituting defaults.
 */
public sealed interface MetricValue<T> permits MetricValue.Present, MetricValue.Absent {

    static <T> MetricValue<T> present(T value) {
        Objects.requireNonNull(value, "value must not be null");
        return new Present<>(value);
    }

    @SuppressWarnings("unchecked")
    static <T> MetricValue<T> absent() {
        return (MetricValue<T>) Absent.INSTANCE;
    }

    boolean isPresent();

    boolean isAbsent();

    Optional<T> asOptional();

    record Present<T>(T value) implements MetricValue<T> {
        public Present {
            Objects.requireNonNull(value, "value must not be null");
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public boolean isAbsent() {
            return false;
        }

        @Override
        public Optional<T> asOptional() {
            return Optional.of(value);
        }
    }

    enum Absent implements MetricValue<Object> {
        INSTANCE;

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public boolean isAbsent() {
            return true;
        }

        @Override
        public Optional<Object> asOptional() {
            return Optional.empty();
        }
    }
}
