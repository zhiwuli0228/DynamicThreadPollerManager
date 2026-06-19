package com.zhiwu.dynamicthreadpollermanager.experiment.adjustment;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms the adjustment package has no text-level references to
 * the production {@code ThreadPoolExecutor} or to queue-capacity
 * mutation APIs. Also confirms the policy and analysis packages do
 * not depend on the adjustment package.
 */
class AdjustmentBoundaryIsolationTest {

    private static final List<String> ADJUSTMENT_BANNED_SUBSTRINGS = List.of(
            "ThreadPoolExecutor",
            "ScheduledExecutorService",
            "QueueCapacityController",
            "RestController",
            "RestTemplate",
            "WebClient",
            "JdbcTemplate",
            "DataSource",
            "EntityManager"
    );

    @Test
    void adjustmentPackageShouldNotReferenceForbiddenRuntimeApis() throws IOException {
        Path adjustmentRoot = Path.of("src", "main", "java",
                "com", "zhiwu", "dynamicthreadpollermanager", "experiment", "adjustment");

        try (Stream<Path> files = Files.walk(adjustmentRoot)) {
            List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
            assertTrue(!javaFiles.isEmpty(),
                    () -> "expected to find Java sources under " + adjustmentRoot);
            for (Path file : javaFiles) {
                String content = Files.readString(file);
                for (String banned : ADJUSTMENT_BANNED_SUBSTRINGS) {
                    assertTrue(!content.contains(banned),
                            () -> file + " must not reference '" + banned + "'");
                }
            }
        }
    }

    @Test
    void policyPackageShouldNotReferenceAdjustmentPackage() throws IOException {
        Path policyRoot = Path.of("src", "main", "java",
                "com", "zhiwu", "dynamicthreadpollermanager", "experiment", "policy");

        try (Stream<Path> files = Files.walk(policyRoot)) {
            List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
            assertTrue(!javaFiles.isEmpty(),
                    () -> "expected to find Java sources under " + policyRoot);
            for (Path file : javaFiles) {
                String content = Files.readString(file);
                assertTrue(!content.contains("experiment.adjustment"),
                        () -> file + " must not reference experiment.adjustment package");
                assertTrue(!content.contains("ScaleAdjustmentCommand"),
                        () -> file + " must not reference ScaleAdjustmentCommand");
                assertTrue(!content.contains("ExecutorAdjustmentAdapter"),
                        () -> file + " must not reference ExecutorAdjustmentAdapter");
            }
        }
    }

    @Test
    void analysisPackageShouldNotInvokeAdjustmentMutation() throws IOException {
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
                assertTrue(!content.contains("ScaleAdjustmentCommand"),
                        () -> file + " must not reference ScaleAdjustmentCommand");
                assertTrue(!content.contains("ExecutorAdjustmentAdapter"),
                        () -> file + " must not reference ExecutorAdjustmentAdapter");
                assertTrue(!content.contains("AdjustmentEvidence"),
                        () -> file + " must not create runtime adjustment evidence");
                assertTrue(!content.contains("experiment.adjustment"),
                        () -> file + " must not depend on experiment.adjustment package");
            }
        }
    }

    @Test
    void scenarioPackageShouldNotReferenceAdjustmentPackage() throws IOException {
        Path scenarioRoot = Path.of("src", "main", "java",
                "com", "zhiwu", "dynamicthreadpollermanager", "experiment", "scenario");

        try (Stream<Path> files = Files.walk(scenarioRoot)) {
            List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
            assertTrue(!javaFiles.isEmpty(),
                    () -> "expected to find Java sources under " + scenarioRoot);
            for (Path file : javaFiles) {
                String content = Files.readString(file);
                assertTrue(!content.contains("experiment.adjustment"),
                        () -> file + " must not reference experiment.adjustment package");
                assertTrue(!content.contains("ScaleAdjustmentCommand"),
                        () -> file + " must not reference ScaleAdjustmentCommand");
                assertTrue(!content.contains("ExecutorAdjustmentAdapter"),
                        () -> file + " must not reference ExecutorAdjustmentAdapter");
            }
        }
    }

    @Test
    void adjustmentPackageShouldNotDefineQueueCapacityController() throws IOException {
        Path adjustmentRoot = Path.of("src", "main", "java",
                "com", "zhiwu", "dynamicthreadpollermanager", "experiment", "adjustment");

        try (Stream<Path> files = Files.walk(adjustmentRoot)) {
            List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
            for (Path file : javaFiles) {
                String content = Files.readString(file);
                assertTrue(!content.contains("class QueueCapacityController"),
                        () -> file + " must not define QueueCapacityController");
                assertTrue(!content.contains("setQueueCapacity"),
                        () -> file + " must not mutate queue capacity");
                assertTrue(!content.contains("setCapacity("),
                        () -> file + " must not call setCapacity(");
            }
        }
    }
}
