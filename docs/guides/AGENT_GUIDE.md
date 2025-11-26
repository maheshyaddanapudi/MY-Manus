# Agent Implementation Guide

## Overview

The Manus AI clone implements a CodeAct agent that solves problems by writing and executing Python code. The agent uses an **Event Stream Architecture** where all interactions are stored as immutable events.

## Architecture

### Core Components

The agent loop is orchestrated by these main services:

#### CodeActAgentService
**Location**: `/backend/src/main/java/ai/mymanus/service/CodeActAgentService.java`

Main orchestrator that implements the agent loop using Event Stream Architecture.

```java
@Service
@RequiredArgsConstructor
public class CodeActAgentService {
    private final AnthropicService anthropicService;
    private final SandboxExecutor sandboxExecutor;
    private final PromptBuilder promptBuilder;
    private final AgentStateService stateService;
    private final EventService eventService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${agent.max-iterations:20}")
    private Integer maxIterations;
}
```

**Key Pattern**: Executes ONE action per iteration (Manus AI design principle).

#### AnthropicService
**Location**: `/backend/src/main/java/ai/mymanus/service/AnthropicService.java`

Integrates with Claude via Spring AI ChatClient.

```java
@Service
@RequiredArgsConstructor
public class AnthropicService {
    private final ChatClient chatClient;

    public String generate(String conversationId, String systemPrompt, String userMessage) {
        return chatClient.prompt()
            .system(systemPrompt)
            .user(userMessage)
            .advisors(advisor -> advisor.param("conversationId", conversationId))
            .call()
            .content();
    }
}
```

Uses JDBC chat memory for conversation history management.

#### EventService
**Location**: `/backend/src/main/java/ai/mymanus/service/EventService.java`

Manages the event stream - the core data structure for agent execution.

**Event Types**:
- `USER_MESSAGE`: User query
- `AGENT_THOUGHT`: Agent's reasoning (LLM response)
- `AGENT_ACTION`: Code to execute
- `OBSERVATION`: Execution result (success/failure)
- `AGENT_RESPONSE`: Final answer to user
- `SYSTEM`: System messages
- `ERROR`: Errors during execution

```java
public Event appendObservation(String sessionId, String observation,
                                Map<String, Object> observationData,
                                boolean success, String error,
                                long durationMs, int iteration) {
    // Creates immutable event record
    Event event = Event.builder()
        .agentState(state)
        .type(Event.EventType.OBSERVATION)
        .iteration(iteration)
        .sequence(getNextSequence(state.getId(), iteration))
        .content(observation)
        .data(observationData)
        .success(success)
        .error(error)
        .durationMs(durationMs)
        .build();

    return eventRepository.save(event);
}
```

#### PromptBuilder
**Location**: `/backend/src/main/java/ai/mymanus/service/PromptBuilder.java`

Constructs system prompts with tool descriptions and execution context.

```java
private static final String SYSTEM_PROMPT_TEMPLATE = """
    You are a helpful AI assistant that solves problems by writing and executing Python code.

    ## Available Tools:
    %s

    ## Execution Environment:
    - Python 3.11 in Ubuntu 22.04
    - Variables persist between code blocks
    - Use print() to show results
    - Maximum 30 seconds per execution
    - Network: %s

    ## Code Execution:
    Wrap your Python code in <execute> tags:

    <execute>
    # Your Python code here
    </execute>

    ## Current Execution Context:
    Variables in memory: %s
    """;
```

Includes RAG context integration for knowledge base retrieval.

## Database Schema

### AgentState Table
**Location**: `/backend/src/main/java/ai/mymanus/model/AgentState.java`

```java
@Entity
@Table(name = "agent_states")
public class AgentState {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String sessionId;

    @Column(length = 500)
    private String title;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> executionContext;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
```

**Purpose**: Stores session state and execution variables as JSONB.

### Event Table
**Location**: `/backend/src/main/java/ai/mymanus/model/Event.java`

```java
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_state_id", nullable = false)
    private AgentState agentState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Column(nullable = false)
    private Integer iteration;

    @Column(nullable = false)
    private Integer sequence;

    @Column(columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    private Long durationMs;
    private Boolean success;

    @Column(columnDefinition = "TEXT")
    private String error;
}
```

