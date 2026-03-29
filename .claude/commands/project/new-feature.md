Create a complete implementation for: $ARGUMENTS

Follow this workflow:

1. **Plan**: Delegate to `planner` agent to analyze the request, map the blast radius, and produce a step-by-step implementation plan with risk assessment.

2. **Implement**: Review the plan, then delegate to `implementer` agent for backend code. If the feature includes UI work, also delegate to `frontend-specialist` agent for React/Zustand/Tailwind changes.

3. **Test**: Delegate to `test-engineer` agent to write tests for all new code — backend (JUnit 5) and frontend (Vitest) as applicable.

4. **Review**: Delegate to `reviewer` agent for pattern compliance, build verification, and structured verdict.

5. **Security check**: If the reviewer flags security concerns, or if the change touches SecurityConfig, PythonSandboxExecutor, auth, or sandbox code, delegate to `security-auditor` agent for a read-only audit.

6. **Summary**: Compile a final report listing:
   - Files created and modified
   - Test results (pass/fail counts)
   - Review verdict and any open items
   - Security findings (if audited)
