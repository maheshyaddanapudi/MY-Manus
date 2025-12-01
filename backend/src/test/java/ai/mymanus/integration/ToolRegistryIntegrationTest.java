package ai.mymanus.integration;

import ai.mymanus.tool.Tool;
import ai.mymanus.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
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
        List<Tool> tools = new ArrayList<>(toolRegistry.getAllTools());

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
    void testToolRetrieval() throws Exception {
        // Test that we can retrieve a specific tool
        var fileWriteTool = toolRegistry.getTool("file_write");
        assertTrue(fileWriteTool.isPresent());
        assertEquals("file_write", fileWriteTool.get().getName());
        
        var shellExecTool = toolRegistry.getTool("shell_exec");
        assertTrue(shellExecTool.isPresent());
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
