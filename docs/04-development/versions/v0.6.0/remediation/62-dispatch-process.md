# v0.6.0-remediation Dispatch Process

## 1. Purpose

This document turns the remediation reflection into a repeatable dispatch workflow.

Its purpose is to prevent a repeat of the earlier failure mode:

- confusing the mainline with the remediation path,
- issuing execution wording before authorization is confirmed,
- and letting downstream agents hit avoidable gate failures.

## 2. Mandatory Order

Every future remediation dispatch MUST follow this order:

1. Read `docs/00-project/current-state.md`.
2. Read `preflight-note.md`.
3. Decide whether the repository snapshot is authorized for remediation execution.
4. If authorization is `NONE`, stop and return `BLOCKED: unauthorized`.
5. If authorization is updated, read `30-execution-task.md`.
6. Dispatch only the remediation execution task, never the original v0.6.0 mainline.

## 3. Dispatch Split

The workflow is split into two separate artifacts:

- `preflight-note.md`
- `30-execution-task.md`

Rules:

- `preflight-note.md` is the only artifact allowed to answer the authorization question.
- `30-execution-task.md` is only for authorized remediation execution.
- No single message should mix authorization checking and execution instructions.

## 4. Required Self-Check Before Dispatch

Before sending anything downstream, the dispatcher MUST answer these questions:

- Is the current state still `NONE`?
- Am I accidentally describing the original v0.6.0 mainline?
- Is the remediation path explicitly named?
- Does the first screen tell the worker whether it may execute?
- If the worker is unauthorized, will the text stop instead of drifting into task instructions?

If any answer is unclear, do not dispatch.

## 5. Downstream Decision Tree

```
current-state -> preflight-note
         |
         v
   authorization?
      |      |
     no     yes
      |      |
   stop   execution-task
```

Text rule:

- `no` means the downstream response is a blocked preflight only.
- `yes` means the downstream response may include the remediation execution task.

## 6. Output Rules

When authorization is denied:

- report the blocked state,
- do not create output directories,
- do not ask the worker to collect data,
- do not include any run matrix or artifact checklist.

When authorization is granted:

- include only the remediation scope,
- require the worker to keep outputs under `outputs/reports/v0.6.0-remediation/`,
- require the worker to stop on any blocking data-quality gate.

## 7. Dispatch Checklist

Before dispatching remediation work, confirm:

- `preflight-note.md` is present,
- `30-execution-task.md` is present,
- the remediation package is indexed in `README.md`,
- the current-state authority has been checked,
- the message is scoped to remediation only.

If any item fails, do not dispatch.

## 8. Anti-Regression Rule

The following behaviors are now disallowed:

- starting from the mainline `v0.6.0` plan when the remediation path is requested,
- using execution language before authorization is known,
- treating missing workspace files as a reason to improvise scope,
- allowing downstream agents to infer permission from file presence alone.

## 9. Handoff Contract

Future handoffs MUST include two explicit lines:

- authorization verdict
- next allowed action

Examples:

- `authorization: BLOCKED`
- `next allowed action: none`

or

- `authorization: GRANTED`
- `next allowed action: remediation execution only`

## 10. Final Rule

This process is the permanent fix for the earlier dispatch error.
If future remediation work starts here, it will not repeat the mainline/remediation confusion.

