---
name: smart-build
description: |
  Build only the modules affected by recent changes instead of building everything.
  Use when user says: "build my changes", "fast build", "incremental build",
  "build what I changed", "quick build", "smart build", "build affected"
allowed-tools:
  - Bash
  - Read
  - Glob
  - Grep
---

# Smart Build

Build only the modules affected by your changes.

## Steps

1. Detect changed files:
   ```bash
   git diff --name-only HEAD~1
   ```
   If on a branch, compare against main:
   ```bash
   git diff --name-only main...HEAD
   ```

2. Classify changes by module:
   - Files under `backend/` → backend module affected
   - Files under `frontend/` → frontend module affected
   - Files under `sandbox/` → sandbox Docker image affected
   - Files under `docs/` only → no build needed

3. Execute targeted builds:

   **Backend only:**
   ```bash
   cd backend && mvn compile -q
   ```

   **Frontend only:**
   ```bash
   cd frontend && npx tsc -b && npx vite build
   ```

   **Both (full-stack change):**
   ```bash
   cd backend && mvn compile -q
   cd frontend && npx tsc -b
   ```

   **Sandbox only:**
   ```bash
   cd sandbox && docker build -t mymanus-sandbox:latest .
   ```

4. Report which modules were built and whether each succeeded or failed.

## Notes
- `mvn compile` is the fastest backend check (no tests, no packaging)
- `npx tsc -b` alone is sufficient for frontend type-checking without bundling
- If only `.md` or docs files changed, report "No build needed — documentation only"
