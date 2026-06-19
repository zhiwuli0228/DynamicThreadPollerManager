# Engineering and Testing Rules

## 1. Technology Baseline

- Java 21
- Spring Boot 4.0.6
- Maven Wrapper
- JUnit 5
- Mockito
- No PowerMock

## 2. Coding Rules

- Keep changes focused and minimal.
- Avoid unrelated refactoring.
- Do not create empty placeholder classes just to make a structure look complete.
- Do not implement unapproved capabilities because they appear useful.

## 3. Testing Pyramid for This Demo

The demo should favor fast unit and slice tests for behavior, with only the smallest necessary context tests for wiring. Every functional change must bring tests with it.

## 4. Deterministic Concurrency Testing

- Avoid relying on long `Thread.sleep` calls as the primary proof of correctness.
- Prefer `CountDownLatch`, `CyclicBarrier`, controlled executors, replaceable clocks or schedulers, and polling assertions with timeouts.
- Every concurrency test must have an explicit timeout so it cannot hang indefinitely.
- Dynamic configuration updates, version invalidation, cancellation, and rebuild behavior need both success and failure-path coverage.

## 5. Observability and Error Handling Rules

- Invalid configuration must not be silently ignored.
- Executor and task state transitions must be observable through queries, metrics, logs, or test evidence.
- Rejection, failure, and rebuild-trigger events must be defined in the relevant change before implementation.

## 6. Prohibited Engineering Behaviors

- No unapproved stack expansion.
- No incidental technology replacement inside a feature change.
- No implementation of behavior that exists only in roadmap language.
- No pretending the demo is production-ready before the architecture is designed for it.

## 7. Verification Evidence

Changes should leave clear evidence: tests, validation output, and a commit trail. If a behavior cannot be verified, it should not be treated as complete.
