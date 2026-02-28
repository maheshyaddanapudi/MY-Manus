# Flow: Agent Loop (Chat Request → Task Completion)

The primary user-facing flow. A user sends a message, and the agent autonomously solves the task through iterative code execution.

## Entry Point

`AgentController.chat()` → `POST /api/agent/chat`
- Request: `ChatRequest { sessionId?: string, message: string }`
- Response: `ChatResponse { sessionId, message, status }` (returns immediately)

## Sequence

### Phase 1: Request Setup

```
AgentController.chat(request)
  │
  ├─ sessionId = request.sessionId ?? UUID.randomUUID()
  ├─ agentService.processQuery(sessionId, message)  // runs on calling thread
  └─ return ChatResponse(sessionId, response, "completed")
```

`CodeActAgentService.processQuery()`:
1. `stateService.getOrCreateSession(sessionId)` — creates `AgentState` if absent (status=IDLE)
2. `stateService.addMessage(sessionId, USER, message)` — persists to `messages` table
3. `stateService.autoGenerateTitle(sessionId, message)` — sets title from first 50 chars if still "New Conversation"
4. `eventService.appendUserMessage(sessionId, message, iteration=0)` — writes `USER_MESSAGE` event
5. WebSocket → `{ type: "status", content: "thinking" }` to `/topic/agent/{sessionId}`

### Phase 2: Agent Loop (`executeAgentLoop`)

Bounded by `agent.max-iterations` (default 20). Each iteration:

```
┌─────────────────────────────────────────────────────────┐
│ ITERATION N                                              │
│                                                          │
│  1. Send "thinking" status event (iteration N)          │
│  2. Build system prompt (tool descriptions + context)    │
│  3. Call Claude via anthropicService.generateStream()     │
│     ├─ Each chunk → WebSocket "thought_chunk" event      │
│     └─ .blockLast() waits for full response              │
│  4. Append AGENT_THOUGHT event to DB                     │
│  5. Extract <execute>...</execute> code blocks           │
│                                                          │
│  ┌─ NO CODE BLOCKS (task complete) ──────────────────┐  │
│  │  → validateTodoCompletion()                        │  │
│  │  → Append AGENT_RESPONSE event                     │  │
│  │  → Generate summary via second Claude stream       │  │
│  │  → WebSocket "message_chunk" then "message" events │  │
│  │  → BREAK loop                                      │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  ┌─ CODE BLOCKS FOUND ───────────────────────────────┐  │
│  │  → validateSyntax(code) via python3 ast.parse()    │  │
│  │    └─ On failure: append OBSERVATION, CONTINUE      │  │
│  │  → checkSafety(code) via regex checks              │  │
│  │    └─ On failure: append OBSERVATION, CONTINUE      │  │
│  │  → Append AGENT_ACTION event                       │  │
│  │  → WebSocket "code" + "executing" events           │  │
│  │  → sandboxExecutor.execute(sessionId, code, ctx)   │  │
│  │  → Merge result.variables into executionContext    │  │
│  │  → Append OBSERVATION event (stdout/stderr/vars)   │  │
│  │  → WebSocket "output" event (+ "error" if stderr) │  │
│  │  → CONTINUE to next iteration                      │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  userMessage for next iteration =                        │
│    "The code executed with result: " + lastObservation   │
└─────────────────────────────────────────────────────────┘
```

### Phase 3: Completion

After loop exits:
1. `stateService.addMessage(sessionId, ASSISTANT, response)` — persists final response
2. WebSocket → `{ type: "status", content: "done" }`
3. Return accumulated response string to controller

On error at any point:
1. `eventService.appendError(sessionId, error, data, iteration)`
2. WebSocket → `{ type: "error", content: errorMessage }`
3. Rethrow as `RuntimeException`

## Sandbox Execution Detail

When `sandboxExecutor.execute()` is called:

### Docker Mode (production)

