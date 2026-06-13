## Why

The test suite has grown to 622 tests across 75 classes, all running sequentially. As the project grows, test execution time becomes a bottleneck in the development feedback loop. The test suite is well-isolated (no shared mutable state, no base classes, per-test temp dirs) making it an ideal candidate for parallelization. Enabling JUnit 5 parallel execution requires only configuration changes with zero code modifications to existing tests.

## What Changes

**Test Execution Strategy**
- From: All 75 test classes run sequentially in a single thread
- To: Test classes run concurrently across available CPU cores, methods within a class remain sequential
- Impact: Non-breaking — test behavior and assertions unchanged, only execution ordering affected

**SpringBootTest Isolation**
- From: `DynamicThreadPollerManagerApplicationTests` runs as part of the sequential suite
- To: Explicitly marked with `@Execution(SAME_THREAD)` to prevent Spring context cache contention
- Impact: Non-breaking — no behavioral change, explicit safety annotation

## Capabilities

### New Capabilities
- `parallel-test-execution-config`: JUnit 5 parallel execution configuration via `junit-platform.properties` and Surefire plugin setup in `pom.xml`

### Modified Capabilities
(none — this change adds configuration only, no existing capability requirements change)

## Impact

- `src/test/resources/junit-platform.properties` — new file
- `pom.xml` — add explicit `maven-surefire-plugin` configuration
- `DynamicThreadPollerManagerApplicationTests.java` — add `@Execution(SAME_THREAD)` annotation
- `docs/04-development/testing-guide.md` — update with parallelization guidelines
- No production code changes
- No dependency changes
- No API changes
