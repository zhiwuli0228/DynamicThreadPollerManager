# CLAUDE.md

Claude Code reads this file before implementation work.

Required inputs before editing code:

- `docs/harness/project-harness.md`
- The approved change artifacts for the current active change

Execution rules:

- Implement only approved tasks.
- Keep changes scoped to the approved capability.
- Write and run tests for the work being changed.
- Verify before handing back results.
- Do not expand scope or alter architecture boundaries on your own.

Engineering baseline:

- Java 21
- Maven
- JUnit 5 and Mockito
- No PowerMock