```
PythonSandboxExecutor.execute(sessionId, code, previousState)
  │
  ├─ getOrCreateContainer(sessionId)
  │   └─ Docker: create manus-sandbox-{sessionId}, user=ubuntu,
  │      workdir=/home/ubuntu/workspace/{sessionId}, cmd=sleep infinity,
  │      memory=512MB, cpu=50%, network=none
  │
  ├─ buildExecutionScript(code, previousState)
  │   ├─ import json, sys, os, uuid, traceback, pickle
  │   ├─ Restore state from .python_state.pkl if exists
  │   ├─ Define _execute_tool() RPC function
  │   ├─ toolRegistry.generatePythonBindings()  ← all tool stubs
  │   ├─ Inject previousState variables via json.loads()
  │   ├─ User code in try/except
  │   ├─ Save state to .python_state.pkl
  │   └─ Print STATE:{json} for Java to capture
  │
  ├─ writeCodeToContainer(containerId, code.py)
  │
  └─ executeInContainer(containerId, sessionId)
      ├─ docker exec -i python3.11 code.py
      ├─ stdout thread: intercepts __TOOL_REQUEST__ lines
      │   └─ ToolRpcHandler.handleToolRequest()
      │       ├─ toolRegistry.getTool(name).execute(params)
      │       ├─ stateService.recordToolExecution(...)
      │       └─ return __TOOL_RESPONSE__{json}__END__
      ├─ stderr thread: buffers errors
      ├─ process.waitFor(30s) → forced kill on timeout
      └─ parseStateFromOutput → ExecutionResult
```

### Tool RPC Protocol

Python-side tool calls use a synchronous stdin/stdout protocol:

```
Python:  print("__TOOL_REQUEST__" + json + "__END__")
         response = input()  # blocks until Java responds
Java:    reads __TOOL_REQUEST__, dispatches tool, writes __TOOL_RESPONSE__
Python:  parses __TOOL_RESPONSE__, returns result dict
```

## WebSocket Event Timeline (frontend perspective)

For a single 2-iteration session:

```
← { type: "status",        content: "thinking",   metadata: { iteration: 1 } }
← { type: "thought_chunk", content: "Let me..." }
← { type: "thought_chunk", content: " analyze..." }
← { type: "thought",       content: "Let me analyze...", metadata: { complete: true } }
← { type: "code",          content: "print('hello')" }
← { type: "status",        content: "executing" }
← { type: "output",        content: "hello\n" }
← { type: "status",        content: "thinking",   metadata: { iteration: 2 } }
← { type: "thought_chunk", content: "The task..." }
← { type: "thought",       content: "The task is complete.", metadata: { complete: true } }
← { type: "message_chunk", content: "I've completed..." }
← { type: "message",       content: "I've completed the task.", metadata: { final: true } }
← { type: "status",        content: "done" }
```

## Frontend State Updates (`agentStore.handleAgentEvent`)

| Event Type | Store Mutations |
|---|---|
| `thought_chunk` | Append to `thoughtBuffer`, create/update thought `Message` and `Event` record |
| `thought` (complete) | Finalize thought message and event, clear buffers |
| `code` | Set `currentCode`, append to `codeHistory`, suggest editor panel |
| `output` | Inject `<observation>` into last assistant message, append to `terminalOutput`, suggest terminal |
| `error` | Append to `terminalOutput` as stderr, set `agentStatus: 'error'` |
| `status` | Update `agentStatus`, `currentIteration`, `maxIterations` |
| `message_chunk` | Append to `messageBuffer`, create/update final `Message` |
| `message` | Finalize message content, clear buffer |

## Key Design Decisions

1. **One code block per iteration**: Even if Claude produces multiple `<execute>` blocks, only the first is executed. This enforces the CodeAct "one action per turn" principle.
2. **Validation as self-correction**: Syntax and safety failures become OBSERVATION events, not exceptions. The LLM sees the error and can correct its code on the next iteration.
3. **Dual state persistence**: Python variables survive via both `.python_state.pkl` (container-side pickle) and `AgentState.executionContext` (Java-side JSON). The pickle provides seamless cross-iteration continuity; the JSON provides API-accessible state inspection.
4. **Streaming everything**: Both thoughts and final messages stream chunk-by-chunk to the frontend, enabling real-time display even during long LLM generations.
5. **Conversation memory via JDBC**: Spring AI's `MessageWindowChatMemory` (100-message window) automatically maintains conversation context across iterations without manual prompt construction.
