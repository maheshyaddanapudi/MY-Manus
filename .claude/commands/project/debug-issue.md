Investigate and fix: $ARGUMENTS

Follow this systematic debugging workflow:

1. **Understand**: Clarify the issue — identify expected vs actual behavior. If a stack trace or error message is provided, read it carefully.

2. **Search**: Search the codebase for relevant code paths. Check known bug hotspots first: JsonMapConverter, HostPythonExecutor, agentStore.handleAgentEvent(), MessageItem.tsx, ExecutionResult null handling.

3. **Isolate**: Identify which layer owns the bug (controller, service, sandbox, tool, store, component). Check git history for recent changes to the affected files: `git log --oneline -10 -- {file}`.

4. **Root cause**: Trace the data flow to find the exact root cause. Delegate to `debugger` agent if the issue is complex or spans multiple layers.

5. **Propose fix**: Describe the fix before implementing — explain what will change and why. Apply the minimal change that resolves the root cause.

6. **Implement**: Delegate to `implementer` (backend) or `frontend-specialist` (frontend) to apply the fix.

7. **Test**: Delegate to `test-engineer` to write a regression test proving the fix works and preventing recurrence.

8. **Verify**: Run the full build and test suite to confirm no regressions. Compare test pass counts against baseline (149/152 frontend, all-pass backend).

9. **Summary**: Report what was wrong, what caused it, what was changed, and what tests were added.
