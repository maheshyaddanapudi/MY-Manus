package ai.mymanus.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolExecution model
 * Tests tool execution tracking and result persistence
 */
class ToolExecutionTest {

    private ToolExecution toolExecution;
    private AgentState testAgentState;

    @BeforeEach
    void setUp() {
        testAgentState = AgentState.builder()
            .sessionId("test-session")
            .status(AgentState.Status.RUNNING)
            .build();
            
        toolExecution = ToolExecution.builder()
            .agentState(testAgentState)
            .toolName("file_read")
            .status(ToolExecution.ExecutionStatus.SUCCESS)
            .build();
    }

    @Test
    void testToolExecutionCreation() {
        assertNotNull(toolExecution);
        assertEquals("file_read", toolExecution.getToolName());
        assertEquals(testAgentState, toolExecution.getAgentState());
        assertEquals(ToolExecution.ExecutionStatus.SUCCESS, toolExecution.getStatus());
    }

    @Test
    void testFileReadToolExecution() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("path", "/workspace/test.txt");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("content", "File content here");

        ToolExecution execution = ToolExecution.builder()
            .agentState(testAgentState)
            .toolName("file_read")
            .parameters(parameters)
            .result(result)
            .status(ToolExecution.ExecutionStatus.SUCCESS)
            .build();

