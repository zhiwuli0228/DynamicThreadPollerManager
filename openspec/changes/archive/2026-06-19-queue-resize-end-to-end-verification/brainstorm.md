## Alternatives

### Alternative A: Single merged test in change 1
Add all end-to-end tests to `queue-resize-command-and-rebuild` directly instead of a separate change.
- **Pros**: Fewer changes to track, simpler workflow.
- **Cons**: Change 1 already covers 5 components + unit/integration tests (~15-20 tests); adding 6 more end-to-end tests bloats the change. The e2e tests depend on the full adapter/rebuild/safety-gate chain being stable, so executing them before change 1 is complete creates false failures.
- **Verdict**: Rejected — cleaner to verify the integrated chain after it exists.

### Alternative B: Defer e2e tests to post-v0.9.0
Skip end-to-end verification in v0.9.0, rely on unit/integration tests from change 1.
- **Pros**: Faster v0.9.0 delivery.
- **Cons**: No proof that the resize pipeline works end-to-end under realistic conditions (real ManagedExecutor, real queue, real scenario runner). The SR explicitly requires e2e coverage for AC-v0.9-012/013/014.
- **Verdict**: Rejected — violates v0.9.0 acceptance criteria.

### Alternative C: Dedicated verification change (chosen)
Separate `queue-resize-end-to-end-verification` change that depends on change 1 completing first.
- **Pros**: Clean separation of concerns. Change 1 delivers the capability; change 2 proves it works. Each change is independently verifiable. Follows the SR's explicit change decomposition (section 5).
- **Cons**: Two changes instead of one.
- **Verdict**: Selected — matches SR design and avoids bloating change 1.
