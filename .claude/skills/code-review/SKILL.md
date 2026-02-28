---
name: code-review
description: |
  Structured code review of changed files against project standards.
  Use when user says: "review my code", "code review", "check my changes",
  "review this PR", "look at my diff", "review before merge", "LGTM check"
allowed-tools:
  - Bash
  - Read
  - Glob
  - Grep
---

# Code Review

Perform a structured review of all changes.

## Steps

1. **Gather scope:**
   ```bash
   git diff --stat main...HEAD
   git log --oneline main...HEAD
   ```

2. **Read every changed file** — use `git diff main...HEAD -- {file}` for each.

3. **Check against project rules** (from CLAUDE.md and `.claude/rules/`):

   **Backend Java:**
   - Lombok annotations present (`@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`)
   - Constructor injection (no `@Autowired` field injection)
   - OpenAPI annotations on controller methods (`@Operation`, `@ApiResponse`)
   - JSON columns use `columnDefinition = "json"` not `"jsonb"`
   - `ExecutionResult` stdout/stderr null-checked before use
   - Tools implement `Tool` interface correctly (all 4 methods)
   - New entities have `schema.sql` entries

   **Frontend TypeScript:**
   - State in `agentStore.ts`, not component-local (for shared state)
   - Types in `types/index.ts`, not component files
   - Barrel `index.ts` in new feature directories
   - Tailwind classes for styling, no inline `style={}`
   - No new `any` types introduced

   **Tests:**
   - Correct test pattern for the layer (WebMvcTest, MockitoExtension, SpringBootTest, Vitest)
   - No queries by CSS class name in frontend tests
   - `vi.clearAllMocks()` in `beforeEach`

4. **Verify builds pass:**
   ```bash
   cd backend && mvn compile -q
   cd frontend && npx tsc -b
   ```

5. **Produce review report** with findings by severity:
   - **Blocking**: Violations that will cause bugs, build failures, or security issues
   - **Warning**: Pattern violations that should be fixed
   - **Suggestion**: Improvements that are optional
