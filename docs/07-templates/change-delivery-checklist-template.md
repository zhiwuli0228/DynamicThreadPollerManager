# Change Delivery Checklist Template

## Header

- Version name:
- Change identifier:
- Delivery stage:
- Authoritative branch:

## Checklist

### Before Implementation

- [ ] The version design status authorizes this change.
- [ ] Managed-change gates are satisfied: IR closure and SR closure are recorded, or the task explicitly authorizes a lighter documented path.
- [ ] The change scope matches the approved capability boundary.
- [ ] Requirement wording matches the intended runtime semantics.
- [ ] The current-state record reflects the current authorization story.

### During Implementation

- [ ] Implementation stays within the approved change scope.
- [ ] Tests verify semantic intent, not just object shape or lifecycle flow.
- [ ] Placeholder behavior is explicitly identified if any part is incomplete.
- [ ] No unapproved dependency or scope expansion is introduced.

### Before Archive

- [ ] Implementation evidence matches the spec wording.
- [ ] Verification evidence proves the intended semantics.
- [ ] Implementation review and test review findings are closed or recorded as accepted residual risk.
- [ ] Acceptance precheck maps user-confirmed semantics to IR, SR, spec, implementation, tests, and evidence.
- [ ] `docs/00-project/current-state.md` is synchronized to the actual repository state.
- [ ] If long-lived architecture changed, `docs/01-architecture/` is updated.
- [ ] If a long-lived decision was accepted, an ADR has been created or updated.

### After Archive

- [ ] Archive receipt, verification report, and current-state record agree.
- [ ] Any remaining semantic gaps are recorded for the next change.
- [ ] The next change does not start until the handoff story is internally consistent.
