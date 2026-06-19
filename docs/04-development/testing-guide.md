# Testing Guide

## Baseline

- Java 21.
- Maven Wrapper.
- JUnit 5.
- Mockito.
- No PowerMock.

## Testing Principles

- Prefer deterministic tests.
- Cover concurrency behavior with controlled synchronization, time bounds, and explicit assertions.
- Record verification commands as part of the active task evidence.

## Note

- This guide is a policy reference, not a claim that new code has been implemented.
