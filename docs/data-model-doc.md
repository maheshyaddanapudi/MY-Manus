# Data Model

## Entity Relationship Diagram

```
                    ┌─────────────────┐
                    │   AgentState    │ ← Root aggregate
                    │   (agent_states)│
                    ├─────────────────┤
                    │ id: UUID (PK)   │
                    │ sessionId: str  │ UNIQUE
                    │ status: enum    │
                    │ iteration: int  │
                    │ executionContext │ JSON
                    │ metadata        │ JSON
                    └────────┬────────┘
                             │ 1
              ┌──────────────┼──────────────┐
              │ N            │ N            │ N
     ┌────────▼───────┐ ┌───▼──────┐ ┌────▼──────────┐
     │    Message     │ │  Event   │ │ToolExecution  │
     │   (messages)   │ │ (events) │ │(tool_executions)│
     ├────────────────┤ ├──────────┤ ├───────────────┤
     │ id: BIGINT (PK)│ │ id: UUID │ │ id: BIGINT    │
     │ state_id: FK   │ │ state_id │ │ state_id: FK  │
     │ role: enum     │ │ type: enum│ │ toolName: str │
     │ content: TEXT  │ │ iteration│ │ parameters    │ JSON
     │ timestamp      │ │ sequence │ │ result        │ JSON
     └────────────────┘ │ content  │ │ status: enum  │
                        │ data     │ JSON │ durationMs│
                        │ durationMs│ │ timestamp    │
                        │ success  │ └───────────────┘
                        │ error    │
                        └──────────┘

     ┌────────────────┐      1    ┌───────────────┐
     │   Document     │ ◄────────│ DocumentChunk │
     │  (documents)   │      N   │(document_chunks)│
     ├────────────────┤          ├───────────────┤
     │ id: BIGINT (PK)│          │ id: BIGINT    │
     │ sessionId: str │          │ document_id:FK│
     │ filename: str  │          │ chunkIndex    │
     │ content: TEXT  │          │ content: TEXT │
     │ metadata       │ JSON     │ embedding     │ JSON
     │ uploadedAt     │          │ metadata      │ JSON
     └────────────────┘          └───────────────┘

     ┌────────────────┐  ┌──────────────┐  ┌──────────────┐
     │  ConsoleLog    │  │NetworkRequest│  │ Notification │
     │(console_logs)  │  │(network_     │  │(notifications)│
     ├────────────────┤  │ requests)    │  ├──────────────┤
     │ sessionId: str │  ├──────────────┤  │ userId: str  │
     │ level: enum    │  │ sessionId    │  │ sessionId    │
     │ message: TEXT  │  │ method: str  │  │ type: enum   │
     │ timestamp      │  │ url: TEXT    │  │ priority:enum│
     └────────────────┘  │ status: int  │  │ isRead: bool │
                         │ reqHeaders   │ JSON │ title/message│
                         │ respHeaders  │ JSON └──────────────┘
                         └──────────────┘
```

## Cascade Behavior

**Foreign key cascades** (enforced by `schema.sql ON DELETE CASCADE`):
- Delete `AgentState` → deletes all its `Message`, `Event`, `ToolExecution` rows

**No cascade** (keyed by `sessionId` string, no FK):
- `Document`, `ConsoleLog`, `NetworkRequest`, `Notification` are independently keyed — they survive session deletion and require separate cleanup

## State Machines

### AgentState.Status

| Status | Meaning | Transition From | Transition To |
|---|---|---|---|
| `IDLE` | Created, not yet used | (creation) | `RUNNING` |
| `RUNNING` | Agent actively executing iterations | `IDLE`, `WAITING_INPUT` | `COMPLETED`, `ERROR`, `WAITING_INPUT` |
| `WAITING_INPUT` | Paused, `message_ask_user` tool called | `RUNNING` | `RUNNING` |
| `COMPLETED` | Task finished successfully | `RUNNING` | `IDLE` (on new query) |
| `ERROR` | Execution failed | `RUNNING` | `IDLE` (on new query) |

### ToolExecution.ExecutionStatus

