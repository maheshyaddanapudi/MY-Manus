package ai.mymanus.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolExecution model
 * Tests tool execution tracking and result persistence
 */
class ToolExecutionTest {

    private ToolExecution toolExecution;

    @BeforeEach
    void setUp() {
        toolExecution = new ToolExecution();
    }

    @Test
    void testToolExecutionCreation() {
        toolExecution.setSessionId("test-session");
        toolExecution.setToolName("file_read");
        toolExecution.setIteration(1);

        assertNotNull(toolExecution);
        assertEquals("test-session", toolExecution.getSessionId());
        assertEquals("file_read", toolExecution.getToolName());
        assertEquals(1, toolExecution.getIteration());
    }

    @Test
    void testFileReadToolExecution() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("path", "/workspace/test.txt");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("content", "File content here");

        toolExecution.setToolName("file_read");
        toolExecution.setParameters(parameters);
        toolExecution.setResult(result);

        assertEquals("file_read", toolExecution.getToolName());
        assertEquals("/workspace/test.txt", toolExecution.getParameters().get("path"));
        assertTrue((Boolean) toolExecution.getResult().get("success"));
    }

    @Test
    void testBrowserNavigateToolExecution() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("url", "https://example.com");
        parameters.put("sessionId", "browser-123");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Navigated successfully");

        toolExecution.setToolName("browser_navigate");
        toolExecution.setParameters(parameters);
        toolExecution.setResult(result);

        assertEquals("browser_navigate", toolExecution.getToolName());
        assertEquals("https://example.com", toolExecution.getParameters().get("url"));
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

        toolExecution.setToolName("browser_view");
        toolExecution.setParameters(parameters);
        toolExecution.setResult(result);

        assertEquals("browser_view", toolExecution.getToolName());
        assertTrue(toolExecution.getResult().containsKey("screenshot"));
        assertTrue(toolExecution.getResult().containsKey("htmlContent"));
        assertTrue(toolExecution.getResult().containsKey("accessibilityTree"));
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

        toolExecution.setToolName("shell_exec");
        toolExecution.setParameters(parameters);
        toolExecution.setResult(result);

        assertEquals("shell_exec", toolExecution.getToolName());
        assertEquals(0, toolExecution.getResult().get("exitCode"));
    }

    @Test
    void testErrorResult() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("path", "/invalid/path.txt");

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", "File not found");

        toolExecution.setToolName("file_read");
        toolExecution.setParameters(parameters);
        toolExecution.setResult(result);

        assertFalse((Boolean) toolExecution.getResult().get("success"));
        assertEquals("File not found", toolExecution.getResult().get("error"));
    }

    @Test
    void testExecutionDuration() {
        toolExecution.setDurationMs(1500L);
        assertEquals(1500L, toolExecution.getDurationMs());
    }

    @Test
    void testIterationTracking() {
        toolExecution.setIteration(5);
        assertEquals(5, toolExecution.getIteration());

        // Verify iteration can be incremented
        toolExecution.setIteration(6);
        assertEquals(6, toolExecution.getIteration());
    }

    @Test
    void testNullParameters() {
        toolExecution.setParameters(null);
        assertNull(toolExecution.getParameters());

        // Set to empty map
        toolExecution.setParameters(new HashMap<>());
        assertNotNull(toolExecution.getParameters());
        assertTrue(toolExecution.getParameters().isEmpty());
    }

    @Test
    void testNullResult() {
        toolExecution.setResult(null);
        assertNull(toolExecution.getResult());

        // Set to empty map
        toolExecution.setResult(new HashMap<>());
        assertNotNull(toolExecution.getResult());
        assertTrue(toolExecution.getResult().isEmpty());
    }

    @Test
    void testComplexResultData() {
        Map<String, Object> result = new HashMap<>();
        result.put("nested", Map.of("key1", "value1", "key2", 123));
        result.put("list", java.util.Arrays.asList("item1", "item2", "item3"));
        result.put("boolean", true);
        result.put("number", 42.5);

        toolExecution.setResult(result);

        assertTrue(toolExecution.getResult().get("nested") instanceof Map);
        assertTrue(toolExecution.getResult().get("list") instanceof java.util.List);
        assertEquals(true, toolExecution.getResult().get("boolean"));
        assertEquals(42.5, toolExecution.getResult().get("number"));
    }

    @Test
    void testTimestampGeneration() {
        long beforeTime = System.currentTimeMillis();

        // Simulate timestamp setting (would be auto-generated by JPA)
        toolExecution.setCreatedAt(new java.util.Date());

        long afterTime = System.currentTimeMillis();

        assertNotNull(toolExecution.getCreatedAt());
        assertTrue(toolExecution.getCreatedAt().getTime() >= beforeTime);
        assertTrue(toolExecution.getCreatedAt().getTime() <= afterTime);
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
            ToolExecution te = new ToolExecution();
            te.setToolName(toolName);
            assertEquals(toolName, te.getToolName());
        }
    }
}
