# Workflow Architecture

## System Overview

MY-Manus implements the CodeAct architecture (ICML 2024) — an LLM agent that solves tasks by generating executable Python code rather than selecting from fixed action templates. The workflow has three interacting subsystems: the **agent orchestrator**, the **sandbox executor**, and the **real-time event pipeline**.

## Agent Orchestrator

### Core Loop (`CodeActAgentService`)

The orchestrator implements a bounded ReAct loop:

```
User query → [Think → Act → Observe] × N → Final response
```

**Think**: Claude generates reasoning with optional `<execute>` code blocks
**Act**: First code block is validated and executed in the sandbox
**Observe**: Execution output feeds back as the next iteration's input

The loop terminates when:
1. Claude produces a response without code blocks (task complete)
2. `agent.max-iterations` (20) is reached
3. An unrecoverable error occurs

### Prompt Architecture

`PromptBuilder.buildSystemPrompt()` constructs a dynamic system prompt with four sections:

1. **Tool descriptions** — generated from `ToolRegistry.getToolDescriptions()`, listing all 25 tools with their Python signatures
2. **Network status** — whether outbound network is available (Docker: no; host: yes)
3. **Execution context** — variable names and types from previous iterations (e.g., `df: DataFrame, results: list`)
4. **RAG context** — relevant document chunks if the session has uploaded documents (via cosine similarity search)

Key prompt rules embedded in the template:
- Use pre-defined tools before raw Python
- Use `search_tools()` + `mcp_call()` for MCP tool discovery
- Wrap executable code in `<execute>...</execute>` tags
- Use `todo()` tool for multi-step tasks (3+ steps)
- One code block per response

### Conversation Memory

Spring AI's `MessageWindowChatMemory` maintains a 100-message sliding window, persisted to the database via `JdbcChatMemoryRepository`. This is invisible to `CodeActAgentService` — the `ChatClient` advisor automatically injects history into each Claude call.

## Sandbox Executor

### Execution Script Assembly

Each code execution compiles a complete Python script:

```python
# Section 1: Standard imports
import json, sys, os, uuid, traceback, pickle

# Section 2: State restoration (from previous iteration)
if os.path.exists('.python_state.pkl'):
    with open('.python_state.pkl', 'rb') as f:
        _state = pickle.load(f)
    globals().update({k: v for k, v in _state.items() if ...})

# Section 3: Tool RPC function
def _execute_tool(tool_name, params):
    print("__TOOL_REQUEST__" + json.dumps(request) + "__END__")
    response = input()  # blocks for Java response
    return parsed_result

# Section 4: Tool stubs (25 Python function definitions)
def file_read(sessionId: str, path: str): ...
def shell_exec(sessionId: str, command: str, timeout: int = 30): ...
# ... all tools from ToolRegistry.generatePythonBindings()

# Section 5: Previous state injection (from Java-side context)
var1 = json.loads('...')
var2 = json.loads('...')

# Section 6: User code (from LLM output)
try:
    <extracted code>
except Exception as e:
    print(f"ERROR: {e}")

# Section 7: State capture
_state = {k: v for k, v in globals().items() if serializable(v)}
pickle.dump(_state, open('.python_state.pkl', 'wb'))
print(f"STATE:{json.dumps(_state, default=str)}")
```

### Dual State Persistence

Python variables survive between iterations via two parallel mechanisms:

| Mechanism | Location | Purpose |
|---|---|---|
| `.python_state.pkl` | Container/workspace filesystem | Python-native state via pickle; handles complex types (DataFrames, custom objects) |
| `AgentState.executionContext` | PostgreSQL JSON column | Java-accessible state snapshot; used for API introspection and prompt context |

On each iteration: pickle is loaded first (Section 2), Java state is overlaid (Section 5), then state is saved back to both (Sections 6-7).

### Container Lifecycle (Docker mode)

```
First code execution for session:
  createContainer("manus-sandbox-{sessionId}")
    ├─ Image: mymanus-sandbox:latest
    ├─ User: ubuntu
    ├─ Workdir: /home/ubuntu/workspace/{sessionId}
    ├─ Cmd: sleep infinity (keeps container alive)
    ├─ Memory: 512MB, CPU: 50%, Network: none
    └─ Cache in sessionContainers map

Subsequent executions:
  getOrCreateContainer() → reuse cached container
  (container persists across iterations for state continuity)

Session deletion:
  destroySessionContainer() → stop + remove container

JVM shutdown:
  @PreDestroy cleanupAllContainers() → destroy all
```

