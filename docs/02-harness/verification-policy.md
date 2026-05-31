# Verification Policy

## Required Evidence

- Confirm the file scope of the change.
- Run the repository validation commands requested by the task.
- Record the result of each check honestly.

## Expected Checks for Documentation Work

- `git diff --name-only`
- `git diff --stat`
- `.\mvnw.cmd test`
- `openspec.cmd validate --all --json`
- `openspec.cmd schema validate superspec`

## Rule

- Never claim validation that was not actually executed.
