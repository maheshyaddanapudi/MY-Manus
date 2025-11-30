package ai.mymanus.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgentState model
 * Tests state persistence and variable storage
 */
class AgentStateTest {

    private AgentState agentState;

    @BeforeEach
    void setUp() {
        agentState = new AgentState();
    }

    @Test
    void testAgentStateCreation() {
        agentState.setSessionId("test-session");
        agentState.setIteration(1);
        agentState.setStatus(AgentState.Status.RUNNING);

        assertNotNull(agentState);
        assertEquals("test-session", agentState.getSessionId());
        assertEquals(1, agentState.getIteration());
        assertEquals(AgentState.Status.RUNNING, agentState.getStatus());
    }

    @Test
    void testPythonVariablesPersistence() {
        // Note: pythonVariables are stored in executionContext
        Map<String, Object> variables = new HashMap<>();
        variables.put("count", 5);
        variables.put("message", "Hello");
        variables.put("is_ready", true);

        agentState.setExecutionContext(variables);

        assertEquals(3, agentState.getExecutionContext().size());
        assertEquals(5, agentState.getExecutionContext().get("count"));
        assertEquals("Hello", agentState.getExecutionContext().get("message"));
        assertEquals(true, agentState.getExecutionContext().get("is_ready"));
    }

    @Test
    void testExecutionContextPersistence() {
        Map<String, Object> context = new HashMap<>();
        context.put("current_file", "/workspace/main.py");
        context.put("browser_session_id", "browser-123");
        context.put("working_directory", "/workspace");

        agentState.setExecutionContext(context);

        assertEquals("/workspace/main.py", agentState.getExecutionContext().get("current_file"));
        assertEquals("browser-123", agentState.getExecutionContext().get("browser_session_id"));
    }

    @Test
    void testStatusTransitions() {
        // IDLE -> RUNNING
        agentState.setStatus(AgentState.Status.IDLE);
        assertEquals(AgentState.Status.IDLE, agentState.getStatus());

        // RUNNING -> WAITING_INPUT
        agentState.setStatus(AgentState.Status.RUNNING);
        assertEquals(AgentState.Status.RUNNING, agentState.getStatus());

        // WAITING_INPUT -> COMPLETED
        agentState.setStatus(AgentState.Status.WAITING_INPUT);
        assertEquals(AgentState.Status.WAITING_INPUT, agentState.getStatus());

        // COMPLETED
        agentState.setStatus(AgentState.Status.COMPLETED);
        assertEquals(AgentState.Status.COMPLETED, agentState.getStatus());

        // ERROR
        agentState.setStatus(AgentState.Status.ERROR);
        assertEquals(AgentState.Status.ERROR, agentState.getStatus());
    }

    @Test
    void testAllStatusTypes() {
        AgentState.Status[] allStatuses = AgentState.Status.values();

        // Verify all 5 status types exist
        assertEquals(5, allStatuses.length);

        assertTrue(containsStatus(allStatuses, AgentState.Status.IDLE));
        assertTrue(containsStatus(allStatuses, AgentState.Status.RUNNING));
        assertTrue(containsStatus(allStatuses, AgentState.Status.WAITING_INPUT));
        assertTrue(containsStatus(allStatuses, AgentState.Status.COMPLETED));
        assertTrue(containsStatus(allStatuses, AgentState.Status.ERROR));
    }

    @Test
    void testIterationIncrement() {
        agentState.setIteration(1);
        assertEquals(1, agentState.getIteration());

        agentState.setIteration(2);
        assertEquals(2, agentState.getIteration());

        agentState.setIteration(3);
        assertEquals(3, agentState.getIteration());
    }

    @Test
    void testNullPythonVariables() {
        // Note: pythonVariables are stored in executionContext
        agentState.setExecutionContext(null);
        assertNull(agentState.getExecutionContext());

        // Set to empty map
        agentState.setExecutionContext(new HashMap<>());
        assertNotNull(agentState.getExecutionContext());
        assertTrue(agentState.getExecutionContext().isEmpty());
    }

    @Test
    void testNullExecutionContext() {
        agentState.setExecutionContext(null);
        assertNull(agentState.getExecutionContext());

        // Set to empty map
        agentState.setExecutionContext(new HashMap<>());
        assertNotNull(agentState.getExecutionContext());
        assertTrue(agentState.getExecutionContext().isEmpty());
    }

    @Test
    void testComplexDataTypes() {
        // Note: pythonVariables are stored in executionContext
        Map<String, Object> variables = new HashMap<>();
        variables.put("list", java.util.Arrays.asList(1, 2, 3));
        variables.put("nested", Map.of("key", "value"));
        variables.put("number", 42.5);

        agentState.setExecutionContext(variables);

        assertTrue(agentState.getExecutionContext().get("list") instanceof java.util.List);
        assertTrue(agentState.getExecutionContext().get("nested") instanceof Map);
        assertEquals(42.5, agentState.getExecutionContext().get("number"));
    }

    @Test
    void testLastError() {
        agentState.setLastError("Python execution failed");
        assertEquals("Python execution failed", agentState.getLastError());

        agentState.setLastError(null);
        assertNull(agentState.getLastError());
    }

    @Test
    void testCurrentTask() {
        agentState.setCurrentTask("Read file and extract data");
        assertEquals("Read file and extract data", agentState.getCurrentTask());

        agentState.setCurrentTask(null);
        assertNull(agentState.getCurrentTask());
    }

    private boolean containsStatus(AgentState.Status[] statuses, AgentState.Status target) {
        for (AgentState.Status status : statuses) {
            if (status == target) return true;
        }
        return false;
    }
}