## Real-Time Event Pipeline

### Three WebSocket Topics

| Topic | Source | Content |
|---|---|---|
| `/topic/agent/{sessionId}` | `CodeActAgentService` via `SimpMessagingTemplate` | Agent events: thought chunks, code, output, status, errors, messages |
| `/topic/plan/{sessionId}` | `TodoMdWatcher` via `SimpMessagingTemplate` | Parsed `todo.md` structure updates |
| `/topic/notifications/{sessionId}` | `NotificationService` via `SimpMessagingTemplate` | Task completion, failures, agent waiting |

### Event Types and Their Purpose

| Wire Type | DB Event Type | Direction | Purpose |
|---|---|---|---|
| `status` | `SYSTEM` | Backend → Frontend | Agent state transitions (thinking, executing, done) |
| `thought_chunk` | (accumulated) | Backend → Frontend | Streaming LLM reasoning text |
| `thought` | `AGENT_THOUGHT` | Backend → Frontend | Complete reasoning (or chunk finalization) |
| `code` | `AGENT_ACTION` | Backend → Frontend | Code block to be executed |
| `output` | `OBSERVATION` | Backend → Frontend | Execution stdout |
| `error` | `ERROR` | Backend → Frontend | Execution stderr or system error |
| `message_chunk` | (accumulated) | Backend → Frontend | Streaming final response |
| `message` | `AGENT_RESPONSE` | Backend → Frontend | Complete final response |
| `connected` | — | Backend → Frontend | WebSocket subscription confirmed |
| `warning` | — | Backend → Frontend | Non-fatal warning |

### Frontend Chunk Buffering

The Zustand store accumulates streaming chunks into coherent messages:

```
thought_chunk → append to thoughtBuffer → update/create thought Message
thought_chunk → append to thoughtBuffer → update thought Message
thought (complete=true) → finalize Message, clear thoughtBuffer

message_chunk → append to messageBuffer → update/create final Message
message_chunk → append to messageBuffer → update final Message
message → finalize Message, clear messageBuffer
```

State variables involved: `thoughtBuffer`, `lastThoughtMessageId`, `lastEventId`, `messageBuffer`, `lastMessageId`. Modifying any of these without understanding the full pipeline causes duplicate or lost messages.

## Multi-Agent Orchestration

`MultiAgentOrchestrator` supports running multiple agent configurations against the same task:

### Sequential Mode
Agents execute one after another. Each agent's output feeds into the next as context.

### Parallel Mode
Agents execute concurrently via `CompletableFuture.allOf()`. Results are merged after all complete.

### Agent Roles (from `AgentRole` enum)
`RESEARCHER`, `CODER`, `REVIEWER`, `PLANNER`, `EXECUTOR`

Each role can be configured with: `agentId`, `role`, `llmModel`, `systemPrompt`, `maxIterations`.

## Background Processes

| Process | Trigger | Mechanism |
|---|---|---|
| Todo.md watching | `POST /api/plan/{id}/watch` | NIO `WatchService` in `CompletableFuture.runAsync()` thread per session |
| Notification cleanup | Daily 2 AM | `@Scheduled(cron)` — deletes read notifications older than 30 days |
| Container cleanup | JVM shutdown | `@PreDestroy` on `PythonSandboxExecutor` |

## Error Recovery Patterns

| Failure | Recovery |
|---|---|
| Python syntax error | Becomes OBSERVATION; LLM self-corrects on next iteration |
| Safety check failure | Becomes OBSERVATION; LLM rewrites code |
| Sandbox timeout (30s) | Container force-killed; error returned as OBSERVATION |
| Anthropic API failure | `RuntimeException` → ERROR event → agent loop aborts |
| Tool execution error | `ToolRpcHandler` returns JSON error → Python raises → stderr |
| WebSocket send failure | Caught and logged; agent loop continues without interruption |
| Container not running | Recreated automatically by `getOrCreateContainer()` |
| Embedding API failure | Falls back to TF-IDF (no external API required) |
