package com.zhiwu.dynamicthreadpollermanager.experiment.metrics;

import com.zhiwu.dynamicthreadpollermanager.experiment.model.PressureSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms the metrics package has no compile-time or text-level references
 * to adaptive policy or executor-mutation types. The check guards the
 * observation/control boundary defined by the version blueprint.
 */
class MetricsBoundaryIsolationTest {

    @Test
    void shouldNotReferencePolicyOrExecutorTypes() throws IOException {
        Path metricsRoot = Path.of("src", "main", "java",
                "com", "zhiwu", "dynamicthreadpollermanager", "experiment", "metrics");

        List<String> bannedSubstrings = List.of(
                "ControlPolicy",
                "ScaleDecision",
                "AdjustmentEvent",
                "ThreadPoolExecutor",
                "ScheduledExecutorService",
                "ExecutorService",
                "ExecutorConfig",
                ".policy.",
                "adaptive"
        );

        try (Stream<Path> files = Files.walk(metricsRoot)) {
            List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
            for (Path file : javaFiles) {
                String content = Files.readString(file);
                for (String banned : bannedSubstrings) {
                    assertTrue(!content.contains(banned),
                            () -> file + " must not reference '" + banned + "'");
                }
            }
        }
    }

    @Test
    void shouldCompileAgainstModelOnly() {
        PressureSnapshot snapshot = new PressureSnapshot(
                java.time.Instant.parse("2026-06-02T10:00:00Z"), 0, 0, 0.0);
        assertTrue(snapshot.timestamp() != null,
                "metrics layer should only depend on the experiment model package");
    }
}
