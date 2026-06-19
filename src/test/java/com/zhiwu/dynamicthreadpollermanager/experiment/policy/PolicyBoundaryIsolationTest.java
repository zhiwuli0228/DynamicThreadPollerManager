package com.zhiwu.dynamicthreadpollermanager.experiment.policy;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms the policy package has no compile-time or text-level
 * references to scenario runner, executor mutation, thread-pool,
 * scheduled-executor, or adjustment-event types. The check guards
 * the policy / mutation boundary defined by the version blueprint.
 */
class PolicyBoundaryIsolationTest {

    @Test
    void shouldNotReferenceForbiddenTypes() throws IOException {
        Path policyRoot = Path.of("src", "main", "java",
                "com", "zhiwu", "dynamicthreadpollermanager", "experiment", "policy");

        List<String> bannedSubstrings = List.of(
                "ScenarioExperimentRunner",
                "BaselineWorkloadExecutor",
                ".scenario.",
                "ExecutorAdapter",
                "QueueCapacityController",
                "MutationValidator",
                "AdjustmentEvent",
                "ThreadPoolExecutor",
                "ScheduledExecutorService"
        );

        try (Stream<Path> files = Files.walk(policyRoot)) {
            List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
            assertTrue(!javaFiles.isEmpty(),
                    () -> "expected to find Java sources under " + policyRoot);
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
