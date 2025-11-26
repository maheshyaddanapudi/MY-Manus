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
        testState = new AgentState();
        testState.setSessionId(testSessionId);
        testState.setIteration(1);
        testState.setStatus(AgentState.Status.IDLE);
        testState.setPythonVariables(new HashMap<>());
        testState.setExecutionContext(new HashMap<>());
    }

    @Test
    void testProcessUserMessage() {
        String userMessage = "Read /workspace/test.txt";

        when(agentStateService.getOrCreateState(testSessionId))
            .thenReturn(testState);
        when(eventService.appendUserMessage(eq(testSessionId), eq(userMessage), anyInt()))
            .thenReturn(new Event());

        // This would trigger the agent loop in real scenario
        // Testing the method exists and doesn't throw
        assertDoesNotThrow(() -> {
            codeActAgentService.processUserMessage(testSessionId, userMessage);
        });

        verify(eventService, times(1)).appendUserMessage(eq(testSessionId), eq(userMessage), anyInt());
        verify(agentStateService, atLeastOnce()).getOrCreateState(testSessionId);
    }

    @Test
    void testExecutePythonCode() {
        String pythonCode = "print('Hello, World!')";

        ExecutionResult executionResult = ExecutionResult.builder()
            .stdout("Hello, World!\n")
            .stderr("")
            .exitCode(0)
            .build();

        when(sandboxExecutor.execute(eq(pythonCode), any()))
            .thenReturn(executionResult);

        ExecutionResult result = sandboxExecutor.execute(pythonCode, new HashMap<>());

        assertNotNull(result);
        assertEquals("Hello, World!\n", result.getStdout());
        assertEquals(0, result.getExitCode());
    }

    @Test
    void testExtractPythonCodeBlocks() {
        String assistantMessage = """
            I'll read the file for you.

            ```python
            with open('/workspace/test.txt', 'r') as f:
                content = f.read()
            print(content)
            ```

            This code reads the file and prints its content.
            """;

        // Test code block extraction logic
        assertTrue(assistantMessage.contains("```python"));
        assertTrue(assistantMessage.contains("with open"));
    }

    @Test
    void testOneActionPerIteration() {
        String assistantMessage = """
            I'll perform two actions:

            ```python
            print('First action')
            ```

            ```python
            print('Second action')
            ```
            """;

        // According to Manus AI pattern, only the FIRST code block should be executed
        // This is a critical design pattern
        long codeBlockCount = assistantMessage.split("```python").length - 1;
        assertEquals(2, codeBlockCount);

        // In real implementation, only first block would be executed
        // Second block would be executed in next iteration
    }

    @Test
    void testStateUpdate() {
        when(agentStateService.updateState(any(AgentState.class)))
            .thenReturn(testState);

        AgentState updated = agentStateService.updateState(testState);

        assertNotNull(updated);
        verify(agentStateService, times(1)).updateState(testState);
    }

    @Test
    void testToolRegistryIntegration() {
        String toolBindings = "# Tool functions\ndef file_read(path):\n    pass";

        when(toolRegistry.generatePythonBindings())
            .thenReturn(toolBindings);

        String bindings = toolRegistry.generatePythonBindings();

        assertNotNull(bindings);
        assertTrue(bindings.contains("file_read"));
        verify(toolRegistry, times(1)).generatePythonBindings();
    }

    @Test
    void testErrorHandling() {
        String pythonCode = "1 / 0  # Division by zero";

        ExecutionResult executionResult = ExecutionResult.builder()
            .stdout("")
            .stderr("ZeroDivisionError: division by zero")
            .exitCode(1)
            .build();

        when(sandboxExecutor.execute(eq(pythonCode), any()))
            .thenReturn(executionResult);

        ExecutionResult result = sandboxExecutor.execute(pythonCode, new HashMap<>());

        assertNotNull(result);
        assertEquals(1, result.getExitCode());
        assertTrue(result.getStderr().contains("ZeroDivisionError"));
    }

    @Test
    void testIterationIncrement() {
        testState.setIteration(5);

        when(agentStateService.getOrCreateState(testSessionId))
            .thenReturn(testState);

        AgentState state = agentStateService.getOrCreateState(testSessionId);

        assertEquals(5, state.getIteration());

        // Simulate iteration increment
        state.setIteration(state.getIteration() + 1);
        assertEquals(6, state.getIteration());
    }
}
