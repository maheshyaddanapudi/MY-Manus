---
name: compliance-check
description: |
  Pre-commit verification to catch common issues before they're committed.
  Use when user says: "check my code", "pre-commit check", "compliance check",
  "ready to commit?", "validate changes", "sanity check", "before I commit"
allowed-tools:
  - Bash
  - Read
  - Glob
  - Grep
---

# Compliance Check

Run all pre-commit verifications against staged/changed files.

## Steps

1. **Identify changed files:**
   ```bash
   git diff --name-only --cached   # staged
   git diff --name-only            # unstaged
   ```

2. **Secret scan** — Check changed files for potential secrets:
   ```bash
   grep -rn -E '(api[_-]?key|secret|password|token|credential)\s*[:=]\s*["\x27][^"\x27]{8,}' --include="*.java" --include="*.ts" --include="*.tsx" --include="*.yml" --include="*.properties"
   ```
   Flag any hardcoded values that look like real credentials (not `${ENV_VAR}` references).

3. **Protected file check** — Verify none of the protected paths from CLAUDE.md are modified:
   - `docker-compose.yml`, `**/Dockerfile`, `*.env.example`
   - `application.yml`, `application.properties`, `application-dev.yml`
   - `SecurityConfig.java`, `PythonSandboxExecutor.java`
   - `package-lock.json`, `scripts/*`

4. **Backend checks** (if Java files changed):
   ```bash
   cd backend && mvn compile -q
   ```
   - Verify Lombok annotations on new classes
   - Verify JSON columns use `columnDefinition = "json"` not `"jsonb"`
   - Verify new entities have matching `schema.sql` entries

5. **Frontend checks** (if TS/TSX files changed):
   ```bash
   cd frontend && npx tsc -b
   ```
   - Count ESLint errors: `npx eslint . 2>&1 | grep -c 'error'` — must not exceed 137 (baseline)
   - Verify new types are in `types/index.ts`, not in component files

6. **Test check** — Verify tests exist for new source files:
   - New `*Controller.java` → expect `*ControllerTest.java`
   - New `*Service.java` → expect `*ServiceTest.java`
   - New `*.tsx` component → expect `__tests__/*.test.tsx`

7. **Commit message format** — Remind user to use conventional commits: `feat:`, `fix:`, `test:`, `docs:`, `refactor:`

8. **Report** — Produce a pass/fail summary for each check.
