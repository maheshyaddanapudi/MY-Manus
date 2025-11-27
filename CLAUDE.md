# MY-Manus - CodeAct Agent Platform

> **A production-grade Manus AI clone implementing the CodeAct paradigm where agents solve problems by writing and executing Python code in isolated Docker containers.**

## Project Status: Production-Ready

**Repository**: MY-Manus
**Architecture**: Spring Boot 3.3 + React 19 + PostgreSQL 15 + Docker
**AI Provider**: Anthropic Claude (via Spring AI)
**Feature Parity**: 100% with Manus AI + 8 additional enhancements

---

## Quick Reference

| Component | Technology | Location |
|-----------|------------|----------|
| Backend | Spring Boot 3.3 (Java 21) | `backend/` |
| Frontend | React 19 + TypeScript + Vite | `frontend/` |
| Database | PostgreSQL 15 with JSONB | `docker-compose.yml` |
| Sandbox | Ubuntu 22.04 Docker container | `sandbox/` |
| Docs | Comprehensive markdown | `docs/` |

---

## Core Architecture

### CodeAct Pattern
```
User Query → LLM generates Python code → Execute in Docker sandbox →
Capture stdout/stderr → Feed observation back to LLM → Repeat until done
```

**Key Pattern**: ONE code block executed per iteration (matches Manus AI design).

### Event Stream Architecture
Seven immutable event types provide complete transparency:
1. `USER_MESSAGE` - User input
2. `AGENT_THOUGHT` - LLM reasoning (streamed)
3. `AGENT_ACTION` - Python code to execute
4. `OBSERVATION` - Execution results
5. `AGENT_RESPONSE` - Final answer
6. `SYSTEM` - System messages
7. `ERROR` - Error details

### Database Schema (9 Entities)
- `AgentState` - Session metadata + execution context (JSONB)
- `Event` - Immutable event stream with iteration/sequence ordering
- `Message` - Chat history (Spring AI JDBC memory)
- `Document` / `DocumentChunk` - RAG knowledge base
- `ToolExecution` - Tool usage audit log
- `ConsoleLog` - Browser console logs
- `NetworkRequest` - Browser network monitoring
- `Notification` - User notifications

---

## Implemented Features

### Tool System (22 Tools)

| Category | Tools | Count |
|----------|-------|-------|
| **File Operations** | `file_read`, `file_write`, `file_list`, `file_find_by_name`, `file_find_content`, `file_replace_string` | 6 |
| **Browser Automation** | `browser_navigate`, `browser_view`, `browser_click`, `browser_input`, `browser_scroll_up/down`, `browser_press_key`, `browser_refresh` | 8 |
| **Shell** | `shell_exec` | 1 |
| **Communication** | `message_notify_user`, `message_ask_user` | 2 |
| **Utilities** | `print`, `todo`, `search_tools`, `web_search`, `data_visualization` | 5 |

### UI Panels (8 Panels + Chat)
1. **Terminal** - xterm.js real-time output
2. **Code Editor** - Monaco Editor with syntax highlighting
3. **Browser** - Screenshots + Console logs + Network requests
4. **Event Stream** - Complete execution timeline
5. **Files** - File tree navigation and viewer
6. **Replay** - Session time-travel debugging
7. **Knowledge** - RAG document upload and search
8. **Plan** - Live todo.md visualization

### Advanced Features
- **Multi-Session Management** - Unlimited concurrent sessions with auto-titles
- **Streaming LLM Responses** - Chunk-by-chunk display
- **Variable Persistence** - Python state saved in PostgreSQL JSONB
- **Container Caching** - Session-based Docker container reuse
- **Multi-Agent Orchestration** - 6 specialized agent roles
- **Observability** - Prometheus metrics integration
- **Notifications** - Browser + in-app with 7 types, 4 priorities

---

## Project Structure