**Purpose**: Immutable event log - primary data structure for agent execution.

### Message Table
**Location**: Spring AI Chat Memory (JDBC)

Managed automatically by Spring AI's JDBC chat memory advisor for conversation history.

## Agent Loop Flow

### Main Loop Implementation

```java
public String processQuery(String sessionId, String userQuery) {
    // 1. Ensure session exists
    stateService.getOrCreateSession(sessionId);

    // 2. Append USER_MESSAGE to event stream
    eventService.appendUserMessage(sessionId, userQuery, 0);

    // 3. Auto-generate session title
    stateService.autoGenerateTitle(sessionId, userQuery);

    // 4. Execute agent loop
    String finalResponse = executeAgentLoop(sessionId);

    // 5. Send completion event
    sendEvent(sessionId, "status", "done", Map.of("message", "Task completed"));

    return finalResponse;
}
```

### Iteration Loop

```java
private String executeAgentLoop(String sessionId) {
    Map<String, Object> executionContext = stateService.getExecutionContext(sessionId);
    int iteration = 0;

    while (iteration < maxIterations) {
        iteration++;

        // 1. Build context from event stream
        String eventStreamContext = eventService.buildEventStreamContext(sessionId);
        String systemPrompt = promptBuilder.buildSystemPrompt(executionContext, false);
        String fullContext = systemPrompt + "\n\n" + eventStreamContext;

        // 2. Generate LLM response
        String llmResponse = anthropicService.generate(sessionId, fullContext, "");

        // 3. Append AGENT_THOUGHT to event stream
        eventService.appendAgentThought(sessionId, llmResponse, iteration);

        // 4. Extract code blocks
        List<String> codeBlocks = promptBuilder.extractCodeBlocks(llmResponse);

        if (codeBlocks.isEmpty()) {
            // No code = task complete
            eventService.appendAgentResponse(sessionId, llmResponse, iteration);
            break;
        }

        // 5. Execute ONLY the FIRST code block (Manus AI pattern)
        String code = codeBlocks.get(0);

        // 6. Append AGENT_ACTION to event stream
        eventService.appendAgentAction(sessionId, "execute_code", code,
            Map.of("codeLength", code.length()), iteration);

        // 7. Execute code in sandbox
        ExecutionResult result = sandboxExecutor.execute(sessionId, code, executionContext);

        // 8. Update execution context with new variables
        if (result.getVariables() != null) {
            executionContext.putAll(result.getVariables());
            stateService.updateExecutionContext(sessionId, executionContext);
        }

        // 9. Append OBSERVATION to event stream
        String observation = buildObservation(result);
        eventService.appendObservation(sessionId, observation,
            Map.of("stdout", result.getStdout(), "stderr", result.getStderr()),
            result.isSuccess(), result.getError(), duration, iteration);

        // 10. Check if task is complete
        if (isTaskComplete(llmResponse)) {
            eventService.appendAgentResponse(sessionId, llmResponse, iteration);
            break;
        }
    }

    return fullResponse.toString().trim();
}
```

### Critical Design Patterns

1. **ONE Action Per Iteration**: Only the first code block is executed per iteration, matching Manus AI's design
2. **Event Stream Context**: Full event history is passed to LLM on each iteration
3. **State Persistence**: Python variables persist in `executionContext` JSONB column
4. **Immutable Events**: Events are append-only and never modified

## Code Execution Pattern

### Code Extraction

```java
public List<String> extractCodeBlocks(String response) {
    List<String> codeBlocks = new ArrayList<>();
    String[] parts = response.split("<execute>");

    for (int i = 1; i < parts.length; i++) {
        int endIndex = parts[i].indexOf("</execute>");
        if (endIndex != -1) {
            String code = parts[i].substring(0, endIndex).trim();
            codeBlocks.add(code);
        }
    }

    return codeBlocks;
}
```

### Sandbox Execution Interface

**Location**: `/backend/src/main/java/ai/mymanus/service/sandbox/SandboxExecutor.java`

