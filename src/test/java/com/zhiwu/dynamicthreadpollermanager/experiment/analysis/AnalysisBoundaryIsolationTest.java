package com.zhiwu.dynamicthreadpollermanager.experiment.analysis;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms the analysis package has no compile-time or text-level
 * references to runtime-mutation, executor-adapter, or queue-resizing
 * types. The check guards the analysis / mutation boundary defined
 * by the version blueprint.
 */
class AnalysisBoundaryIsolationTest {

    private static final List<String> BANNED_SUBSTRINGS = List.of(
            "AdjustmentEvent",
            "ThreadPoolExecutor",
            "ScheduledExecutorService",
            "ExecutorAdapter",
            "QueueCapacityController",
            "MutationValidator",
            "Instant.now(",
            "RestController",
            "RestTemplate",
            "WebClient",
            "JdbcTemplate",
            "DataSource",
            "EntityManager",
            "Entity",
            "Table(",
            "Column("
    );

    @Test
    void shouldNotReferenceForbiddenTypes() throws IOException {
        Path analysisRoot = Path.of("src", "main", "java",
                "com", "zhiwu", "dynamicthreadpollermanager", "experiment", "analysis");

        try (Stream<Path> files = Files.walk(analysisRoot)) {
            List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
            assertTrue(!javaFiles.isEmpty(),
                    () -> "expected to find Java sources under " + analysisRoot);
            for (Path file : javaFiles) {
                String content = Files.readString(file);
                for (String banned : BANNED_SUBSTRINGS) {
                    assertTrue(!content.contains(banned),
                            () -> file + " must not reference '" + banned + "'");
                }
            }
        }
    }

    @Test
    void shouldNotImportScenarioRunnerTypes() throws IOException {
        // analysis may use ScenarioProfile / ScenarioRunOutcome for metadata
        // but must not depend on ScenarioExperimentRunner / BaselineWorkloadExecutor
        // which own runtime execution.
        Path analysisRoot = Path.of("src", "main", "java",
                "com", "zhiwu", "dynamicthreadpollermanager", "experiment", "analysis");

        List<String> bannedScenarioTypes = List.of(
                "ScenarioExperimentRunner",
                "BaselineWorkloadExecutor"
        );

        try (Stream<Path> files = Files.walk(analysisRoot)) {
            List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
            for (Path file : javaFiles) {
                String content = Files.readString(file);
                for (String banned : bannedScenarioTypes) {
                    assertTrue(!content.contains(banned),
                            () -> file + " must not reference scenario runner type '" + banned + "'");
                }
            }
        }
    }
}
