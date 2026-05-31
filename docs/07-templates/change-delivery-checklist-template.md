# Change Delivery Checklist Template

## Header

- Version name:
- Change identifier:
- Delivery stage:
- Authoritative branch:

## Checklist

### Before Implementation

- [ ] The version design status authorizes this change.
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
- [ ] `docs/00-project/current-state.md` is synchronized to the actual repository state.
- [ ] If long-lived architecture changed, `docs/01-architecture/` is updated.
- [ ] If a long-lived decision was accepted, an ADR has been created or updated.

### After Archive

- [ ] Archive receipt, verification report, and current-state record agree.
- [ ] Any remaining semantic gaps are recorded for the next change.
- [ ] The next change does not start until the handoff story is internally consistent.