```java
public interface SandboxExecutor {
    ExecutionResult execute(String sessionId, String code,
                          Map<String, Object> previousState);

    void destroySessionContainer(String sessionId);
    void cleanupAllContainers();
    Map<String, Object> getContainerStats();
}
```

**Implementations**:
- `PythonSandboxExecutor`: Docker-based isolation (production)
- `HostPythonExecutor`: Direct execution (development/testing)

## Tool Integration

### Tool Interface

**Location**: `/backend/src/main/java/ai/mymanus/tool/Tool.java`

```java
public interface Tool {
    String getName();
    String getDescription();
    String getPythonSignature();
    Map<String, Object> execute(Map<String, Object> parameters) throws Exception;
    default boolean requiresNetwork() { return false; }
}
```

### Tool Registry

**Location**: `/backend/src/main/java/ai/mymanus/tool/ToolRegistry.java`

```java
@Component
public class ToolRegistry {
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(List<Tool> toolList) {
        // Auto-registers all Spring @Component tools
        toolList.forEach(tool -> {
            tools.put(tool.getName(), tool);
            log.info("Registered tool: {}", tool.getName());
        });
    }

    public String getToolDescriptions() {
        return tools.values().stream()
            .map(tool -> String.format("%s: %s\nSignature: %s",
                tool.getName(), tool.getDescription(), tool.getPythonSignature()))
            .collect(Collectors.joining("\n\n"));
    }
}
```

Tools are automatically discovered via Spring's dependency injection.

## WebSocket Events

The agent sends real-time updates to the frontend via WebSocket:

```java
private void sendEvent(String sessionId, String type, String content,
                      Map<String, Object> metadata) {
    AgentEvent event = AgentEvent.builder()
        .type(type)
        .content(content)
        .metadata(metadata)
        .build();

    messagingTemplate.convertAndSend("/topic/agent/" + sessionId, event);
}
```

**Event Types**:
- `status`: thinking, executing, done
- `thought`: LLM reasoning
- `code`: Code being executed
- `output`: stdout from execution
- `error`: stderr or execution failures
- `warning`: Non-fatal issues

## Error Handling

### Execution Failures

When code execution fails, the error is captured in the OBSERVATION event:

```java
if (!result.isSuccess()) {
    log.warn("⚠️ Action failed - LLM will see error in next iteration");
}
```

The LLM sees the error in the event stream and can retry with fixed code in the next iteration.

### Task Completion Detection

```java
private boolean isTaskComplete(String response) {
    String lower = response.toLowerCase();
    return lower.contains("task complete") ||
           lower.contains("task is complete") ||
           lower.contains("finished") ||
           lower.contains("done") ||
           (lower.contains("final") && lower.contains("answer"));
}
```

## Session Management

### Session Lifecycle

```java
// Create/get session
stateService.getOrCreateSession(sessionId);

// Clear session and sandbox
public void clearSession(String sessionId) {
    // 1. Destroy sandbox container
    sandboxExecutor.destroySessionContainer(sessionId);

    // 2. Clear event stream
    eventService.clearEventStream(sessionId);

    // 3. Clear database state
    stateService.deleteSession(sessionId);
    stateService.createSession(sessionId);
}
```

### Auto Title Generation

Sessions automatically get titles based on first user message:

```java
stateService.autoGenerateTitle(sessionId, userQuery);
```

## Configuration

**Location**: `/backend/src/main/resources/application.properties`

```properties
# Agent settings
agent.max-iterations=20

# Spring AI
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.options.model=claude-sonnet-4-5-20250929
spring.ai.anthropic.chat.options.temperature=0.7
spring.ai.anthropic.chat.options.max-tokens=4096

# Sandbox
sandbox.docker.image=manus-sandbox:latest
sandbox.docker.memory-limit=512m
sandbox.docker.cpu-limit=0.5
sandbox.timeout-seconds=30
```

## Performance Monitoring

### Metrics Tracked

- Execution duration (stored in Event.durationMs)
- Success/failure rates (Event.success)
- Iterations per task
- Token usage (via Spring AI)

### Logging

Structured logging throughout the agent loop:

