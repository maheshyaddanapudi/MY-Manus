# MY-Manus — Claude Code Instructions

## Identity

MY-Manus is a full-stack AI agent platform using CodeAct architecture where an LLM writes and executes Python code inside Docker-sandboxed containers. Backend: Java 17 / Spring Boot 3.5.6 / Spring AI 1.1.0-M2 / PostgreSQL 15 / Maven. Frontend: TypeScript 5.9 / React 19 / Vite 7.2 / Zustand 5 / Tailwind CSS 4. Sandbox: Docker (Ubuntu 22.04 + Python 3.11 + Node 22.13).

## Essential Commands

### Backend (from `backend/` directory)

```bash
# Compile (no tests)
mvn compile

# Run all tests (JUnit 5, uses H2 + mocked AnthropicService)
mvn test

# Run single test class
mvn test -Dtest=AgentControllerTest

# Run single test method
mvn test -Dtest=AgentControllerTest#testSendMessage

# Run dev server (H2 in-memory, no Docker/Postgres required)
mvn spring-boot:run -Dspring.profiles.active=dev

# Package JAR
mvn package -DskipTests
```

### Frontend (from `frontend/` directory)

```bash
# Install dependencies
npm install

# Type check (must pass clean)
npx tsc -b

# Run all tests (Vitest + Testing Library + jsdom)
npx vitest run

# Run single test file
npx vitest run src/components/__tests__/ChatPanel.test.tsx

# Lint (ESLint 9 flat config)
npm run lint

# Build for production (runs tsc -b then vite build)
npm run build

# Dev server (port 5173, proxies /api and /ws to localhost:8080)
npm run dev
```

### Docker

```bash
# Full stack (Postgres + Backend + Frontend)
docker-compose up -d

# Build sandbox image
cd sandbox && docker build -t mymanus-sandbox:latest .
```

## Architecture

Spring Boot monolith (Controller → Service → Repository → PostgreSQL) with STOMP WebSocket for real-time events, React SPA frontend, and Docker sandbox for Python execution.

| Directory | Purpose |
|---|---|
| `backend/src/main/java/ai/mymanus/controller/` | REST endpoints (`/api/**`), 10 controllers |
| `backend/src/main/java/ai/mymanus/service/` | Business logic, agent loop, LLM integration |
| `backend/src/main/java/ai/mymanus/service/sandbox/` | Docker/host Python execution |
| `backend/src/main/java/ai/mymanus/model/` | JPA entities (9 tables) |
| `backend/src/main/java/ai/mymanus/repository/` | JPA repository interfaces |
| `backend/src/main/java/ai/mymanus/tool/` | Tool interface + 22 tool implementations |
| `backend/src/main/java/ai/mymanus/config/` | Spring beans, security, WebSocket, OpenAPI |
| `frontend/src/components/` | 11 feature panels (Chat, Terminal, Editor, Browser, etc.) |
| `frontend/src/stores/agentStore.ts` | Single Zustand store — all app state |
| `frontend/src/services/` | REST client (Axios), WebSocket client (STOMP) |
| `frontend/src/types/index.ts` | All TypeScript type definitions |

## Code Patterns

