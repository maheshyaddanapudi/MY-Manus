package ai.mymanus.tool;

import ai.mymanus.tool.impl.file.FileReadTool;
import ai.mymanus.tool.impl.browser.BrowserNavigateTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ToolRegistry
 * Tests tool registration and Python bindings generation
 */
@ExtendWith(MockitoExtension.class)
class ToolRegistryTest {

    @Mock
    private FileReadTool fileReadTool;

    @Mock
    private BrowserNavigateTool browserNavigateTool;

    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        List<Tool> tools = List.of(fileReadTool, browserNavigateTool);
        toolRegistry = new ToolRegistry(tools);

        when(fileReadTool.getName()).thenReturn("file_read");
        when(fileReadTool.getDescription()).thenReturn("Read a file from workspace");
        when(fileReadTool.getParameters()).thenReturn(Map.of("path", "string"));

        when(browserNavigateTool.getName()).thenReturn("browser_navigate");
        when(browserNavigateTool.getDescription()).thenReturn("Navigate browser to URL");
        when(browserNavigateTool.getParameters()).thenReturn(Map.of(
            "url", "string",
            "sessionId", "string"
        ));
    }

    @Test
    void testToolRegistration() {
        assertNotNull(toolRegistry);

        Tool tool = toolRegistry.getTool("file_read");
        assertNotNull(tool);
        assertEquals("file_read", tool.getName());
    }

    @Test
    void testGetTool() {
        Tool tool = toolRegistry.getTool("file_read");

        assertNotNull(tool);
        assertEquals("file_read", tool.getName());
    }

    @Test
    void testGetToolNotFound() {
        Tool tool = toolRegistry.getTool("non_existent_tool");

        assertNull(tool);
    }

    @Test
    void testGetAllTools() {
        List<Tool> allTools = toolRegistry.getAllTools();

        assertNotNull(allTools);
        assertEquals(2, allTools.size());
    }

    @Test
    void testGeneratePythonBindings() {
        String bindings = toolRegistry.generatePythonBindings();

        assertNotNull(bindings);
        assertTrue(bindings.contains("file_read"));
        assertTrue(bindings.contains("browser_navigate"));
    }

    @Test
    void testPythonBindingsContainAllTools() {
        String bindings = toolRegistry.generatePythonBindings();

        assertTrue(bindings.contains("file_read"));
        assertTrue(bindings.contains("browser_navigate"));
        assertTrue(bindings.contains("def ") || bindings.contains("function"));
    }

    @Test
    void testPythonBindingsFormat() {
        String bindings = toolRegistry.generatePythonBindings();

        // Should contain Python function definitions
        assertTrue(bindings.contains("file_read"));
        assertTrue(bindings.contains("path"));
    }

    @Test
    void testExecuteTool() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("path", "/workspace/test.txt");

        Map<String, Object> expectedResult = Map.of(
            "success", true,
            "content", "File content"
        );

        when(fileReadTool.execute(parameters)).thenReturn(expectedResult);

        Map<String, Object> result = toolRegistry.executeTool("file_read", parameters);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertEquals("File content", result.get("content"));
        verify(fileReadTool, times(1)).execute(parameters);
    }

    @Test
    void testExecuteToolNotFound() {
        Map<String, Object> parameters = new HashMap<>();

        assertThrows(IllegalArgumentException.class, () -> {
            toolRegistry.executeTool("non_existent_tool", parameters);
        });
    }

    @Test
    void testToolCount() {
        List<Tool> allTools = toolRegistry.getAllTools();

        assertEquals(2, allTools.size());
    }

    @Test
    void testToolNames() {
        List<Tool> allTools = toolRegistry.getAllTools();

        List<String> toolNames = allTools.stream()
            .map(Tool::getName)
            .toList();

        assertTrue(toolNames.contains("file_read"));
        assertTrue(toolNames.contains("browser_navigate"));
    }

    @Test
    void testToolDescriptions() {
        Tool fileReadToolInstance = toolRegistry.getTool("file_read");

        assertEquals("Read a file from workspace", fileReadToolInstance.getDescription());
    }

    @Test
    void testToolParameters() {
        Tool fileReadToolInstance = toolRegistry.getTool("file_read");

        Map<String, String> parameters = fileReadToolInstance.getParameters();

        assertNotNull(parameters);
        assertTrue(parameters.containsKey("path"));
        assertEquals("string", parameters.get("path"));
    }

    @Test
    void testMultipleParameters() {
        Tool browserToolInstance = toolRegistry.getTool("browser_navigate");

        Map<String, String> parameters = browserToolInstance.getParameters();

        assertNotNull(parameters);
        assertEquals(2, parameters.size());
        assertTrue(parameters.containsKey("url"));
        assertTrue(parameters.containsKey("sessionId"));
    }

    @Test
    void testToolAutoRegistration() {
        // In real Spring context, tools are auto-registered via dependency injection
        // This test verifies the registry can handle multiple tools

        List<Tool> allTools = toolRegistry.getAllTools();

        assertNotNull(allTools);
        assertFalse(allTools.isEmpty());

        for (Tool tool : allTools) {
            assertNotNull(tool.getName());
            assertNotNull(tool.getDescription());
            assertNotNull(tool.getParameters());
        }
    }
}