```java
log.info("🔄 Iteration {}/{} starting", iteration, maxIterations);
log.info("🤔 LLM Response: {} chars", llmResponse.length());
log.info("▶️ Executing action (code block)");
log.info("📊 OBSERVATION recorded: success={}, duration={}ms", success, duration);
```

## Testing

### Integration Test Example

Test the complete agent loop with a simple query:

```java
@Test
void testAgentLoop() {
    String sessionId = "test-session";
    String query = "Calculate 2 + 2 and print the result";

    String response = agentService.processQuery(sessionId, query);

    // Verify response contains the answer
    assertTrue(response.contains("4"));

    // Verify event stream
    List<Event> events = eventService.getEventStream(sessionId);
    assertTrue(events.stream().anyMatch(e -> e.getType() == EventType.USER_MESSAGE));
    assertTrue(events.stream().anyMatch(e -> e.getType() == EventType.OBSERVATION));
}
```

## Best Practices

1. **Always use Event Stream**: Store all interactions as immutable events
2. **ONE Action per Iteration**: Execute only the first code block (Manus pattern)
3. **Persist Variables**: Store execution context in JSONB for state continuity
4. **WebSocket Updates**: Send real-time events to frontend for transparency
5. **Error Recovery**: Let LLM see errors in event stream to self-correct
6. **Resource Limits**: Enforce strict timeout and memory limits
7. **Structured Logging**: Use emoji prefixes for easy log scanning

## Common Issues

### Variables Not Persisting

**Problem**: Variables lost between executions
**Solution**: Ensure `result.getVariables()` is saved to `executionContext`

### Infinite Loops

**Problem**: Agent doesn't detect task completion
**Solution**: Check `isTaskComplete()` logic and max iterations limit

### Timeout Errors

**Problem**: Code execution exceeds 30 seconds
**Solution**: Increase `sandbox.timeout-seconds` or optimize code

### Memory Issues

**Problem**: Container runs out of memory
**Solution**: Increase `sandbox.docker.memory-limit` or reduce data size

## Enhancements Status

### ✅ Implemented (Ready to Use)

**1. Code Validation Before Execution**
- **Status**: ✅ Implemented in `PythonValidationService.java`
- **Features**:
  - Python syntax validation using `ast.parse`
  - Safety checks for dangerous patterns (rm -rf, eval, exec)
  - Warning system for risky operations
- **Integration**: Ready to inject into CodeActAgentService before execution
- **Location**: `/backend/src/main/java/ai/mymanus/service/PythonValidationService.java`

**2. Streaming LLM Responses**
- **Status**: ✅ Implemented in `AnthropicService.generateStream()`
- **Features**:
  - Real-time token streaming from Claude API
  - WebSocket-ready for frontend real-time updates
  - Reactive Flux-based implementation
- **Integration**: Ready to wire into agent loop for improved UX
- **Location**: `/backend/src/main/java/ai/mymanus/service/AnthropicService.java` (line 64)

### 🔄 Future Enhancements (Not Yet Implemented)

**3. Enhanced Error Recovery with Retry Strategies**
- **Scope**: Automatic retry logic for transient failures (network, timeout, etc.)
- **Complexity**: Medium
- **Value**: High for production reliability
- **Status**: Not implemented

**4. Multi-Agent Collaboration (Planner/Executor/Verifier Roles)**
- **Scope**: Role-based agent architecture for complex tasks
- **Complexity**: High (requires architectural changes)
- **Value**: High for complex multi-step workflows
- **Note**: Multi-agent infrastructure exists (`MultiAgentController`), but not role-based delegation
- **Status**: Infrastructure ready, roles not implemented

**5. Parallel Tool Execution**
- **Scope**: Execute multiple independent tools concurrently
- **Complexity**: High (async execution framework needed)
- **Value**: Medium (faster execution for parallelizable tasks)
- **Status**: Not implemented

**6. Resource Usage Analytics**
- **Scope**: Track CPU, memory, execution time per tool/iteration
- **Complexity**: Medium
- **Value**: Medium (useful for optimization and debugging)
- **Status**: Not implemented
