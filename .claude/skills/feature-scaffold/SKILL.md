---
name: feature-scaffold
description: |
  Generate boilerplate for a new feature following this project's patterns.
  Use when user says: "scaffold a feature", "create new feature", "generate boilerplate",
  "new tool", "new component", "new endpoint", "add a panel", "new API"
allowed-tools:
  - Bash
  - Read
  - Write
  - Glob
  - Grep
---

# Feature Scaffold

Generate correctly structured boilerplate based on what the user wants to add.

## Determine Feature Type

Ask the user or infer from context which type of feature:

### New Agent Tool
1. Create `backend/src/main/java/ai/mymanus/tool/impl/{category}/{ToolName}Tool.java`
2. For file tools: extend `FileTool`. For others: implement `Tool` directly.
3. Read an existing tool in the same category as reference (e.g., `FileReadTool.java` for file tools)
4. Implement: `getName()` (snake_case), `getDescription()`, `getPythonSignature()`, `execute()`
5. Annotate with `@Slf4j`, `@Component`
6. Create test: `backend/src/test/java/ai/mymanus/tool/impl/{category}/{ToolName}ToolTest.java`

### New REST Endpoint
1. Create or update controller in `backend/src/main/java/ai/mymanus/controller/`
2. Read `AgentController.java` for the pattern: `@RestController`, `@RequestMapping`, OpenAPI annotations
3. Add `@Operation`, `@ApiResponse`, `@Tag` annotations
4. Return `ResponseEntity<T>` with proper status codes
5. Create test: `@WebMvcTest` + `@Import(TestSecurityConfig.class)` + `@MockBean` dependencies

### New Frontend Panel
1. Create directory: `frontend/src/components/{PanelName}/`
2. Create component: `{PanelName}Panel.tsx` — functional component using `useAgentStore()` for state
3. Create barrel: `index.ts` re-exporting the component
4. Register panel in `components/Layout/MainLayout.tsx`
5. Add panel type to `activePanel` union in `stores/agentStore.ts`
6. Create test: `frontend/src/components/__tests__/{PanelName}Panel.test.tsx`

### New JPA Entity
1. Create entity in `backend/src/main/java/ai/mymanus/model/` with `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
2. Use `@Column(columnDefinition = "json")` + `@Convert(converter = JsonMapConverter.class)` for JSON fields
3. Add `@PrePersist`/`@PreUpdate` for timestamps
4. Create repository: `backend/src/main/java/ai/mymanus/repository/{Name}Repository.java`
5. Add `CREATE TABLE` to `backend/src/main/resources/schema.sql`
6. Create model test in `backend/src/test/java/ai/mymanus/model/`

## After Scaffolding
- Run the appropriate build check (`mvn compile` or `npx tsc -b`)
- Inform the user which files were created and what they should customize