```
MY-Manus/
├── backend/                          # Spring Boot application
│   ├── src/main/java/ai/mymanus/
│   │   ├── config/                   # Configuration classes
│   │   ├── controller/               # REST API endpoints (9 controllers)
│   │   ├── service/                  # Business logic (12+ services)
│   │   ├── tool/                     # Tool implementations (22 tools)
│   │   ├── model/                    # JPA entities (9 entities)
│   │   ├── repository/               # Spring Data repositories
│   │   ├── multiagent/               # Multi-agent orchestration
│   │   └── dto/                      # Data transfer objects
│   └── src/main/resources/
│       ├── application.yml           # Main configuration
│       └── application-dev.yml       # Development profile
├── frontend/                         # React application
│   ├── src/
│   │   ├── components/               # UI components (30+)
│   │   │   ├── Chat/                 # ChatPanel, MessageList, etc.
│   │   │   ├── Terminal/             # TerminalPanel (xterm.js)
│   │   │   ├── Editor/               # EditorPanel (Monaco)
│   │   │   ├── Browser/              # BrowserPanel, ConsoleViewer, NetworkViewer
│   │   │   ├── EventStream/          # EventStreamPanel, EventItem
│   │   │   ├── FileTree/             # FileTreePanel, FileViewer
│   │   │   ├── Replay/               # SessionReplayPanel
│   │   │   ├── Knowledge/            # DocumentPanel
│   │   │   ├── Plan/                 # PlanPanel
│   │   │   └── Notifications/        # NotificationBell, NotificationPanel
│   │   ├── stores/                   # Zustand state management
│   │   ├── services/                 # API and WebSocket services
│   │   └── types/                    # TypeScript interfaces
│   └── package.json
├── sandbox/                          # Docker sandbox environment
│   └── Dockerfile                    # Ubuntu 22.04 + Python 3.11 + Node 22.13
├── docs/                             # Documentation (19 markdown files)
│   ├── architecture/                 # Architecture docs
│   ├── guides/                       # Implementation guides
│   ├── project/                      # Project status and analysis
│   └── research/                     # Research reports
├── docker-compose.yml                # Local development setup
└── CLAUDE.md                         # This file
```

---

## Commands for Claude Code

### VERIFY
Run a comprehensive verification of the implementation against documentation:

```
1. Check all 22 tools are implemented in backend/src/main/java/ai/mymanus/tool/
2. Verify all 9 database entities exist in backend/src/main/java/ai/mymanus/model/
3. Confirm all 9 controllers in backend/src/main/java/ai/mymanus/controller/
4. Validate all 30+ frontend components exist
5. Check all WebSocket topics are implemented
6. Verify API endpoints match docs/guides/API_REFERENCE.md
7. Confirm tests exist for critical paths
```

### AUDIT
Perform an Architect council review:

```
1. Review code quality and patterns in services/
2. Check security measures in sandbox execution
3. Verify database schema matches documentation
4. Audit API contracts for consistency
5. Review error handling completeness
6. Check logging and observability
7. Validate configuration management
```

### GAP-ANALYSIS
Identify and fix any implementation gaps:

```
1. Compare docs/project/FINAL_SUMMARY.md claims vs actual code
2. Check docs/project/DIFFERENTIAL_ANALYSIS.md features exist
3. Verify all API endpoints in docs/guides/API_REFERENCE.md work
4. Confirm test coverage for critical paths
5. Validate documentation accuracy
```

### BUILD
Build and run the project:

```bash
# Build sandbox image
cd sandbox && docker build -t mymanus-sandbox:latest .

# Start all services
cd .. && export ANTHROPIC_API_KEY=your-key
docker-compose up

# Access
# Frontend: http://localhost:3000
# Backend: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
```

### TEST
Run the test suite:

```bash
# Backend tests
cd backend && ./mvnw test

# Frontend tests
cd frontend && npm test

# Integration tests
cd backend && ./mvnw test -Dtest=*IntegrationTest
```

---

## Verification Checklist

Use this checklist to verify implementation completeness:

### Backend Services
- [ ] `CodeActAgentService` - Main agent loop with streaming
- [ ] `AnthropicService` - LLM integration with Claude
- [ ] `PythonSandboxExecutor` - Docker sandbox execution
- [ ] `EventService` - Event stream management
- [ ] `AgentStateService` - Session state persistence
- [ ] `PromptBuilder` - System prompt construction
- [ ] `PythonValidationService` - Code syntax validation
- [ ] `TodoMdParser` / `TodoMdWatcher` - Plan visualization
- [ ] `MessageClassifier` - Multi-turn message classification
- [ ] `RAGService` - Knowledge base search
- [ ] `SessionReplayService` - Time-travel debugging
- [ ] `NotificationService` - User notifications

