## 1. Closed-loop experiment test

- [x] 1.1 Create `ClosedLoopExperimentTest` in `experiment.executor` test package.
- [x] 1.2 Test: Full pipeline — submit workload → observe state → evaluate policy → create command → apply adjustment → verify.
- [x] 1.3 Test: Before/after state consistency — corePoolSize, maxPoolSize, extended fields populated.
- [x] 1.4 Test: Executor cleanup — shutdown and termination in `@AfterEach`.

## 2. Verification

- [x] 2.1 `mvn test` exits 0 — all existing tests + new tests pass.
- [x] 2.2 Existing `InMemoryAdjustableExecutorProbe` tests pass unmodified.
- [x] 2.3 `openspec validate --all --json` passes.
