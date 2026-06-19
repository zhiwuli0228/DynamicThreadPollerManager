## Design Summary

The goal is to reduce unit test execution time by enabling parallel test execution. The project has 75 test classes across 8 packages with 622 tests, all currently running sequentially. The test suite is well-isolated: no shared mutable state, no base classes, per-test temp directories, and only one `@SpringBootTest` class.

## Alternatives Considered

### Alternative A: JUnit 5 Parallel Execution (junit-platform.properties)

- **Approach**: Enable JUnit 5's built-in parallel execution via `junit-platform.properties`. Configure `junit.jupiter.execution.parallel.enabled=true` with `same-thread` or `concurrent` strategies at class and method level.
- **Pros**:
  - Zero code changes required — pure configuration
  - Fine-grained control: `@Execution(ExecutionMode.SAME_THREAD)` annotation for specific tests that need isolation
  - Built into JUnit 5 platform, no plugin dependencies
  - Supports dynamic thread count via `junit.jupiter.execution.parallel.config.dynamic.factor`
  - Works at both class and method level
- **Cons**:
  - Single JVM — still shares heap and GC pressure
  - Thread count limited by available CPU cores
  - `@SpringBootTest` class may need `@Execution(SAME_THREAD)` to avoid context cache issues
- **Why not chosen**: This is the simplest approach and should be the primary strategy. However, for maximum speedup on multi-core machines, combining with Surefire forking may yield better results.

### Alternative B: Maven Surefire Forking (forkCount)

- **Approach**: Configure `maven-surefire-plugin` with `forkCount` to spawn multiple JVM processes. Each fork runs a subset of test classes in its own JVM.
- **Pros**:
  - True process isolation — no shared heap, no GC contention
  - Each fork gets its own JVM, so memory is fully isolated
  - Can combine with `reuseForks=false` for maximum isolation
- **Cons**:
  - JVM startup overhead per fork (significant for fast tests)
  - Less fine-grained — splits at class level only, not method level
  - Requires explicit surefire plugin declaration in pom.xml
  - Harder to configure optimal fork count (too many = overhead, too few = no gain)
- **Why not chosen**: JVM startup overhead likely negates gains for a 622-test suite that runs fast individually. Better suited for very large suites (thousands of tests) or integration tests.

### Alternative C: Hybrid — JUnit 5 Parallel + Surefire Forking

- **Approach**: Use Surefire forking (2 forks) with JUnit 5 parallel execution within each fork.
- **Pros**:
  - Maximum CPU utilization across cores
  - Process-level isolation between forks
  - Thread-level parallelism within each fork
- **Cons**:
  - Most complex configuration
  - Harder to diagnose flaky tests
  - Overkill for 75 test classes
- **Why not chosen**: The test suite is small enough that JUnit 5 parallel alone should achieve near-linear speedup without the complexity of multi-fork JVM management.

## Agreed Approach

**Alternative A: JUnit 5 Parallel Execution** is the recommended approach.

Rationale:
1. The test suite is small (75 classes, 622 tests) and well-isolated — JUnit 5 parallel execution alone will provide significant speedup.
2. Zero code changes needed — only configuration files.
3. The `@SpringBootTest` class can be annotated with `@Execution(SAME_THREAD)` to avoid Spring context cache contention.
4. Dynamic thread factor (`parallel.config.dynamic.factor`) adapts to the machine running the tests.
5. If further optimization is needed later, Surefire forking can be added on top.

Configuration plan:
- Create `src/test/resources/junit-platform.properties` with parallel enabled
- Set class-level strategy to `concurrent`, method-level to `same_thread` (safe default)
- Use dynamic factor for thread count (e.g., factor=1.0 = match CPU cores)
- Annotate `@SpringBootTest` class with `@Execution(SAME_THREAD)`

## Key Decisions

1. **Parallel scope**: Classes run concurrently, methods within a class run in the same thread. This is the safest default — avoids intra-class state leakage while maximizing inter-class parallelism.
2. **Thread count**: Use dynamic factor rather than fixed thread count. This adapts to CI (2 cores) vs developer machines (8+ cores).
3. **SpringBootTest isolation**: Mark the single `@SpringBootTest` class with `@Execution(SAME_THREAD)` to prevent Spring context cache thrashing.
4. **No Surefire forking**: JVM startup overhead not justified for this suite size.

## Open Questions

1. Should we also enable method-level parallelism for classes with multiple independent test methods? (Could be a follow-up optimization)
2. What dynamic factor value is optimal? (1.0 = match cores, 0.5 = half cores — depends on test CPU intensity)
3. Should the testing guide be updated with parallelization guidelines for future test authors?