### Controllers
- [ ] `AgentController` - `/api/agent/*`
- [ ] `MultiAgentController` - `/api/multi-agent/*`
- [ ] `WebSocketController` - STOMP messaging
- [ ] `BrowserMonitorController` - `/api/browser/*`
- [ ] `SandboxController` - `/api/sandbox/*`
- [ ] `DocumentController` - `/api/documents/*`
- [ ] `SessionReplayController` - `/api/replay/*`
- [ ] `NotificationController` - `/api/notifications/*`
- [ ] `PlanController` - `/api/plan/*`

### Tools (22 Total)
**File Tools (6)**:
- [ ] `FileReadTool`, `FileWriteTool`, `FileListTool`
- [ ] `FileFindByNameTool`, `FileFindContentTool`, `FileReplaceStringTool`

**Browser Tools (8)**:
- [ ] `BrowserNavigateTool`, `BrowserViewTool`, `BrowserClickTool`
- [ ] `BrowserInputTool`, `BrowserScrollUpTool`, `BrowserScrollDownTool`
- [ ] `BrowserPressKeyTool`, `BrowserRefreshTool`

**Other Tools (8)**:
- [ ] `ShellExecTool`
- [ ] `MessageNotifyUserTool`, `MessageAskUserTool`
- [ ] `PrintTool`, `TodoTool`, `SearchToolsTool`
- [ ] `WebSearchTool`, `DataVisualizationTool`

### Frontend Components
- [ ] `ChatPanel`, `ChatInput`, `MessageItem`, `MessageList`
- [ ] `TerminalPanel` (xterm.js integration)
- [ ] `EditorPanel` (Monaco Editor integration)
- [ ] `BrowserPanel`, `SnapshotViewer`, `ConsoleViewer`, `NetworkViewer`
- [ ] `EventStreamPanel`, `EventItem`
- [ ] `FileTreePanel`, `FileNode`, `FileViewer`
- [ ] `SessionReplayPanel`
- [ ] `DocumentPanel`
- [ ] `PlanPanel`
- [ ] `NotificationBell`, `NotificationPanel`
- [ ] `ConversationList`
- [ ] `MainLayout`, `Header`

### Database Entities
- [ ] `AgentState` - Session with JSONB executionContext
- [ ] `Event` - Immutable event log with type, iteration, sequence
- [ ] `Message` - Chat messages with role and content
- [ ] `Document` - RAG documents with metadata
- [ ] `DocumentChunk` - Vector embeddings for semantic search
- [ ] `ToolExecution` - Tool usage tracking
- [ ] `ConsoleLog` - Browser console entries
- [ ] `NetworkRequest` - Browser network monitoring
- [ ] `Notification` - User notifications with priority

### WebSocket Topics
- [ ] `/topic/agent/{sessionId}` - Agent events
- [ ] `/topic/notifications/{sessionId}` - Notifications
- [ ] `/topic/browser/{sessionId}` - Browser updates
- [ ] `/topic/plan/{sessionId}` - Plan updates
- [ ] `/topic/terminal/{sessionId}` - Terminal output

---

## Architecture Decision Records

### ADR-001: CodeAct vs Function Calling
**Decision**: Implement CodeAct pattern (agents write Python code)
**Rationale**: Matches Manus AI architecture; provides more flexibility than JSON function calling
**Status**: Implemented

### ADR-002: Event Stream vs Message History
**Decision**: Use immutable event stream with 7 types
**Rationale**: Provides complete audit trail and enables session replay
**Status**: Implemented

### ADR-003: Container Caching Strategy
**Decision**: Cache Docker containers per session
**Rationale**: 5-10x performance improvement after first execution
**Status**: Implemented

### ADR-004: Spring AI for LLM Integration
**Decision**: Use Spring AI framework for Anthropic integration
**Rationale**: Native MCP support, JDBC chat memory, production-ready
**Status**: Implemented

