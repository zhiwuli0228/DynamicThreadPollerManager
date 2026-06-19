# v0.6.0-remediation Dispatch Checklist

## Purpose

Use this checklist immediately before dispatching any remediation-related message or task.

The checklist exists to prevent the earlier failure mode where mainline wording and remediation wording were mixed, and where execution text was sent before authorization was confirmed.

## 1. Authorization Check

- [ ] `docs/00-project/current-state.md` has been read.
- [ ] The current state is explicitly recorded.
- [ ] The current authorized work type is explicitly recorded.
- [ ] If the state is `NONE`, dispatch is blocked.

## 2. Path Check

- [ ] The message is scoped to `v0.6.0-remediation`.
- [ ] The message does not describe the original `v0.6.0` mainline as the execution target.
- [ ] The message does not mix mainline and remediation instructions.
- [ ] The remediation path is named before any execution text appears.

## 3. Artifact Check

- [ ] `preflight-note.md` is present in the remediation package.
- [ ] `30-execution-task.md` is present in the remediation package.
- [ ] `62-dispatch-process.md` is present in the remediation package.
- [ ] The remediation package index in `README.md` includes the dispatch assets.

## 4. Message Structure Check

- [ ] The first block of the message is authorization verdict, not task steps.
- [ ] If authorization is denied, the message stops after the blocked preflight.
- [ ] If authorization is granted, the message limits itself to remediation execution only.
- [ ] The message does not contain generic encouragement or implied permission.

## 5. Scope Check

- [ ] The message does not request code changes.
- [ ] The message does not request test changes.
- [ ] The message does not request new dependencies.
- [ ] The message does not request queue resizing.
- [ ] The message does not request production `ThreadPoolExecutor` integration.
- [ ] The message does not request a new OpenSpec change.

## 6. Output Check

- [ ] The worker is told where outputs must go if authorization is granted.
- [ ] The worker is told to stop on any blocking data-quality gate.
- [ ] The worker is told not to fabricate evidence.
- [ ] The worker is told not to treat file presence as permission.

## 7. Self-Review Check

Before dispatch, answer these questions:

- [ ] Am I accidentally sending mainline language?
- [ ] Is the authorization verdict visible before the task body?
- [ ] Would an unauthorized worker know to stop immediately?
- [ ] Would an authorized worker know exactly which remediation assets to use?

If any box cannot be checked, do not dispatch.

## 8. Dispatch Result

Record one of the following:

- `BLOCKED: unauthorized`
- `DISPATCHED: remediation execution only`

No other result is acceptable for this workflow.

