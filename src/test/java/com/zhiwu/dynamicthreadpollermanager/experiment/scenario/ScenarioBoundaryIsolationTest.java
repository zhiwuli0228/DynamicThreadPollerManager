package com.zhiwu.dynamicthreadpollermanager.experiment.scenario;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms the scenario package has no compile-time or text-level
 * references to adaptive policy or executor-mutation types. The check
 * guards the observation/control boundary defined by the version
 * blueprint.
 */
class ScenarioBoundaryIsolationTest {

    @Test
    void shouldNotReferencePolicyOrMutationTypes() throws IOException {
        Path scenarioRoot = Path.of("src", "main", "java",
                "com", "zhiwu", "dynamicthreadpollermanager", "experiment", "scenario");

        List<String> bannedSubstrings = List.of(
                "ControlPolicy",
                "ScaleDecision",
                "AdjustmentEvent",
                "ExecutorAdapter",
                "QueueCapacityController",
                "MutationValidator",
                ".policy.",
                "adaptive"
        );

        try (Stream<Path> files = Files.walk(scenarioRoot)) {
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
}
