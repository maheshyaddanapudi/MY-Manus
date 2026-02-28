# Repository Map

## What This Repository Is

MY-Manus is a full-stack AI agent platform built on CodeAct architecture (ICML 2024) where an LLM (Anthropic Claude) writes and executes Python code inside Docker-sandboxed containers to solve user tasks. The backend is a Java 17 / Spring Boot 3.5.6 monolith that orchestrates an agent loop (think -> code -> execute -> observe -> repeat), persists state to PostgreSQL, and streams real-time events over STOMP WebSocket. The frontend is a React 19 / TypeScript SPA with an eight-panel UI (Chat, Terminal, Editor, Browser, Events, Files, Replay, Knowledge/Plan) that renders the agent's work transparently. A Docker sandbox image (Ubuntu 22.04 + Python 3.11 + Node 22.13) provides isolated code execution with strict resource limits.

## Major Domains

| Domain | Location | Responsibility |
|---|---|---|
| **Agent Orchestration** | `backend/.../service/CodeActAgentService.java` | Core agent loop: prompt LLM, extract code, execute, observe, repeat |
| **LLM Integration** | `backend/.../service/AnthropicService.java` | Spring AI Anthropic client, streaming responses |
| **Sandbox Execution** | `backend/.../service/sandbox/` | Docker container management, Python code execution, state persistence |
| **Tool System** | `backend/.../tool/` | 22 tools (file, browser, shell, search, communication) with auto-registration |
| **Event Stream** | `backend/.../service/EventService.java` | Event sourcing: USER_MESSAGE -> AGENT_THOUGHT -> AGENT_ACTION -> OBSERVATION |
| **Session Management** | `backend/.../service/AgentStateService.java` | Session CRUD, message history, execution context persistence |
| **Real-time Communication** | `backend/.../controller/WebSocketController.java` | STOMP WebSocket events to frontend |
| **REST API** | `backend/.../controller/` | 10 controllers, 40+ endpoints, OpenAPI documented |
| **Frontend UI** | `frontend/src/components/` | 11 feature panels (Chat, Terminal, Editor, Browser, etc.) |
| **Frontend State** | `frontend/src/stores/agentStore.ts` | Single Zustand store managing all UI and session state |
| **Frontend Services** | `frontend/src/services/` | REST client (Axios), WebSocket client (STOMP), notifications |
| **Multi-Agent** | `backend/.../multiagent/` | Multi-agent orchestration with configurable agent roles |
| **RAG / Knowledge** | `backend/.../service/RAGService.java`, `DocumentService.java` | Document upload, chunking, semantic search |
| **Observability** | `application.properties` (actuator config) | Prometheus metrics via Micrometer, health endpoints |

## Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Backend runtime | Java (OpenJDK) | 17 (target), 21 (host runtime) |
| Backend framework | Spring Boot | 3.5.6 |
| AI integration | Spring AI (Anthropic) | 1.1.0-M2 |
| Database (prod) | PostgreSQL | 15 |
| Database (dev) | H2 (in-memory) | via Boot BOM |
| ORM | Hibernate / Spring Data JPA | via Boot BOM |
| Auth | Spring Security + JJWT | 0.12.3 |
| API docs | springdoc-openapi | 2.3.0 |
| Browser automation | Playwright | 1.40.0 |
| Docker client | docker-java | 3.3.4 |
| Code generation | Lombok | via Boot BOM |
| Build tool | Maven | 3.9.x (system) |
| Frontend runtime | Node.js | 22.x |
| Frontend framework | React | 19.2.0 |
| Frontend language | TypeScript | ~5.9.3 |
| Bundler | Vite | 7.2.4 |
| State management | Zustand | 5.0.8 |
| CSS framework | Tailwind CSS | 4.1.17 |
| HTTP client | Axios | 1.13.2 |
| WebSocket client | @stomp/stompjs | 7.2.1 |
| Code editor | @monaco-editor/react | 4.7.0 |
| Test (backend) | JUnit 5 + Spring Boot Test | via Boot BOM |
| Test (frontend) | Vitest + Testing Library | 4.0.14 / 16.3.0 |
| Linting | ESLint (flat config) | 9.39.1 |
| Metrics | Micrometer + Prometheus | via Boot BOM |

## Navigation Guide

| If you need to... | Look in... |
|---|---|
| Change the agent loop logic | `backend/src/main/java/ai/mymanus/service/CodeActAgentService.java` |
| Modify LLM prompts or system instructions | `backend/.../service/PromptBuilder.java` |
| Add a new tool for the agent | `backend/.../tool/impl/` — implement `Tool` interface, annotate with `@Component` |
| Change how code is executed in sandbox | `backend/.../service/sandbox/PythonSandboxExecutor.java` |
| Modify the sandbox Docker image | `sandbox/Dockerfile` |
| Add a new REST endpoint | `backend/.../controller/` — follow `AgentController.java` pattern |
| Change WebSocket event handling | Backend: `WebSocketController.java` / Frontend: `agentStore.ts` `handleAgentEvent()` |
| Add a new UI panel | `frontend/src/components/{PanelName}/` + register in `MainLayout.tsx` |
| Change state management | `frontend/src/stores/agentStore.ts` |
| Modify API client calls | `frontend/src/services/api.ts` |
| Change database schema | `backend/src/main/resources/schema.sql` + corresponding JPA entity in `model/` |
| Configure Spring profiles | `backend/src/main/resources/application-{profile}.yml` |
| Add security rules | `backend/.../config/SecurityConfig.java` |
| Change MCP server config | `backend/src/main/resources/application-mcp-example.yml` |
| Add Python packages to sandbox | `sandbox/Dockerfile` |
| Write backend tests | `backend/src/test/java/ai/mymanus/` — mirror main package structure |
| Write frontend tests | `frontend/src/components/__tests__/` or `frontend/src/services/__tests__/` |

## Dependency Flow

```
User Browser
    |
    v
[Frontend SPA]  ── REST (Axios) ──>  [Spring Boot Backend]
    |                                       |
    +--- WebSocket (STOMP) <───────────────+
                                            |
                          +─────────────────+─────────────────+
                          |                 |                 |
                          v                 v                 v
                    [PostgreSQL]    [Anthropic API]    [Docker Sandbox]
                    (sessions,      (Claude LLM)      (Python execution,
                     events,                           file ops, browser)
                     messages)
                                            |
                                            v
                                    [MCP Servers]
                                    (optional, dynamic
                                     tool discovery)
```

Within the backend, the dependency flow is:
```
Controller -> Service -> Repository -> Database
                |
                +-> SandboxExecutor -> Docker containers
                +-> AnthropicService -> Anthropic API
                +-> ToolRegistry -> Tool implementations
                +-> EventService -> Event persistence
                +-> SimpMessagingTemplate -> WebSocket
```
