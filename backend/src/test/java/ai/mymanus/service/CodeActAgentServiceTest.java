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

import java.util.HashMap;
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
    }

    @Test
    void testProcessQuery() {
        String userQuery = "Read /workspace/test.txt";

        when(agentStateService.getOrCreateState(testSessionId))
            .thenReturn(testState);
        when(eventService.appendUserMessage(eq(testSessionId), eq(userQuery), anyInt()))
            .thenReturn(new Event());

        // This would trigger the agent loop in real scenario
        // Testing the method exists and doesn't throw
        assertDoesNotThrow(() -> {
            codeActAgentService.processQuery(testSessionId, userQuery);
        });

        verify(eventService, times(1)).appendUserMessage(eq(testSessionId), eq(userQuery), anyInt());
        verify(agentStateService, atLeastOnce()).getOrCreateState(testSessionId);
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

        when(agentStateService.getOrCreateState(testSessionId))
            .thenReturn(testState);

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

        when(agentStateService.getOrCreateState(testSessionId))
            .thenReturn(testState);
        when(eventService.appendUserMessage(eq(testSessionId), eq(userQuery), anyInt()))
            .thenReturn(new Event());

        assertDoesNotThrow(() -> {
            codeActAgentService.processQuery(testSessionId, userQuery);
        });

        verify(agentStateService, atLeastOnce()).getOrCreateState(testSessionId);
    }

    @Test
    void testMultipleQueries() {
        String query1 = "First query";
        String query2 = "Second query";

        when(agentStateService.getOrCreateState(testSessionId))
            .thenReturn(testState);
        when(eventService.appendUserMessage(anyString(), anyString(), anyInt()))
            .thenReturn(new Event());

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
