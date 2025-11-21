# Agent Implementation Guide

## CodeAct Architecture Overview

The agent writes Python code to solve tasks. Each code block builds on previous executions with persistent state.

### Core Loop Pattern
1. User provides task
2. Agent thinks and generates Python code
3. Execute code in Docker sandbox
4. Capture output as observation
5. Agent reflects on results
6. Repeat until task complete (max 20 iterations)

## Implementation Structure

### Main Components
- **CodeActAgentService**: Orchestrates the loop
- **PythonSandboxExecutor**: Runs code safely
- **AgentStateRepository**: PostgreSQL persistence
- **PromptBuilder**: Constructs LLM prompts
- **CodeParser**: Extracts code from responses

### Database Schema
```sql
-- Agent state table with JSONB for flexibility
CREATE TABLE agent_states (
    id UUID PRIMARY KEY,
    session_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    execution_context JSONB,
    metadata JSONB
);

-- Messages table for conversation history
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    state_id UUID REFERENCES agent_states(id),
    role VARCHAR(20),
    content TEXT,
    timestamp TIMESTAMP
);

-- Tool executions for audit trail
CREATE TABLE tool_executions (
    id BIGSERIAL PRIMARY KEY,
    state_id UUID REFERENCES agent_states(id),
    tool_name VARCHAR(100),
    parameters JSONB,
    result JSONB,
    status VARCHAR(20),
    duration_ms INTEGER
);
```

## System Prompt Template

Structure the prompt to guide code generation:

```
You solve problems by writing Python code.

Available tools as Python functions:
{tool_descriptions}

Execution rules:
- Variables persist between code blocks
- Use print() to show results
- Handle errors with try/except
- Maximum 30 seconds per execution

Current variables in memory:
{execution_context}

To execute code, wrap it in:
<execute>
# your code here
</execute>
```

## Sandbox Execution Pattern

### Docker Container Setup
- Base: Ubuntu 22.04
- Python 3.11 pre-installed
- Resource limits: 512MB RAM, 50% CPU
- Network: Isolated by default
- Timeout: 30 seconds max

### Execution Flow
1. Create ephemeral container
2. Inject previous state as Python variables
3. Add tool functions to namespace
4. Execute user code
5. Capture stdout/stderr
6. Extract new variable state
7. Destroy container

### State Preservation
Variables persist between executions:
- Serialize Python objects to JSON
- Store in PostgreSQL JSONB column
- Restore at start of next execution
- Handle non-serializable objects gracefully

## Tool Integration

Tools are Python functions available in sandbox:

### Tool Registry Pattern
- Each tool is a Spring @Component
- Implements Tool interface
- Provides Python signature
- Returns JSON-serializable results

### Making Tools Available
Generate Python function stubs that call back to Java:
```python
def search_web(query, max_results=5):
    return _execute_tool('search_web', locals())
```

## Error Handling

### Graceful Recovery
- Parse Python exceptions
- Suggest fixes to agent
- Retry with corrected code
- Track failure patterns

### Common Patterns
- NameError → Define variable first
- ModuleNotFoundError → Install package
- TimeoutError → Optimize approach
- SyntaxError → Fix code structure

## WebSocket Integration

Send real-time updates to frontend:

### Event Types
- `status`: thinking, executing, done
- `code`: Code being executed
- `output`: Stdout/stderr streams
- `tool`: Tool execution updates
- `error`: Execution failures

### Message Flow
1. Agent starts thinking → Send status update
2. Code generated → Send code block
3. Execution starts → Send executing status
4. Output produced → Stream stdout
5. Execution complete → Send result

## Testing Strategy

### Unit Tests
- Code parsing logic
- State serialization
- Tool registration
- Error recovery

### Integration Tests
- Full agent loop
- Multi-turn conversations
- State persistence
- Tool execution

### Security Tests
- Code injection attempts
- Resource exhaustion
- Network escape attempts
- Path traversal

## Performance Considerations

### Optimization Points
- Pre-warm Docker containers
- Cache tool descriptions
- Batch database writes
- Stream responses incrementally

### Monitoring Metrics
- Average iterations per task
- Code execution success rate
- Tool usage frequency
- Token consumption
- Response latencies

## Multi-Agent Pattern (Future)

### Agent Roles
- **Planner**: Decomposes tasks
- **Executor**: Runs code
- **Verifier**: Validates results
- **Coordinator**: Manages flow

### Communication
- Shared PostgreSQL state
- Message passing via events
- Task queue for distribution

## Best Practices

1. **Always validate code** before execution
2. **Log every action** for debugging
3. **Set resource limits** strictly
4. **Handle timeouts** gracefully
5. **Preserve state** between executions
6. **Stream output** for responsiveness
7. **Test security** thoroughly

## Common Pitfalls

- Not preserving variables between blocks
- Forgetting to handle non-serializable objects
- Missing timeout handling
- Inadequate error recovery
- Poor prompt engineering
- Insufficient logging

## Next Steps

1. Implement basic loop with single tool
2. Add state persistence
3. Integrate WebSocket updates
4. Expand tool library
5. Add multi-agent support