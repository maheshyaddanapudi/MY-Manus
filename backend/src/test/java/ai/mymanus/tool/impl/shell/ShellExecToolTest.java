package ai.mymanus.tool.impl.shell;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ShellExecTool
 * Tests shell command execution via CodeAct pattern
 */
class ShellExecToolTest {

    private ShellExecTool shellExecTool;

    @BeforeEach
    void setUp() {
        shellExecTool = new ShellExecTool();
    }

    @Test
    void testSimpleCommand() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("command", "echo 'Hello'");
        params.put("timeout", 30);

        Map<String, Object> result = shellExecTool.execute(params);

        assertNotNull(result);
        assertTrue(result.containsKey("success"));
        // ShellExecTool generates Python code, not direct execution
        assertTrue(result.containsKey("pythonCode"));
    }

    @Test
    void testListFiles() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("command", "ls -la");
        params.put("timeout", 30);

        Map<String, Object> result = shellExecTool.execute(params);

        assertNotNull(result);
        assertTrue(result.get("pythonCode").toString().contains("subprocess"));
    }

    @Test
    void testWithTimeout() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("command", "sleep 1");
        params.put("timeout", 60);

        Map<String, Object> result = shellExecTool.execute(params);

        assertNotNull(result);
        assertTrue(result.get("pythonCode").toString().contains("60"));
    }

    @Test
    void testDefaultTimeout() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("command", "pwd");

        Map<String, Object> result = shellExecTool.execute(params);

        assertNotNull(result);
        // Should use default timeout
        assertTrue(result.get("pythonCode").toString().contains("timeout"));
    }

    @Test
    void testMissingCommand() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("timeout", 30);

        Map<String, Object> result = shellExecTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testCommandWithPipes() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("command", "ls -la | grep .py");
        params.put("timeout", 30);

        Map<String, Object> result = shellExecTool.execute(params);

        assertNotNull(result);
        assertTrue(result.containsKey("pythonCode"));
    }

    @Test
    void testCommandWithRedirection() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("command", "echo 'test' > /workspace/output.txt");
        params.put("timeout", 30);

        Map<String, Object> result = shellExecTool.execute(params);

        assertNotNull(result);
    }

    @Test
    void testCodeActPattern() throws Exception {
        // Verify that ShellExecTool follows CodeAct pattern
        // It should generate Python code using subprocess
        Map<String, Object> params = new HashMap<>();
        params.put("command", "echo 'test'");
        params.put("timeout", 30);

        Map<String, Object> result = shellExecTool.execute(params);

        assertTrue(result.containsKey("pythonCode"));
        String pythonCode = result.get("pythonCode").toString();

        // Should contain subprocess module usage
        assertTrue(pythonCode.contains("subprocess") || pythonCode.contains("run"));
        // Should contain the command
        assertTrue(pythonCode.contains("echo"));
    }

    @Test
    void testToolMetadata() {
        assertEquals("shell_exec", shellExecTool.getName());
        assertNotNull(shellExecTool.getDescription());

        String signature = shellExecTool.getPythonSignature();
        assertNotNull(signature);
        assertTrue(signature.contains("command"));
        assertTrue(signature.contains("timeout"));
    }
}
