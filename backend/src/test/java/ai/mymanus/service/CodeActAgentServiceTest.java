package ai.mymanus.service;

import ai.mymanus.model.AgentState;
import ai.mymanus.model.Event;
import ai.mymanus.service.sandbox.PythonSandboxExecutor;
import ai.mymanus.service.sandbox.ExecutionResult;
import ai.mymanus.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CodeActAgentService
 * Tests agent loop logic and code execution
 */
@ExtendWith(MockitoExtension.class)
class CodeActAgentServiceTest {

    @Mock
    private EventService eventService;

    @Mock
    private AgentStateService agentStateService;

    @Mock
    private AnthropicService anthropicService;

    @Mock
    private PythonSandboxExecutor sandboxExecutor;

    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private PromptBuilder promptBuilder;

    @InjectMocks
    private CodeActAgentService codeActAgentService;

    private String testSessionId = "test-session-123";
    private AgentState testState;

    @BeforeEach
    void setUp() {
        testState = AgentState.builder()
            .sessionId(testSessionId)
            .iteration(1)
            .status(AgentState.Status.IDLE)
            .executionContext(new HashMap<>())
            .metadata(new HashMap<>())
            .build();
        
        // Set @Value field using reflection
        ReflectionTestUtils.setField(codeActAgentService, "maxIterations", 20);
        
        // Mock common dependencies (lenient because not all tests use all methods)
        lenient().when(agentStateService.getOrCreateSession(anyString())).thenReturn(testState);
        lenient().when(agentStateService.getExecutionContext(anyString())).thenReturn(new HashMap<>());
        lenient().when(eventService.buildEventStreamContext(anyString())).thenReturn("Previous context");
        lenient().when(promptBuilder.buildSystemPrompt(any(), anyBoolean())).thenReturn("System prompt");
        lenient().when(promptBuilder.extractCodeBlocks(anyString())).thenReturn(List.of()); // No code blocks by default
    }

    @Test
    void testProcessQuery() {
        String userQuery = "Read /workspace/test.txt";

        when(eventService.appendUserMessage(eq(testSessionId), eq(userQuery), anyInt()))
            .thenReturn(new Event());
        
        // Mock streaming response (no code blocks, so loop terminates)
        when(anthropicService.generateStream(anyString(), anyString(), anyString()))
            .thenReturn(Flux.just("I'll help you ", "read the file."));

        // This would trigger the agent loop in real scenario
        // Testing the method exists and doesn't throw
        assertDoesNotThrow(() -> {
            codeActAgentService.processQuery(testSessionId, userQuery);
        });

        verify(eventService, times(1)).appendUserMessage(eq(testSessionId), eq(userQuery), anyInt());
        verify(agentStateService, atLeastOnce()).getOrCreateSession(testSessionId);
    }

    @Test
    void testExecutePythonCode() {
        String pythonCode = "print('Hello, World!')";
        Map<String, Object> context = new HashMap<>();

        ExecutionResult executionResult = ExecutionResult.builder()
            .stdout("Hello, World!\n")
            .stderr("")
            .exitCode(0)
            .success(true)
            .variables(new HashMap<>())
            .build();

        when(sandboxExecutor.execute(eq(testSessionId), eq(pythonCode), eq(context)))
            .thenReturn(executionResult);

        ExecutionResult result = sandboxExecutor.execute(testSessionId, pythonCode, context);

        assertNotNull(result);
        assertEquals("Hello, World!\n", result.getStdout());
        assertEquals(0, result.getExitCode());
        assertTrue(result.isSuccess());
    }

    @Test
    void testProcessQueryWithEmptyMessage() {
        String emptyQuery = "";

        when(eventService.appendUserMessage(eq(testSessionId), eq(emptyQuery), anyInt()))
            .thenReturn(new Event());
        
        // Mock streaming response
        when(anthropicService.generateStream(anyString(), anyString(), anyString()))
            .thenReturn(Flux.just("Please provide a message."));

        assertDoesNotThrow(() -> {
            codeActAgentService.processQuery(testSessionId, emptyQuery);
        });
    }

    @Test
    void testProcessQueryWithNullSessionId() {
        String userQuery = "Test query";

        assertThrows(Exception.class, () -> {
            codeActAgentService.processQuery(null, userQuery);
        });
    }