- **ALWAYS** use Lombok annotations on backend classes: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` on entities; `@RequiredArgsConstructor` + `@Slf4j` on services/controllers. Skipping Lombok causes compilation failures because existing code depends on generated methods.
- **ALWAYS** use constructor injection via `@RequiredArgsConstructor` — never use `@Autowired` field injection. All existing services follow this pattern.
- **ALWAYS** add OpenAPI annotations (`@Operation`, `@ApiResponse`, `@Tag`) to new controller methods. The API is documented at `/swagger-ui.html` and all existing endpoints are annotated.
- **NEVER** use `@Column(columnDefinition = "jsonb")` — use `@Column(columnDefinition = "json")` with `@Convert(converter = JsonMapConverter.class)`. The H2 dev profile does not support JSONB; this mismatch caused multiple production bugs (`JsonMapConverter.java` was the #3 highest-churn file).
- **ALWAYS** implement new agent tools by creating a `@Component` class implementing `ai.mymanus.tool.Tool` interface — they are auto-registered by `ToolRegistry` via Spring DI. Never manually register tools.
- **ALWAYS** null-check `getStderr()` and `getStdout()` on `ExecutionResult` before using their values. These can be null and caused NullPointerExceptions in production.
- **NEVER** modify `agentStore.ts` `handleAgentEvent()` without understanding the chunk buffering logic. This method manages `thoughtBuffer`, `messageBuffer`, `lastThoughtMessageId`, `lastEventId`, and `lastMessageId` — incorrect changes cause duplicate or lost messages in the UI.
- **ALWAYS** place frontend component tests in `frontend/src/components/__tests__/` (co-located `__tests__` directory pattern). Service tests go in `frontend/src/services/__tests__/`, store tests in `frontend/src/stores/__tests__/`.
- **ALWAYS** export new frontend components via barrel `index.ts` files in their feature directory. Existing directories (`Browser/`, `EventStream/`, `FileTree/`, `Knowledge/`, `Plan/`, `Replay/`, `Notifications/`, `ConversationList/`) all use this pattern.
- **NEVER** put TypeScript type definitions in component files — add them to `frontend/src/types/index.ts`. All shared types are centralized there.

## Task Delegation

For non-trivial tasks, you MUST use the Task tool to delegate to specialized subagents.
Choose the right subagent based on the task type:

- **New feature (backend)** → delegate to `implementer`
- **New feature (frontend/UI)** → delegate to `frontend-specialist`
- **New feature (full-stack)** → delegate to `implementer` first, then `frontend-specialist`
- **Bug investigation & root cause** → delegate to `debugger`
- **Bug fix implementation** → delegate to `implementer`
- **Writing or updating tests** → delegate to `test-engineer`
- **Code review** → delegate to `reviewer`
- **Security review (sandbox, auth, code execution paths)** → delegate to `security-auditor`
- **Architecture or impact analysis** → delegate to `planner`

For multi-phase work, orchestrate this delegation chain:

### Feature Development (full-stack)
1. `planner` → produces implementation plan with affected files, risks, and test strategy
2. `implementer` → writes backend code (services, models, controllers, tools)
3. `frontend-specialist` → writes frontend code (components, store updates, API client)
4. `test-engineer` → writes unit and integration tests for both layers
5. `reviewer` → reviews all changes for correctness, style, and completeness

### Bug Fix
1. `debugger` → reproduces the bug, traces root cause, proposes minimal fix
2. `implementer` → applies the fix (minimal change philosophy)
3. `test-engineer` → writes regression test proving the fix
4. `reviewer` → reviews fix and regression test

### Security-Sensitive Change
1. `planner` → scopes the change and identifies security implications
2. `implementer` → writes the code
3. `test-engineer` → writes tests including security-relevant edge cases
4. `security-auditor` → reviews for vulnerabilities (injection, sandbox escape, auth bypass)
5. `reviewer` → final review

**Important**: Subagents cannot spawn other subagents. You (the main agent) must orchestrate all delegation directly.

**Fallback**: If a subagent is unavailable or the Task tool reports it's not found:
1. Inform the user which subagent failed to load
2. Read that agent's prompt from `.claude/agents/{name}.md`
3. Follow the agent's instructions directly in the main context

## Workflow

Read `.claude/workflow-config.yml` for workflow preferences.
- If `human_in_loop_at_handoff` is `true`: pause between phases and present results
- If `human_in_loop_at_handoff` is `false`: proceed autonomously through the full workflow
- If `suggest_next_actions` is `true`: suggest 2-4 relevant next actions after completing tasks

## Verification Checklist

Before any PR or commit, verify ALL of the following:

1. **Backend compiles**: `cd backend && mvn compile` passes
2. **Backend tests pass**: `cd backend && mvn test` passes
3. **Frontend type check**: `cd frontend && npx tsc -b` passes clean
4. **Frontend tests pass**: `cd frontend && npx vitest run` — 149/152 pass (3 pre-existing failures in `MessageItem.test.tsx` alignment tests and `agentStore.test.ts` clear session test are known; do not introduce new failures)
5. **No new lint errors**: `cd frontend && npm run lint` — 137 pre-existing errors (mostly `@typescript-eslint/no-explicit-any`); do not increase this count
6. **Lombok annotations present**: New backend classes must use appropriate Lombok annotations
7. **JSON column type**: Any new JPA entity JSON columns use `@Column(columnDefinition = "json")` with `JsonMapConverter`, never `jsonb`
8. **Commit message format**: Use conventional commits — `feat:`, `fix:`, `test:`, `docs:`, `refactor:`

## Do Not Modify

| Path | Reason |
|---|---|
| `docker-compose.yml` | Infrastructure config with database credentials |
| `backend/Dockerfile` | Production build definition |
| `frontend/Dockerfile` | Production build definition |
| `sandbox/Dockerfile` | Security-critical sandbox isolation boundary |
| `backend/.env.example` | Credential template — risk of committing real secrets |
| `frontend/.env.example` | Connection string template |
| `backend/src/main/resources/application.yml` | Contains credential variable references and JWT config |
| `backend/src/main/resources/application.properties` | Contains credential variable references |
| `backend/src/main/resources/application-dev.yml` | Dev profile with dummy API keys |
| `frontend/package-lock.json` | Auto-generated — manual edits corrupt dependency tree |
| `frontend/pnpm-lock.yaml` | Auto-generated lock file |
| `scripts/*` | Build and deployment scripts |
| `backend/src/main/resources/static/*` | Built frontend assets (auto-generated from `npm run build`) |
| `backend/src/main/java/ai/mymanus/config/SecurityConfig.java` | Authentication and authorization — requires explicit approval |
| `backend/src/main/java/ai/mymanus/service/sandbox/PythonSandboxExecutor.java` | Sandbox security boundary — requires explicit approval |

## Documentation

@import docs/architecture/repo-map.md
@import docs/architecture/modules.md
@import docs/dev/build.md
@import .claude/workflow-config.yml