        assertEquals("file_read", execution.getToolName());
        assertEquals("/workspace/test.txt", execution.getParameters().get("path"));
        assertTrue((Boolean) execution.getResult().get("success"));
    }

    @Test
    void testBrowserNavigateToolExecution() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("url", "https://example.com");
        parameters.put("sessionId", "browser-123");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Navigated successfully");

        ToolExecution execution = ToolExecution.builder()
            .agentState(testAgentState)
            .toolName("browser_navigate")
            .parameters(parameters)
            .result(result)
            .status(ToolExecution.ExecutionStatus.SUCCESS)
            .build();

        assertEquals("browser_navigate", execution.getToolName());
        assertEquals("https://example.com", execution.getParameters().get("url"));
    }

    @Test
    void testBrowserViewToolExecution() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("sessionId", "browser-123");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("screenshot", "base64encodedimage...");
        result.put("url", "https://example.com");
        result.put("title", "Example Domain");
        result.put("htmlContent", "<html>...</html>");
        result.put("accessibilityTree", "RootWebArea...");
        result.put("timestamp", System.currentTimeMillis());

        ToolExecution execution = ToolExecution.builder()
            .agentState(testAgentState)
            .toolName("browser_view")
            .parameters(parameters)
            .result(result)
            .status(ToolExecution.ExecutionStatus.SUCCESS)
            .build();

        assertEquals("browser_view", execution.getToolName());
        assertTrue(execution.getResult().containsKey("screenshot"));
        assertTrue(execution.getResult().containsKey("htmlContent"));
        assertTrue(execution.getResult().containsKey("accessibilityTree"));
    }

    @Test
    void testShellExecToolExecution() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("command", "ls -la");
        parameters.put("timeout", 30);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("stdout", "file1.txt\nfile2.txt");
        result.put("stderr", "");
        result.put("exitCode", 0);

        ToolExecution execution = ToolExecution.builder()
            .agentState(testAgentState)
            .toolName("shell_exec")
            .parameters(parameters)
            .result(result)
            .status(ToolExecution.ExecutionStatus.SUCCESS)
            .build();

        assertEquals("shell_exec", execution.getToolName());
        assertEquals(0, execution.getResult().get("exitCode"));
    }

    @Test
    void testErrorResult() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("path", "/invalid/path.txt");

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", "File not found");

        ToolExecution execution = ToolExecution.builder()
            .agentState(testAgentState)
            .toolName("file_read")
            .parameters(parameters)
            .result(result)
            .status(ToolExecution.ExecutionStatus.FAILED)
            .build();

        assertFalse((Boolean) execution.getResult().get("success"));
        assertEquals("File not found", execution.getResult().get("error"));
        assertEquals(ToolExecution.ExecutionStatus.FAILED, execution.getStatus());
    }

    @Test
    void testExecutionDuration() {
        ToolExecution execution = ToolExecution.builder()
            .agentState(testAgentState)
            .toolName("file_read")
            .durationMs(1500)
            .status(ToolExecution.ExecutionStatus.SUCCESS)
            .build();
            
        assertEquals(1500, execution.getDurationMs());
    }

    @Test
    void testExecutionStatus() {
        // Test all status transitions
        ToolExecution execution = ToolExecution.builder()
            .agentState(testAgentState)
            .toolName("test_tool")
            .status(ToolExecution.ExecutionStatus.PENDING)
            .build();
            
        assertEquals(ToolExecution.ExecutionStatus.PENDING, execution.getStatus());
        
        execution.setStatus(ToolExecution.ExecutionStatus.RUNNING);
        assertEquals(ToolExecution.ExecutionStatus.RUNNING, execution.getStatus());
        
        execution.setStatus(ToolExecution.ExecutionStatus.SUCCESS);
        assertEquals(ToolExecution.ExecutionStatus.SUCCESS, execution.getStatus());
    }

    @Test
    void testNullParameters() {
        ToolExecution execution = ToolExecution.builder()
            .agentState(testAgentState)
            .toolName("test_tool")
            .parameters(null)
            .status(ToolExecution.ExecutionStatus.SUCCESS)
            .build();
            
        assertNull(execution.getParameters());

        // Set to empty map
        execution.setParameters(new HashMap<>());
        assertNotNull(execution.getParameters());
        assertTrue(execution.getParameters().isEmpty());
    }

    @Test
    void testNullResult() {
        ToolExecution execution = ToolExecution.builder()
            .agentState(testAgentState)
            .toolName("test_tool")
            .result(null)
            .status(ToolExecution.ExecutionStatus.SUCCESS)
            .build();
            
        assertNull(execution.getResult());

        // Set to empty map
        execution.setResult(new HashMap<>());
        assertNotNull(execution.getResult());
        assertTrue(execution.getResult().isEmpty());
    }

    @Test
    void testComplexResultData() {
        Map<String, Object> result = new HashMap<>();
        result.put("nested", Map.of("key1", "value1", "key2", 123));
        result.put("list", java.util.Arrays.asList("item1", "item2", "item3"));
        result.put("boolean", true);
        result.put("number", 42.5);

        ToolExecution execution = ToolExecution.builder()
            .agentState(testAgentState)
            .toolName("test_tool")
            .result(result)
            .status(ToolExecution.ExecutionStatus.SUCCESS)
            .build();

        assertTrue(execution.getResult().get("nested") instanceof Map);
        assertTrue(execution.getResult().get("list") instanceof java.util.List);
        assertEquals(true, execution.getResult().get("boolean"));
        assertEquals(42.5, execution.getResult().get("number"));
    }

    @Test
    void testTimestampGeneration() {
        LocalDateTime beforeTime = LocalDateTime.now();

        ToolExecution execution = ToolExecution.builder()
            .agentState(testAgentState)
            .toolName("test_tool")
            .timestamp(LocalDateTime.now())
            .status(ToolExecution.ExecutionStatus.SUCCESS)
            .build();

        LocalDateTime afterTime = LocalDateTime.now();

        assertNotNull(execution.getTimestamp());
        assertTrue(!execution.getTimestamp().isBefore(beforeTime));
        assertTrue(!execution.getTimestamp().isAfter(afterTime));
    }

    @Test
    void testMultipleToolTypes() {
        String[] toolNames = {
            "file_read", "file_write", "file_replace_string",
            "file_find_content", "file_find_by_name",
            "browser_navigate", "browser_view", "browser_click",
            "browser_input", "browser_scroll_up", "browser_scroll_down",
            "browser_press_key", "browser_refresh",
            "shell_exec", "todo", "web_search", "data_visualization"
        };

        for (String toolName : toolNames) {
            ToolExecution te = ToolExecution.builder()
                .agentState(testAgentState)
                .toolName(toolName)
                .status(ToolExecution.ExecutionStatus.SUCCESS)
                .build();
            assertEquals(toolName, te.getToolName());
        }
    }

    @Test
    void testAllExecutionStatuses() {
        ToolExecution.ExecutionStatus[] allStatuses = ToolExecution.ExecutionStatus.values();
        
        assertEquals(4, allStatuses.length);
        assertTrue(containsStatus(allStatuses, ToolExecution.ExecutionStatus.PENDING));
        assertTrue(containsStatus(allStatuses, ToolExecution.ExecutionStatus.RUNNING));
        assertTrue(containsStatus(allStatuses, ToolExecution.ExecutionStatus.SUCCESS));
        assertTrue(containsStatus(allStatuses, ToolExecution.ExecutionStatus.FAILED));
    }

    private boolean containsStatus(ToolExecution.ExecutionStatus[] statuses, ToolExecution.ExecutionStatus target) {
        for (ToolExecution.ExecutionStatus status : statuses) {
            if (status == target) return true;
        }
        return false;
    }
}
