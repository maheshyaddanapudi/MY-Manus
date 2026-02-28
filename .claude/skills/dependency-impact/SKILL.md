---
name: dependency-impact
description: |
  Analyze the blast radius of code changes by tracing dependencies.
  Use when user says: "what does this affect", "blast radius", "impact analysis",
  "dependency impact", "what depends on this", "who uses this", "change impact"
allowed-tools:
  - Bash
  - Read
  - Glob
  - Grep
---

# Dependency Impact Analysis

Trace the ripple effect of changes through the codebase.

## Steps

1. Identify changed files:
   ```bash
   git diff --name-only main...HEAD
   ```

2. For each changed file, determine its dependents:

   **Backend Java files** — Find all files that import the changed class:
   ```bash
   # Extract class name from file path
   # Search for imports of that class
   grep -r "import ai.mymanus.{package}.{ClassName}" backend/src/ --include="*.java" -l
   ```

   **Frontend TypeScript files** — Find all files importing from the changed module:
   ```bash
   grep -r "from '.*/{changed-module}'" frontend/src/ --include="*.ts" --include="*.tsx" -l
   ```

3. Classify impact by layer:
   - **Controller changes** → API consumers (frontend `services/api.ts`) may need updates
   - **Service changes** → Controllers that inject the service, other services that depend on it
   - **Model/Entity changes** → Repository, Service, Controller layers; `schema.sql` may need sync
   - **Tool changes** → `ToolRegistry` auto-discovers, but Python signature changes break sandbox bindings
   - **Store changes** (`agentStore.ts`) → Every component that calls `useAgentStore()`
   - **Type changes** (`types/index.ts`) → Every file importing from `types/`

4. Risk assessment:
   - **High risk**: Changes to `CodeActAgentService`, `agentStore.ts handleAgentEvent()`, `JsonMapConverter`, sandbox executors
   - **Medium risk**: New controller endpoints, new tools, new components
   - **Low risk**: Test-only changes, documentation, styling

5. Report: list affected files grouped by risk level, with the dependency chain for each.
