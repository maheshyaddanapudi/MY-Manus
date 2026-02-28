# System Summary

## What MY-Manus Does at Runtime

MY-Manus is a CodeAct agent platform where users submit natural-language tasks and an LLM (Claude) autonomously solves them by writing and executing Python code in a sandboxed environment. The system runs a think-code-execute-observe loop (max 20 iterations) until the task is complete, streaming real-time progress to the browser over WebSocket.

**Runtime lifecycle of a single user request:**
1. User sends a message via the React SPA
2. `POST /api/agent/chat` accepts the request and returns immediately
3. `CodeActAgentService.processQuery()` runs asynchronously on a background thread
4. Each iteration: Claude generates a thought → extracts `<execute>` code blocks → validates syntax and safety → executes in Docker sandbox → captures output → feeds observation back to Claude
5. WebSocket events (`thought_chunk`, `code`, `output`, `status`) stream to the frontend in real time
6. When Claude produces a response without code blocks, the loop ends and the final message is sent

## Component Inventory

### Backend Services (core runtime)

| Service | Responsibility |
|---|---|
| `CodeActAgentService` | Orchestrates the agent loop: LLM calls, code extraction, sandbox execution, event emission |
| `AnthropicService` | Wraps Spring AI `ChatClient` for streaming/blocking Claude calls with JDBC conversation memory |
| `EventService` | Append-only event log (USER_MESSAGE → AGENT_THOUGHT → AGENT_ACTION → OBSERVATION → AGENT_RESPONSE) |
| `AgentStateService` | Session CRUD, message history, execution context (Python variables), tool execution recording |
| `PromptBuilder` | System prompt construction with tool descriptions, execution context, and optional RAG context |
| `PythonValidationService` | Pre-execution validation: `ast.parse()` for syntax, regex for dangerous patterns |
| `ToolRpcHandler` | Dispatches tool calls from Python sandbox back to Java via stdin/stdout JSON-RPC protocol |

### Backend Services (features)

| Service | Responsibility |
|---|---|
| `DocumentService` | Document upload, chunking (1000 chars, 200 overlap), and embedding for RAG |
| `RAGService` | Embedding generation (Voyage AI → OpenAI → TF-IDF fallback) and cosine similarity search |
| `NotificationService` | Persistent notifications with WebSocket push and daily cleanup |
| `SessionReplayService` | Point-in-time session state reconstruction from the event stream |
| `TodoMdWatcher` | NIO file watcher on `todo.md` files; parses and broadcasts plan updates via WebSocket |
| `MessageClassifier` | LLM-based classification of user messages as TASK, QUERY, or ADJUSTMENT |
| `BrowserExecutor` | Playwright-based headless Chromium for browser automation tools |
| `MultiAgentOrchestrator` | Multi-agent pipeline execution (sequential or parallel) |

### Sandbox Executors

| Executor | Active When | Isolation |
|---|---|---|
| `PythonSandboxExecutor` | `sandbox.mode=docker` (default) | Docker container per session: memory limit, CPU quota, no network |
| `HostPythonExecutor` | `sandbox.mode=host` (dev only) | None — runs Python directly on host. NOT SAFE for production |

### Controllers (API surface)

| Controller | Path | Endpoints | Purpose |
|---|---|---|---|
| `AgentController` | `/api/agent` | 11 | Chat, session CRUD, messages, context, events |
| `FileController` | `/api/files` | 2 | Read-only workspace file access |
| `DocumentController` | `/api/documents` | 6 | Document upload, search, chunk retrieval |
| `SandboxController` | `/api/sandbox` | 3 | Container stats and cleanup |
| `NotificationController` | `/api/notifications` | 6 | User notification management |
| `PlanController` | `/api/plan` | 4 | Todo.md watching and retrieval |
| `SessionReplayController` | `/api/replay` | 5 | Event-based session replay |
| `MultiAgentController` | `/api/multi-agent` | 3 | Multi-agent orchestration |
| `BrowserMonitorController` | `/api/browser` | 6 | Console logs and network requests |
| `HealthController` | `/api/health` | 1 | Liveness check |
| `WebSocketController` | STOMP | 1 | Session subscription confirmation |

**Total: 48 endpoints** (47 REST + 1 STOMP message mapping)

### Frontend

| Module | Responsibility |
|---|---|
| `agentStore.ts` | Single Zustand store — all state, chunk buffering, event dispatching |
| `api.ts` | Axios-based REST client (15 methods, 5-minute timeout) |
| `websocket.ts` | STOMP client with auto-reconnect (5s), heartbeat (4s) |
| 11 component panels | Chat, Terminal, Editor, Browser, EventStream, Files, Replay, Knowledge, Plan, Notifications, ConversationList |

## Integration Points

| External System | Protocol | Direction | Configuration |
|---|---|---|---|
| **Anthropic Claude API** | HTTPS (SSE streaming) | Backend → External | `ANTHROPIC_API_KEY` env var, model configured in `application.yml` |
| **PostgreSQL 15** | JDBC | Backend ↔ DB | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` env vars |
| **Docker Engine** | Unix socket | Backend → Docker | `/var/run/docker.sock` (configurable via `docker.host`) |
| **Voyage AI** (optional) | HTTPS | Backend → External | `rag.embedding.voyageai-api-key` for embeddings |
| **OpenAI** (optional) | HTTPS | Backend → External | `rag.embedding.openai-api-key` for embeddings |
| **SerpAPI** (optional) | HTTPS | Backend → External | API key for web search tool |

## Configuration Surface

### Application Properties (`application.yml`)

| Category | Key Properties | Defaults |
|---|---|---|
| **LLM** | `spring.ai.anthropic.api-key`, `chat.options.model`, `temperature`, `max-tokens` | claude-sonnet-4-20250514, 0.7, 4096 |
| **Database** | `spring.datasource.url/username/password` | PostgreSQL on localhost:5432 |
| **Sandbox** | `sandbox.mode`, `sandbox.docker.image/memory-limit/cpu-quota/timeout-seconds/network-mode` | docker, 512MB, 50% CPU, 30s, none |
| **Agent** | `agent.max-iterations`, `agent.code-execution-timeout` | 20, 30 |
| **Auth** | `auth.enabled`, `auth.jwt.secret`, `auth.jwt.expiration` | false, default-key, 24h |
| **Server** | `server.port` | 8080 |

### Dev Profile Overrides (`application-dev.yml`)

| Override | Dev Value |
|---|---|
| Database | H2 in-memory (`jdbc:h2:mem:manusdb`) |
| Schema | `spring.sql.init.mode=always` runs `schema.sql` on startup |
| Sandbox | `host` mode (no Docker) |
| Auth | Disabled |
| API key | Falls back to `sk-ant-dummy-key-for-dev` |

### Frontend Environment

| Variable | Default | Purpose |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | Backend API URL |
