# Module Reference

## Backend Modules

All backend code lives under `backend/src/main/java/ai/mymanus/`. Package prefix: `ai.mymanus`.

---

### `config/` — Application Configuration

**Responsibility:** Spring beans, security filters, WebSocket setup, Docker client, OpenAPI, and Spring AI configuration.

**Key classes:**
- `SecurityConfig` — HTTP security filter chain, CORS, JWT toggle via `auth.enabled`
- `WebSocketConfig` — STOMP endpoint `/ws`, message broker `/topic`
- `DockerConfig` — Docker client bean from `DOCKER_HOST`
- `SpringAIConfig` — Anthropic chat client configuration
- `ToolConfiguration` — Tool bean registration
- `OpenApiConfig` — Swagger/OpenAPI metadata
- `JsonMapConverter` — JPA `Map<String, Object>` <-> JSON column converter

**Dependencies:** Imported by all other packages (Spring beans). Imports nothing from project code except `JsonMapConverter` used by `model/`.

---

### `controller/` — REST API Layer

**Responsibility:** HTTP request handling, parameter validation, response formatting. All endpoints prefixed `/api/`.

**Key classes:**
- `AgentController` (`/api/agent/**`) — Chat, session CRUD, event stream, tool history (main API surface)
- `WebSocketController` — STOMP message handling for real-time events
- `SandboxController` (`/api/sandbox/**`) — Sandbox status, shell execution
- `FileController` (`/api/files/**`) — File listing and reading within session workspaces
- `DocumentController` (`/api/documents/**`) — Knowledge base document upload/search
- `PlanController` (`/api/plan/**`) — Todo/plan retrieval for live visualization
- `NotificationController` (`/api/notifications/**`) — Notification CRUD
- `SessionReplayController` (`/api/replay/**`) — Session replay data
- `BrowserMonitorController` (`/api/browser/**`) — Console logs, network requests
- `MultiAgentController` (`/api/multi-agent/**`) — Multi-agent orchestration
- `HealthController` (`/api/health`) — Health check endpoint

**Dependencies:** Imports `service/`, `dto/`, `model/`. Imported by nothing (leaf layer).

---

### `dto/` — Data Transfer Objects

**Responsibility:** Request/response shapes for REST API.

**Key classes:**
- `ChatRequest` — `sessionId` + `message` (POST `/api/agent/chat`)
- `ChatResponse` — `sessionId` + `message` + `status` (Lombok `@Builder`)
- `AgentEvent` — WebSocket event payload: `type` + `content` + `metadata`

**Dependencies:** Imports nothing from project. Imported by `controller/`.

---

### `model/` — JPA Entities

