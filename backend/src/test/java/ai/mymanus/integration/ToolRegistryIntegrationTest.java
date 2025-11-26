package ai.mymanus.integration;

import ai.mymanus.tool.Tool;
import ai.mymanus.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for tool auto-registration
 */
@SpringBootTest
@ActiveProfiles("test")
class ToolRegistryIntegrationTest {

    @Autowired
    private ToolRegistry toolRegistry;

    @Test
    void testAllToolsRegistered() {
        List<Tool> tools = toolRegistry.getAllTools();

        assertNotNull(tools);
        assertFalse(tools.isEmpty());

        // Verify all expected tools are registered
        assertTrue(tools.stream().anyMatch(t -> t.getName().equals("file_read")));
        assertTrue(tools.stream().anyMatch(t -> t.getName().equals("file_write")));
        assertTrue(tools.stream().anyMatch(t -> t.getName().equals("browser_navigate")));
        assertTrue(tools.stream().anyMatch(t -> t.getName().equals("browser_view")));
        assertTrue(tools.stream().anyMatch(t -> t.getName().equals("shell_exec")));
        assertTrue(tools.stream().anyMatch(t -> t.getName().equals("todo")));
    }

    @Test
    void testToolExecution() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "/workspace/registry-test.txt");
        params.put("content", "Test content");

        Map<String, Object> result = toolRegistry.executeTool("file_write", params);

        assertNotNull(result);
        assertTrue(result.containsKey("success"));
    }

    @Test
    void testPythonBindingsGeneration() {
        String bindings = toolRegistry.generatePythonBindings();

        assertNotNull(bindings);
        assertFalse(bindings.isEmpty());

        // Should contain all tool function definitions
        assertTrue(bindings.contains("file_read"));
        assertTrue(bindings.contains("file_write"));
        assertTrue(bindings.contains("browser_navigate"));
    }
}