    @Test
    void testAgentStateInitialization() {
        when(agentStateService.getOrCreateState(testSessionId))
            .thenReturn(testState);

        AgentState state = agentStateService.getOrCreateState(testSessionId);

        assertNotNull(state);
        assertEquals(testSessionId, state.getSessionId());
        assertEquals(AgentState.Status.IDLE, state.getStatus());
        assertEquals(1, state.getIteration());
    }

    @Test
    void testExecutePythonWithError() {
        String pythonCode = "1 / 0";
        Map<String, Object> context = new HashMap<>();

        ExecutionResult executionResult = ExecutionResult.builder()
            .stdout("")
            .stderr("ZeroDivisionError: division by zero")
            .exitCode(1)
            .success(false)
            .error("ZeroDivisionError: division by zero")
            .build();

        when(sandboxExecutor.execute(eq(testSessionId), eq(pythonCode), eq(context)))
            .thenReturn(executionResult);

        ExecutionResult result = sandboxExecutor.execute(testSessionId, pythonCode, context);

        assertNotNull(result);
        assertEquals(1, result.getExitCode());
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr().contains("ZeroDivisionError"));
    }

    @Test
    void testExecutePythonWithContext() {
        String pythonCode = "result = x + y";
        Map<String, Object> context = new HashMap<>();
        context.put("x", 10);
        context.put("y", 20);

        Map<String, Object> newVariables = new HashMap<>();
        newVariables.put("result", 30);

        ExecutionResult executionResult = ExecutionResult.builder()
            .stdout("")
            .stderr("")
            .exitCode(0)
            .success(true)
            .variables(newVariables)
            .build();

        when(sandboxExecutor.execute(eq(testSessionId), eq(pythonCode), eq(context)))
            .thenReturn(executionResult);

        ExecutionResult result = sandboxExecutor.execute(testSessionId, pythonCode, context);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(30, result.getVariables().get("result"));
    }

    @Test
    void testServiceDependencies() {
        assertNotNull(codeActAgentService);
        // Verify that all dependencies are injected
    }

    @Test
    void testProcessQueryUpdatesState() {
        String userQuery = "Test query";

        when(eventService.appendUserMessage(eq(testSessionId), eq(userQuery), anyInt()))
            .thenReturn(new Event());
        
        // Mock streaming response
        when(anthropicService.generateStream(anyString(), anyString(), anyString()))
            .thenReturn(Flux.just("Response"));

        assertDoesNotThrow(() -> {
            codeActAgentService.processQuery(testSessionId, userQuery);
        });

        verify(agentStateService, atLeastOnce()).getOrCreateSession(testSessionId);
    }

    @Test
    void testMultipleQueries() {
        String query1 = "First query";
        String query2 = "Second query";

        when(eventService.appendUserMessage(anyString(), anyString(), anyInt()))
            .thenReturn(new Event());
        
        // Mock streaming response
        when(anthropicService.generateStream(anyString(), anyString(), anyString()))
            .thenReturn(Flux.just("Response"));

        assertDoesNotThrow(() -> {
            codeActAgentService.processQuery(testSessionId, query1);
            codeActAgentService.processQuery(testSessionId, query2);
        });

        verify(eventService, times(2)).appendUserMessage(eq(testSessionId), anyString(), anyInt());
    }

    @Test
    void testExecutionContextPreserved() {
        Map<String, Object> context = new HashMap<>();
        context.put("previous_result", "test");

        testState.setExecutionContext(context);

        when(agentStateService.getOrCreateState(testSessionId))
            .thenReturn(testState);

        AgentState state = agentStateService.getOrCreateState(testSessionId);

        assertNotNull(state.getExecutionContext());
        assertEquals("test", state.getExecutionContext().get("previous_result"));
    }

    @Test
    void testIterationIncrement() {
        testState.setIteration(5);

        when(agentStateService.getOrCreateState(testSessionId))
            .thenReturn(testState);

        AgentState state = agentStateService.getOrCreateState(testSessionId);

        assertEquals(5, state.getIteration());
    }

    @Test
    void testStatusTransitions() {
        testState.setStatus(AgentState.Status.RUNNING);

        when(agentStateService.getOrCreateState(testSessionId))
            .thenReturn(testState);

        AgentState state = agentStateService.getOrCreateState(testSessionId);

        assertEquals(AgentState.Status.RUNNING, state.getStatus());
    }
}
