package ai.mymanus.controller;

import ai.mymanus.service.sandbox.PythonSandboxExecutor;
import ai.mymanus.service.sandbox.ExecutionResult;
import ai.mymanus.tool.Tool;
import ai.mymanus.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SandboxController
 * Tests sandbox management API
 */
@WebMvcTest(SandboxController.class)
class SandboxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PythonSandboxExecutor pythonSandboxExecutor;

    @MockBean
    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    @Test
    void testExecutePython() throws Exception {
        String requestJson = """
            {
                "sessionId": "test-session",
                "code": "print('Hello, World!')",
                "context": {}
            }
            """;

        ExecutionResult executionResult = ExecutionResult.builder()
            .stdout("Hello, World!\n")
            .stderr("")
            .exitCode(0)
            .success(true)
            .build();

        when(pythonSandboxExecutor.execute(anyString(), anyString(), any()))
            .thenReturn(executionResult);

        mockMvc.perform(post("/api/sandbox/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stdout").value("Hello, World!\n"))
            .andExpect(jsonPath("$.exitCode").value(0));

        verify(pythonSandboxExecutor, times(1))
            .execute(eq("test-session"), eq("print('Hello, World!')"), any());
    }

    @Test
    void testExecutePythonWithError() throws Exception {
        String requestJson = """
            {
                "sessionId": "test-session",
                "code": "1 / 0",
                "context": {}
            }
            """;

        ExecutionResult executionResult = ExecutionResult.builder()
            .stdout("")
            .stderr("ZeroDivisionError: division by zero")
            .exitCode(1)
            .success(false)
            .build();

        when(pythonSandboxExecutor.execute(anyString(), anyString(), any()))
            .thenReturn(executionResult);

        mockMvc.perform(post("/api/sandbox/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exitCode").value(1))
            .andExpect(jsonPath("$.stderr").value("ZeroDivisionError: division by zero"));

        verify(pythonSandboxExecutor, times(1))
            .execute(eq("test-session"), eq("1 / 0"), any());
    }

    @Test
    void testExecutePythonWithContext() throws Exception {
        String requestJson = """
            {
                "sessionId": "test-session",
                "code": "print(x)",
                "context": {"x": 42}
            }
            """;

        ExecutionResult executionResult = ExecutionResult.builder()
            .stdout("42\n")
            .stderr("")
            .exitCode(0)
            .success(true)
            .build();

        when(pythonSandboxExecutor.execute(anyString(), anyString(), any()))
            .thenReturn(executionResult);

        mockMvc.perform(post("/api/sandbox/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stdout").value("42\n"));

        verify(pythonSandboxExecutor, times(1))
            .execute(eq("test-session"), eq("print(x)"), any());
    }

    @Test
    void testGetToolBindings() throws Exception {
        String toolBindings = """
            # Tool Functions
            def file_read(path):
                '''Read a file from workspace'''
                pass
            """;

        when(toolRegistry.generatePythonBindings())
            .thenReturn(toolBindings);

        mockMvc.perform(get("/api/sandbox/tool-bindings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bindings").value(toolBindings));

        verify(toolRegistry, times(1)).generatePythonBindings();
    }

    @Test
    void testExecuteTool() throws Exception {
        String requestJson = """
            {
                "toolName": "file_read",
                "parameters": {
                    "path": "/workspace/test.txt"
                }
            }
            """;

        Map<String, Object> toolResult = new HashMap<>();
        toolResult.put("success", true);
        toolResult.put("content", "File content");

        Tool mockTool = mock(Tool.class);
        when(mockTool.execute(any())).thenReturn(toolResult);
        when(toolRegistry.getTool(eq("file_read"))).thenReturn(Optional.of(mockTool));

        mockMvc.perform(post("/api/sandbox/execute-tool")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.content").value("File content"));

        verify(toolRegistry, times(1)).getTool(eq("file_read"));
        verify(mockTool, times(1)).execute(any());
    }

    @Test
    void testExecuteToolNotFound() throws Exception {
        String requestJson = """
            {
                "toolName": "non_existent_tool",
                "parameters": {}
            }
            """;

        when(toolRegistry.getTool(eq("non_existent_tool")))
            .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/sandbox/execute-tool")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isNotFound());

        verify(toolRegistry, times(1)).getTool(eq("non_existent_tool"));
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/sandbox/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("healthy"));
    }

    @Test
    void testInvalidExecuteRequest() throws Exception {
        String invalidJson = "{ invalid json }";

        mockMvc.perform(post("/api/sandbox/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingCode() throws Exception {
        String requestJson = """
            {
                "sessionId": "test-session",
                "context": {}
            }
            """;

        mockMvc.perform(post("/api/sandbox/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingSessionId() throws Exception {
        String requestJson = """
            {
                "code": "print('test')",
                "context": {}
            }
            """;

        mockMvc.perform(post("/api/sandbox/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testExecuteWithVariables() throws Exception {
        String requestJson = """
            {
                "sessionId": "test-session",
                "code": "result = x + y",
                "context": {"x": 10, "y": 20}
            }
            """;

        Map<String, Object> newVariables = new HashMap<>();
        newVariables.put("result", 30);

        ExecutionResult executionResult = ExecutionResult.builder()
            .stdout("")
            .stderr("")
            .exitCode(0)
            .success(true)
            .variables(newVariables)
            .build();

        when(pythonSandboxExecutor.execute(anyString(), anyString(), any()))
            .thenReturn(executionResult);

        mockMvc.perform(post("/api/sandbox/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(pythonSandboxExecutor, times(1))
            .execute(eq("test-session"), eq("result = x + y"), any());
    }
}
