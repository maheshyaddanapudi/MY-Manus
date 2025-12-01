package ai.mymanus.tool;

import ai.mymanus.tool.impl.file.FileReadTool;
import ai.mymanus.tool.impl.browser.BrowserNavigateTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        when(fileReadTool.getPythonSignature()).thenReturn("file_read(path: str)");

        when(browserNavigateTool.getName()).thenReturn("browser_navigate");
        when(browserNavigateTool.getDescription()).thenReturn("Navigate browser to URL");
        when(browserNavigateTool.getPythonSignature()).thenReturn("browser_navigate(url: str, session_id: str)");
    }

    @Test
    void testToolRegistration() {
        assertNotNull(toolRegistry);

        Optional<Tool> tool = toolRegistry.getTool("file_read");
        assertTrue(tool.isPresent());
        assertEquals("file_read", tool.get().getName());
    }

    @Test
    void testGetTool() {
        Optional<Tool> tool = toolRegistry.getTool("file_read");

        assertTrue(tool.isPresent());
        assertEquals("file_read", tool.get().getName());
        assertEquals("Read a file from workspace", tool.get().getDescription());
    }

    @Test
    void testGetToolNotFound() {
        Optional<Tool> tool = toolRegistry.getTool("non_existent_tool");

        assertFalse(tool.isPresent());
    }

    @Test
    void testGetAllTools() {
        Collection<Tool> allTools = toolRegistry.getAllTools();

        assertNotNull(allTools);
        assertEquals(2, allTools.size());
    }

    @Test
    void testRegisterAdditionalTool() {
        Tool newTool = mock(Tool.class);
        when(newTool.getName()).thenReturn("new_tool");
        when(newTool.getDescription()).thenReturn("A new tool");
        when(newTool.getPythonSignature()).thenReturn("new_tool()");

        toolRegistry.registerTool(newTool);

        Optional<Tool> retrievedTool = toolRegistry.getTool("new_tool");
        assertTrue(retrievedTool.isPresent());
        assertEquals("new_tool", retrievedTool.get().getName());
    }

    @Test
    void testGetToolDescriptions() {
        String descriptions = toolRegistry.getToolDescriptions();

        assertNotNull(descriptions);
        assertTrue(descriptions.contains("file_read"));
        assertTrue(descriptions.contains("Read a file from workspace"));
        assertTrue(descriptions.contains("browser_navigate"));
        assertTrue(descriptions.contains("Navigate browser to URL"));
    }

    @Test
    void testGeneratePythonBindings() {
        String bindings = toolRegistry.generatePythonBindings();

        assertNotNull(bindings);
        assertTrue(bindings.contains("def file_read(path: str):"));
        assertTrue(bindings.contains("def browser_navigate(url: str, session_id: str):"));
        assertTrue(bindings.contains("_execute_tool"));
    }

    @Test
    void testPythonBindingsContainDescriptions() {
        String bindings = toolRegistry.generatePythonBindings();

        assertTrue(bindings.contains("Read a file from workspace"));
        assertTrue(bindings.contains("Navigate browser to URL"));
    }

    @Test
    void testToolRegistryWithEmptyList() {
        ToolRegistry emptyRegistry = new ToolRegistry(List.of());

        Collection<Tool> tools = emptyRegistry.getAllTools();
        assertNotNull(tools);
        assertEquals(0, tools.size());
    }

    @Test
    void testToolRegistryWithSingleTool() {
        Tool singleTool = mock(Tool.class);
        when(singleTool.getName()).thenReturn("single_tool");
        when(singleTool.getDescription()).thenReturn("Single tool");
        when(singleTool.getPythonSignature()).thenReturn("single_tool()");

        ToolRegistry singleRegistry = new ToolRegistry(List.of(singleTool));

        Collection<Tool> tools = singleRegistry.getAllTools();
        assertEquals(1, tools.size());

        Optional<Tool> retrieved = singleRegistry.getTool("single_tool");
        assertTrue(retrieved.isPresent());
    }

    @Test
    void testGetToolByName() {
        Optional<Tool> fileRead = toolRegistry.getTool("file_read");
        Optional<Tool> browserNav = toolRegistry.getTool("browser_navigate");

        assertTrue(fileRead.isPresent());
        assertTrue(browserNav.isPresent());

        assertEquals("file_read", fileRead.get().getName());
        assertEquals("browser_navigate", browserNav.get().getName());
    }

    @Test
    void testToolDescriptionsFormat() {
        String descriptions = toolRegistry.getToolDescriptions();

        // Should contain tool name, description, and signature
        assertTrue(descriptions.contains("file_read:"));
        assertTrue(descriptions.contains("Signature:"));
        assertTrue(descriptions.contains("file_read(path: str)"));
    }

    @Test
    void testPythonBindingsFormat() {
        String bindings = toolRegistry.generatePythonBindings();

        // Should contain proper Python function definitions
        assertTrue(bindings.contains("def "));
        assertTrue(bindings.contains("'''"));  // Docstrings
        assertTrue(bindings.contains("return _execute_tool("));
    }

    @Test
    void testRegisterToolOverwrite() {
        // Register a tool with same name
        Tool newFileRead = mock(Tool.class);
        when(newFileRead.getName()).thenReturn("file_read");
        when(newFileRead.getDescription()).thenReturn("New file read implementation");
        when(newFileRead.getPythonSignature()).thenReturn("file_read(path: str)");

        toolRegistry.registerTool(newFileRead);

        Optional<Tool> tool = toolRegistry.getTool("file_read");
        assertTrue(tool.isPresent());
        assertEquals("New file read implementation", tool.get().getDescription());
    }

    @Test
    void testToolCount() {
        Collection<Tool> tools = toolRegistry.getAllTools();
        assertEquals(2, tools.size());

        // Add another tool
        Tool additionalTool = mock(Tool.class);
        when(additionalTool.getName()).thenReturn("additional_tool");
        when(additionalTool.getDescription()).thenReturn("Additional");
        when(additionalTool.getPythonSignature()).thenReturn("additional_tool()");

        toolRegistry.registerTool(additionalTool);

        Collection<Tool> updatedTools = toolRegistry.getAllTools();
        assertEquals(3, updatedTools.size());
    }
}