| Status | Meaning |
|---|---|
| `PENDING` | Queued (not currently used in practice) |
| `RUNNING` | Currently executing (set at start) |
| `SUCCESS` | Completed without error |
| `FAILED` | Threw exception or returned error |

### Notification.NotificationType

| Type | Trigger |
|---|---|
| `TASK_COMPLETED` | Agent loop finishes successfully |
| `TASK_FAILED` | Agent loop errors out |
| `AGENT_WAITING` | `message_ask_user` tool pauses agent |
| `PLAN_ADJUSTED` | Todo.md plan is modified |
| `TOOL_ERROR` | Individual tool execution fails |
| `SYSTEM` | System-level notification |
| `INFO` | General informational |

## Event Stream Ordering

Events are the immutable audit log of everything that happens in a session.

### Event.EventType Sequence (per iteration)

```
USER_MESSAGE (iteration 0, sequence 0)    ← user's original query
  │
  ├─ AGENT_THOUGHT (iteration 1, sequence 0)  ← LLM reasoning
  ├─ AGENT_ACTION  (iteration 1, sequence 1)  ← code to execute
  ├─ OBSERVATION    (iteration 1, sequence 2)  ← execution result
  │
  ├─ AGENT_THOUGHT (iteration 2, sequence 0)  ← next reasoning
  ├─ AGENT_ACTION  (iteration 2, sequence 1)  ← next code
  ├─ OBSERVATION    (iteration 2, sequence 2)  ← next result
  │
  └─ AGENT_RESPONSE (iteration 3, sequence 0) ← final answer
```

Sequence numbers are per-iteration (reset to 0 each iteration). Events are ordered by `(iteration ASC, sequence ASC)`.

Additional event types: `SYSTEM` (status updates, warnings), `ERROR` (failure at any point).

## JSON Columns

All JSON columns use `@Column(columnDefinition = "json")` with `@Convert(converter = JsonMapConverter.class)`.

| Entity | Field | Contents |
|---|---|---|
| `AgentState` | `executionContext` | Python variable names and serialized values from last execution |
| `AgentState` | `metadata` | Arbitrary session metadata (extensible key-value) |
| `Event` | `data` | Event-specific payload: `codeLength`, `variables`, `exitCode`, `durationMs`, etc. |
| `ToolExecution` | `parameters` | Tool input parameters map |
| `ToolExecution` | `result` | Tool output result map |
| `DocumentChunk` | `embedding` | Vector embedding as double array (type: `double[]`, converter: `JsonMapConverter`) |
| `DocumentChunk` | `metadata` | Chunk-level metadata (source file, position) |
| `Document` | `metadata` | Document-level metadata (author, date, file type) |
| `NetworkRequest` | `requestHeaders` / `responseHeaders` | HTTP header maps |

### JsonMapConverter Behavior

```java
// Write: Map → JSON string
convertToDatabaseColumn(map) → objectMapper.writeValueAsString(map)

// Read: JSON string → Map (two-pass)
convertToEntityAttribute(dbValue)
  1. Try: objectMapper.readValue(dbValue, Map.class)
  2. Catch: strip surrounding quotes, unescape, try again
  3. Catch: return empty HashMap (never null, never throws)
```

The two-pass read handles the H2 vs PostgreSQL difference: H2 may store JSON as a quoted string (`"\"{ ... }\""`) while PostgreSQL stores it as a proper JSON value.

## Known Schema/Entity Discrepancies

These mismatches exist between `schema.sql` (H2 DDL) and the Java entities:

1. **Document/DocumentChunk PK type**: schema uses `UUID`, entities use `Long` with `IDENTITY`
2. **NetworkRequest PK type**: schema uses `UUID`, entity uses `Long`
3. **ToolExecution column names**: entity maps `state_id` but schema defines `agent_state_id`; entity has `status` enum but schema has `success BOOLEAN`
4. **DocumentChunk.embedding**: entity declares `double[]` with `JsonMapConverter` (typed for `Map`); schema defines it as `BINARY`

These discrepancies work in practice because Hibernate's `ddl-auto=update` (production) generates tables from entities, while dev uses `schema.sql` directly. The two schemas have drifted.
