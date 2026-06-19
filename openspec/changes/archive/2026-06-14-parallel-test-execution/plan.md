# Parallel Test Execution Implementation Plan

> **For agentic workers:** Implement task-by-task, running verification after each step.

**Goal:** Enable JUnit 5 parallel test execution to reduce the 622-test suite execution time.

**Architecture:** Configuration-only change — add `junit-platform.properties` and explicit Surefire plugin config. One annotation addition on the `@SpringBootTest` class.

**Tech Stack:** Java 21, JUnit 5, Maven Surefire, Spring Boot

---

## Task 1: Maven Surefire Plugin Configuration

- [ ] **Step 1:** Read current `pom.xml` to identify the Spring Boot parent POM version and any existing plugin declarations
- [ ] **Step 2:** Add `maven-surefire-plugin` in `<build><plugins>` section. Use the version managed by Spring Boot parent (do not hardcode). Configure `<argLine>` if needed for module access.
- [ ] **Step 3:** Run `mvn test` to verify existing tests still pass with explicit surefire config (no parallel yet)

## Task 2: JUnit Platform Properties

- [ ] **Step 1:** Create `src/test/resources/junit-platform.properties` with the following content:
  ```properties
  junit.jupiter.execution.parallel.enabled=true
  junit.jupiter.execution.parallel.mode.default=same_thread
  junit.jupiter.execution.parallel.mode.classes.default=concurrent
  junit.jupiter.execution.parallel.config.strategy=dynamic
  junit.jupiter.execution.parallel.config.dynamic.factor=1.0
  ```
- [ ] **Step 2:** Run `mvn test` and verify all 622 tests pass under parallel execution

## Task 3: SpringBootTest Isolation

- [ ] **Step 1:** Read `DynamicThreadPollerManagerApplicationTests.java` to confirm current state
- [ ] **Step 2:** Add `@Execution(ExecutionMode.SAME_THREAD)` annotation. Add import for `org.junit.jupiter.api.parallel.Execution` and `org.junit.jupiter.api.parallel.ExecutionMode`.
- [ ] **Step 3:** Run `mvn test` to verify the SpringBootTest class runs correctly in isolation

## Task 4: Verification

- [ ] **Step 1:** Run `mvn test` with timing — record total execution time
- [ ] **Step 2:** Run `mvn test` a second time to detect flaky tests under parallelism
- [ ] **Step 3:** Compare execution time with sequential baseline (pre-change). Expect 3-5x speedup on 8-core machine.

## Commit Strategy

- **Commit 1** (after Task 1): `build: add explicit maven-surefire-plugin configuration`
- **Commit 2** (after Tasks 2-3): `test: enable JUnit 5 parallel execution with SpringBootTest isolation`
- **Commit 3** (after Task 4): verification only, no commit needed unless fixes required
