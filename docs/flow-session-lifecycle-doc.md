# Flow: Session Lifecycle (Create → Switch → Delete)

Sessions are the top-level organizational unit. Each session has its own conversation history, event stream, execution context, sandbox container, and workspace directory.

## Session Creation

### Entry Points

1. **Explicit**: `POST /api/agent/session?sessionId=X&title=Y` → `AgentStateService.createSession()`
2. **Implicit**: `POST /api/agent/chat` with no `sessionId` → auto-generates UUID, calls `getOrCreateSession()`
3. **Frontend**: `agentStore.createNewSession(title?)` → REST call → `switchSession(newId)`

### What Happens

```
AgentStateService.createSession(sessionId, title)
  │
  ├─ Build AgentState entity:
  │   ├─ id: UUID (auto-generated)
  │   ├─ sessionId: provided or generated
  │   ├─ title: provided or "New Conversation"
  │   ├─ status: IDLE
  │   ├─ iteration: 0
  │   ├─ executionContext: {} (empty map)
  │   └─ metadata: {} (empty map)
  │
  └─ agentStateRepository.save(state)
```

No sandbox container is created until the first code execution. The workspace directory is created lazily by `FileTool.getSessionWorkspace()` on first file operation.

## Session Switching (Frontend)

```
agentStore.switchSession(sessionId)
  │
  ├─ resetSession()  ← clears messages, status, terminal, code, context
  │                     (events[] are NOT cleared — persist for Event Stream)
  │
  ├─ set currentSessionId = sessionId
  │
  ├─ apiService.getMessages(sessionId)     → hydrate messages[]
  ├─ apiService.getExecutionContext(sessionId) → hydrate executionContext
  └─ apiService.getEventStream(sessionId)  → hydrate events[]
```

The WebSocket subscription is NOT switched here — the chat component manages WebSocket lifecycle when sending messages. On session switch, the frontend loads persisted state via REST.

## Session Listing

`GET /api/agent/sessions` → `AgentStateService.listAllSessions()`

Returns all `AgentState` records sorted by `updatedAt DESC`, each with:
- `sessionId`, `title`, `createdAt`, `updatedAt`
- `messageCount` (computed per-session via `getMessages().size()`)

## Session Title Management

- **Auto-title**: `AgentStateService.autoGenerateTitle(sessionId, firstMessage)` — called on first chat message. Takes first line of message, truncates to 50 chars. Only applies if title is still null or "New Conversation".
- **Manual rename**: `PUT /api/agent/session/{id}/title?title=X` → `AgentStateService.updateSessionTitle()`

## Session Deletion

### Entry Point

`DELETE /api/agent/session/{sessionId}` → `CodeActAgentService.clearSession()`

### Cleanup Chain

```
CodeActAgentService.clearSession(sessionId)
  │
  ├─ sandboxExecutor.destroySessionContainer(sessionId)
  │   ├─ [Docker] Stop and remove container manus-sandbox-{sessionId}
  │   │           Remove from sessionContainers cache
  │   └─ [Host]   Delete workspace directory recursively
  │
  ├─ eventService.clearEventStream(sessionId)
  │   └─ DELETE FROM events WHERE agent_state_id = ?
  │
  ├─ stateService.deleteSession(sessionId)
  │   └─ DELETE FROM agent_states WHERE session_id = ?
  │       (cascades: messages, tool_executions deleted via ON DELETE CASCADE)
  │
  └─ stateService.createSession(sessionId)  ← immediately recreates empty session
```

### Frontend Side

```
agentStore.deleteSession(sessionId)
  │
  ├─ apiService.clearSession(sessionId)
  ├─ Remove session from sessions[] array
  └─ If deleted == currentSessionId:
      └─ createNewSession()  ← auto-provision replacement
```

## State Diagram

```
  ┌──────────┐   createSession   ┌──────┐
  │  (none)  │ ───────────────→  │ IDLE │
  └──────────┘                   └──┬───┘
                                    │ processQuery()
                                    ▼
                               ┌─────────┐
                        ┌───── │ RUNNING  │ ◄──────────┐
                        │      └────┬─────┘            │
                        │           │                   │
                        │     code execution            │
                        │     completes                 │
                        │           │                   │
                   message_ask_user │           next iteration
                        │           │                   │
                        ▼           ▼                   │
              ┌──────────────┐  ┌──────────┐           │
              │WAITING_INPUT │  │ observe  │───────────┘
              └──────┬───────┘  └──────────┘
                     │
                user replies
                     │
                     ▼
               ┌──────────┐    error at    ┌───────┐
               │ RUNNING  │ ─────────────→ │ ERROR │
               └────┬─────┘   any point    └───────┘
                    │
              no more code
                    │
                    ▼
              ┌───────────┐
              │ COMPLETED │
              └───────────┘
```

## Data Cascade on Deletion

When `AgentState` is deleted, `ON DELETE CASCADE` in `schema.sql` removes:
- All `Message` rows (`state_id` FK)
- All `Event` rows (`agent_state_id` FK)
- All `ToolExecution` rows (`agent_state_id` FK)

Independently keyed data (by `session_id` string, no FK) is NOT auto-deleted:
- `Document` / `DocumentChunk` rows (knowledge base)
- `ConsoleLog` rows (browser monitoring)
- `NetworkRequest` rows (browser monitoring)
- `Notification` rows
