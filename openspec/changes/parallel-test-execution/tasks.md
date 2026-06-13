## 1. Maven Surefire Plugin Configuration

- [x] 1.1 Add explicit `maven-surefire-plugin` declaration in `pom.xml` with JUnit Platform support

## 2. JUnit Platform Properties

- [x] 2.1 Create `src/test/resources/junit-platform.properties` with parallel execution enabled, concurrent class mode, same_thread method mode, and dynamic thread strategy

## 3. SpringBootTest Isolation

- [x] 3.1 Add `@Execution(ExecutionMode.SAME_THREAD)` annotation to `DynamicThreadPollerManagerApplicationTests`

## 4. Verification

- [x] 4.1 Run `mvn test` and verify all 646 tests pass (0 failures)
- [x] 4.2 Measure execution time: sequential 4:46 → parallel 2:37 (~1.8x speedup)
- [x] 4.3 Run `mvn test` a second time — 0 flaky tests under parallelism