### ADR-005: Hybrid Tool System
**Decision**: 22 pre-loaded core tools + dynamic MCP discovery
**Rationale**: Core tools always available, MCP tools discovered on-demand
**Status**: Implemented

---

## Configuration Reference

### Backend (`application.yml`)
```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-sonnet-4-20250514
          temperature: 0.7
          max-tokens: 4096

sandbox:
  mode: docker  # or 'host' for development
  docker:
    image: mymanus-sandbox:latest
    memory-limit: 512MB
    cpu-quota: 50%
    timeout-seconds: 30
    network-mode: none

agent:
  max-iterations: 20
  code-execution-timeout: 30000
```

### Frontend (`vite.config.ts`)
```typescript
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
```

---

## Security Measures

### Sandbox Isolation (5 Layers)
1. **User Isolation**: Non-root user `ubuntu` in container
2. **Network Isolation**: `network-mode: none` by default
3. **Resource Limits**: 512MB RAM, 50% CPU, 30s timeout
4. **Filesystem Isolation**: Only `/workspace` accessible
5. **Symlink Protection**: Prevents directory traversal

### Input Validation
- Path validation for all file operations
- Code syntax validation before execution
- Tool parameter sanitization

---

## Performance Characteristics

| Operation | Time |
|-----------|------|
| First code execution | 500-1000ms (container creation) |
| Cached code execution | 100-200ms |
| LLM response | 1-5s (depends on query) |
| WebSocket latency | <50ms |
| Session switch | <500ms |

---

## Documentation Index

| Document | Purpose |
|----------|---------|
| `docs/README.md` | Project overview |
| `docs/architecture/ARCHITECTURE.md` | System architecture |
| `docs/architecture/DATABASE_SCHEMA.md` | Entity relationships |
| `docs/architecture/EVENT_STREAM_GUIDE.md` | Event types and patterns |
| `docs/architecture/FRONTEND_ARCHITECTURE.md` | React component structure |
| `docs/architecture/SANDBOX_ARCHITECTURE.md` | Docker sandbox details |
| `docs/guides/AGENT_GUIDE.md` | Agent loop implementation |
| `docs/guides/TOOLS_GUIDE.md` | Tool development guide |
| `docs/guides/UI_GUIDE.md` | Frontend patterns |
| `docs/guides/SANDBOX_GUIDE.md` | Environment setup |
| `docs/guides/DEPLOYMENT.md` | Production deployment |
| `docs/guides/API_REFERENCE.md` | REST API documentation |
| `docs/guides/MULTI_TURN_SCENARIOS.md` | Conversation handling |
| `docs/guides/OBSERVABILITY.md` | Prometheus/Grafana setup |
| `docs/guides/DEVELOPMENT_GUIDE.md` | Developer onboarding |
| `docs/project/FINAL_SUMMARY.md` | Implementation summary |
| `docs/project/DIFFERENTIAL_ANALYSIS.md` | Manus AI comparison |
| `docs/research/MANUS_AI_RESEARCH_REPORT.md` | Original research |

---

## Troubleshooting

### Common Issues

**Docker container not starting**:
```bash
# Check if sandbox image exists
docker images | grep mymanus-sandbox

# Rebuild if missing
cd sandbox && docker build -t mymanus-sandbox:latest .
```

**WebSocket connection failing**:
- Ensure backend is running on port 8080
- Check CORS configuration in `application-dev.yml`
- Verify WebSocket URL in frontend config

**Code execution timeout**:
- Check `sandbox.docker.timeout-seconds` setting
- Verify container resource limits
- Review agent loop iteration count

**LLM not responding**:
- Verify `ANTHROPIC_API_KEY` is set
- Check API key quota and rate limits
- Review backend logs for error details

---

## Contributing

### Code Standards
- Java: Follow Spring Boot conventions
- TypeScript: Use strict mode, Zustand for state
- Tests: JUnit 5 for backend, Vitest for frontend
- Documentation: Keep docs in sync with code

### Pull Request Process
1. Create feature branch from main
2. Implement changes with tests
3. Update relevant documentation
4. Run full test suite
5. Submit PR with clear description

---

## License

This project is an educational implementation inspired by Manus AI. It is intended for learning purposes and demonstrates production-grade agent architecture patterns.
