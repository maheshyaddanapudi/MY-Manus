---
name: test-coverage
description: |
  Analyze test coverage for changed code and identify missing tests.
  Use when user says: "test coverage", "what needs tests", "coverage report",
  "am I missing tests", "untested code", "coverage analysis", "check coverage"
allowed-tools:
  - Bash
  - Read
  - Glob
  - Grep
---

# Test Coverage Analysis

Identify which changed files have tests and which need them.

## Steps

1. **Identify changed source files** (exclude tests, configs, docs):
   ```bash
   git diff --name-only main...HEAD | grep -E '\.(java|ts|tsx)$' | grep -v 'test\|__tests__\|Test\.java'
   ```

2. **Map source files to expected test files:**

   | Source File | Expected Test |
   |---|---|
   | `backend/.../controller/FooController.java` | `backend/.../controller/FooControllerTest.java` |
   | `backend/.../service/FooService.java` | `backend/.../service/FooServiceTest.java` |
   | `backend/.../tool/impl/{cat}/FooTool.java` | `backend/.../tool/impl/{cat}/FooToolTest.java` |
   | `backend/.../model/Foo.java` | `backend/.../model/FooTest.java` |
   | `frontend/src/components/{Dir}/Foo.tsx` | `frontend/src/components/__tests__/Foo.test.tsx` |
   | `frontend/src/services/foo.ts` | `frontend/src/services/__tests__/foo.test.ts` |
   | `frontend/src/stores/fooStore.ts` | `frontend/src/stores/__tests__/fooStore.test.ts` |

3. **Check existence** of each expected test file.

4. **Run targeted tests** for files that have tests:

   Backend:
   ```bash
   cd backend && mvn test -Dtest=FooControllerTest
   ```

   Frontend:
   ```bash
   cd frontend && npx vitest run src/components/__tests__/Foo.test.tsx
   ```

5. **Report:**
   - List source files WITH tests (and whether tests pass)
   - List source files WITHOUT tests (these need attention)
   - For files without tests, suggest which test pattern to use (WebMvcTest, MockitoExtension, Vitest)
