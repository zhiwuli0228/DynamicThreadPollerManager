# closed-loop-experiment-verification Plan

## Header

- Change: `closed-loop-experiment-verification`
- Schema: `superspec`
- Plan purpose: define the implementation sequence and verification

## 1. Implementation Sequence

1. Create `ClosedLoopExperimentTest` — single end-to-end test.
2. Run `mvn test` to verify non-regression.
3. Verify existing tests pass unmodified.

## 2. Verification Commands

```bash
mvn test
openspec validate --all --json
```

## 3. Scope Boundary

- This change creates ONLY the closed-loop test.
- No new source types, no modifications to existing packages.
- No queue resizing, persistence, REST/API/UI.
