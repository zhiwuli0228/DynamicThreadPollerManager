## 1. Maven Surefire Plugin Configuration

- [ ] 1.1 Add explicit `maven-surefire-plugin` declaration in `pom.xml` with JUnit Platform support

## 2. JUnit Platform Properties

- [ ] 2.1 Create `src/test/resources/junit-platform.properties` with parallel execution enabled, concurrent class mode, same_thread method mode, and dynamic thread strategy

## 3. SpringBootTest Isolation

- [ ] 3.1 Add `@Execution(ExecutionMode.SAME_THREAD)` annotation to `DynamicThreadPollerManagerApplicationTests`

## 4. Verification

- [ ] 4.1 Run `mvn test` and verify all 622 tests pass
- [ ] 4.2 Measure execution time before and after parallelization to confirm speedup
- [ ] 4.3 Run `mvn test` a second time to check for flaky tests under parallelism