**Responsibility:** Database table mappings. All use Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`.

**Key classes:**
- `AgentState` — `agent_states` table. Session root entity with `sessionId`, `status` (IDLE/RUNNING/COMPLETED/ERROR), `executionContext` (JSON), `metadata` (JSON)
- `Message` — `messages` table. `role` (USER/ASSISTANT/SYSTEM) + `content`. FK to `AgentState`
- `Event` — `events` table. `type` (USER_MESSAGE/AGENT_THOUGHT/AGENT_ACTION/OBSERVATION/AGENT_RESPONSE/SYSTEM/ERROR), `iteration`, `sequence`, `data` (JSON). FK to `AgentState`
- `ToolExecution` — `tool_executions` table. `toolName`, `inputParams`, `outputResult` (JSON), `success`. FK to `AgentState`
- `Document` — `documents` table. Knowledge base documents with `filename`, `content`, `mimeType`
- `DocumentChunk` — `document_chunks` table. RAG chunks with `embedding`. FK to `Document`
- `Notification` — `notifications` table. `type`, `priority`, `isRead`
- `ConsoleLog` — `console_logs` table. Browser console output
- `NetworkRequest` — `network_requests` table. Browser network activity

**Dependencies:** Imports `config/JsonMapConverter`. Imported by `repository/`, `service/`, `controller/`.

---

### `entity/` — Spring AI Entities

**Responsibility:** JPA mappings for Spring AI's chat memory JDBC support.

**Key classes:**
- `SpringAIChatMemory` — Maps to Spring AI's chat memory table
- `SpringAIChatMemoryId` — Composite key for chat memory

**Dependencies:** Standalone. Used by Spring AI auto-configuration.

---

### `repository/` — Data Access Layer

**Responsibility:** JPA repository interfaces. All extend `JpaRepository`.

**Key classes:**
- `AgentStateRepository` — `findBySessionId()`, `deleteBySessionId()`
- `MessageRepository` — Messages by state
- `EventRepository` — Events by agent state, by iteration
- `ToolExecutionRepository` — Tool executions by agent state
- `DocumentRepository` / `DocumentChunkRepository` — Knowledge base storage
- `NotificationRepository` — Notifications by session
- `ConsoleLogRepository` / `NetworkRequestRepository` — Browser monitoring data

**Dependencies:** Imports `model/`. Imported by `service/`.

---

### `service/` — Business Logic Layer

**Responsibility:** Core business logic, orchestration, external integrations.

**Key classes:**
- `CodeActAgentService` — **Core agent loop.** `processQuery()` runs the think-act-observe cycle up to `maxIterations` (default 20). Streams events via `SimpMessagingTemplate`
- `AnthropicService` — Spring AI Anthropic client wrapper. `generate()`, `generateStream()`, `generateSimple()`
- `PromptBuilder` — Builds system prompts, extracts `<execute>` code blocks from LLM responses
- `AgentStateService` — Session CRUD, message management, execution context persistence
- `EventService` — Event stream append/query: `appendUserMessage()`, `appendAgentThought()`, `appendAgentAction()`, `appendObservation()`, `appendAgentResponse()`
- `MessageClassifier` — LLM-based classification of user messages into TASK/QUERY/ADJUSTMENT
- `PythonValidationService` — Syntax validation and safety checks before code execution
- `DocumentService` — Document upload, chunking for RAG
- `RAGService` — Semantic search over document chunks
- `NotificationService` — Create and manage notifications
- `SessionReplayService` — Session replay data retrieval
- `TodoMdParser` / `TodoMdWatcher` / `TodoMdStructure` / `TodoTask` / `TaskStatus` — Parse and watch `todo.md` files in workspace

**Dependencies:** Imports `repository/`, `model/`, `dto/`, `service/sandbox/`, `service/browser/`, `tool/`. Imported by `controller/`.

---

### `service/sandbox/` — Sandbox Execution

**Responsibility:** Execute Python code in isolated Docker containers or host processes.

**Key classes:**
- `SandboxExecutor` (interface) — `execute(sessionId, code, context)`, `destroySessionContainer()`
- `PythonSandboxExecutor` — Docker-based execution with resource limits (512MB memory, 50% CPU, 30s timeout, no network)
- `HostPythonExecutor` — Host-based execution for dev profile (no Docker required)
- `ExecutionResult` — `stdout`, `stderr`, `exitCode`, `variables`, `success`, `error`
- `ShellExecutionResult` — Shell command result wrapper
- `ToolRpcHandler` — Handles tool RPC calls from within sandbox Python code

**Dependencies:** Imports `model/`, `tool/ToolRegistry`. Imported by `service/CodeActAgentService`.

---

### `service/browser/` — Browser Automation

**Responsibility:** Playwright-based browser session management.

**Key classes:**
- `BrowserExecutor` — Create/manage Playwright browser sessions, execute actions
- `BrowserSession` — State for a single browser session (page, tabs, cookies)

**Dependencies:** Imports Playwright. Imported by browser tool implementations.

---

### `multiagent/` — Multi-Agent Orchestration

**Responsibility:** Coordinate multiple agent instances with different roles.

**Key classes:**
- `MultiAgentOrchestrator` — Orchestrate agents with different roles/specializations
- `AgentConfig` — Configuration for individual agent instances
- `AgentRole` — Enum/definition of agent roles

**Dependencies:** Imports `service/`. Imported by `MultiAgentController`.

---

### `tool/` — Tool System

**Responsibility:** Plugin architecture for agent tools. Tools are auto-registered via Spring DI.

**Key classes:**
- `Tool` (interface) — `getName()`, `getDescription()`, `getPythonSignature()`, `execute(Map<String, Object>)`
- `ToolRegistry` — Auto-discovers all `Tool` beans, provides `getAllTools()`, `generatePythonBindings()`

**Tool implementations in `tool/impl/`:**

| Subdirectory | Tools |
|---|---|
| `browser/` | `BrowserNavigateTool`, `BrowserViewTool`, `BrowserClickTool`, `BrowserInputTool`, `BrowserScrollUpTool`, `BrowserScrollDownTool`, `BrowserRefreshTool`, `BrowserPressKeyTool`, `BrowserTool` |
| `file/` | `FileReadTool`, `FileWriteTool`, `FileListTool`, `FileFindByNameTool`, `FileFindContentTool`, `FileReplaceStringTool`, `FileTool` |
| `shell/` | `ShellExecTool` |
| `communication/` | `MessageNotifyUserTool`, `MessageAskUserTool` |
| (root) | `WebSearchTool`, `DataVisualizationTool`, `PrintTool`, `TodoTool`, `SearchToolsTool`, `McpCallTool` |

**Dependencies:** Imports `service/sandbox/`, `service/browser/`. Imported by `service/sandbox/ToolRpcHandler`, `config/ToolConfiguration`.

---

## Frontend Modules

All frontend code lives under `frontend/src/`.

---

### `components/` — UI Components

**Responsibility:** React functional components organized by feature domain.

| Directory | Key Component(s) | Responsibility |
|---|---|---|
| `Chat/` | `ChatPanel`, `ChatInput`, `MessageList`, `MessageItem`, `CollapsibleCodeBlock`, `CollapsibleObservation`, `CollapsibleThought` | Conversation UI, message rendering, input handling |
| `Terminal/` | `TerminalPanel` | Code execution output display |
| `Editor/` | `EditorPanel` | Monaco code editor showing agent-generated code |
| `Browser/` | `EnhancedBrowserPanel`, `BrowserPanel`, `SnapshotViewer`, `ConsoleViewer`, `NetworkViewer` | Browser automation visualization |
| `EventStream/` | `EventStreamPanel`, `EventItem` | Event stream timeline (think/act/observe events) |
| `FileTree/` | `FileTreePanel`, `FileNode`, `FileViewer` | Workspace file browser |
| `Replay/` | `SessionReplayPanel` | Session replay with time-travel |
| `Knowledge/` | `DocumentPanel` | RAG document upload and search |
| `Plan/` | `PlanPanel` | Live todo.md visualization |
| `Notifications/` | `NotificationBell`, `NotificationPanel` | In-app notification center |
| `ConversationList/` | `ConversationList` | Session sidebar (create, switch, delete, rename) |
| `Layout/` | `MainLayout`, `Header`, `ActivityBanner` | App shell, panel switching, sidebar toggle |

**Dependencies:** Imports `stores/agentStore`, `services/api`, `services/websocket`, `types/`.

---

### `stores/` — State Management

**Responsibility:** Single Zustand store managing all application state.

**Key file:** `agentStore.ts` — Exports `useAgentStore` hook. Contains:
- Session state (multi-session support, current session)
- Message and event arrays
- Agent status tracking (idle/thinking/executing/done/error)
- Terminal output, code history, execution context
- UI state (active panel, sidebar, chunk buffering)
- `handleAgentEvent()` — Maps WebSocket `AgentEvent` to store updates
- Async actions: `loadSessions()`, `createNewSession()`, `switchSession()`, `deleteSession()`, `renameSession()`

**Dependencies:** Imports `services/api`, `types/`. Imported by all components.

---

### `services/` — API and WebSocket Clients

**Responsibility:** External communication layer.

**Key files:**
- `api.ts` — `ApiService` class (Axios singleton). All REST calls to `/api/**`. Exported as `apiService` + individual function exports for backward compatibility
- `websocket.ts` — `WebSocketService` class (STOMP client singleton). Connects to `/ws`, subscribes to `/topic/agent/{sessionId}`. Exported as `websocketService`
- `notificationService.ts` — Browser Notification API wrapper

**Dependencies:** Imports `types/`. Imported by `stores/`, `components/`.

---

### `types/` — TypeScript Definitions

**Responsibility:** Centralized type definitions in `index.ts`.

**Key types:** `Message`, `AgentEvent`, `ChatRequest`, `ChatResponse`, `Session`, `SessionStatus`, `Event`, `EventType`, `ExecutionContext`, `CodeExecution`, `TerminalOutput`

**Dependencies:** None. Imported by all other frontend modules.

---

### `theme/` — Design System

**Responsibility:** Centralized theme configuration for consistent styling.

**Key files:** `theme.ts`, `components.ts`, `index.ts` (barrel export)

**Dependencies:** None. Imported by components.
