Perform a comprehensive code review of the current changes.

1. **Scope the changes**: Run `git diff --name-only main...HEAD` to identify all changed files. Categorize them by module (backend, frontend, config, tests, docs).

2. **Pattern review**: Delegate to `reviewer` agent for full pattern compliance check, build verification, and structured verdict.

3. **Security review**: If any of these changed, also delegate to `security-auditor` agent:
   - SecurityConfig.java, PythonSandboxExecutor.java
   - Any file in `config/`, `service/sandbox/`, or `tool/`
   - Schema.sql, entity classes, or auth-related code

4. **Coverage check**: If new production code lacks corresponding tests, delegate to `test-engineer` agent to identify coverage gaps and recommend which tests to write.

5. **Final summary**: Compile all findings into a single report:
   - Review verdict (APPROVE / REQUEST CHANGES / NEEDS DISCUSSION)
   - Pattern violations found
   - Security findings (if audited)
   - Coverage gaps (if any)
   - Suggested follow-up actions
