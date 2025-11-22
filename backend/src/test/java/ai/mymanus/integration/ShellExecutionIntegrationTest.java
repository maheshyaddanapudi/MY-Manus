package ai.mymanus.integration;

import ai.mymanus.tool.impl.shell.ShellExecTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for shell execution
 */
@SpringBootTest
@ActiveProfiles("test")
class ShellExecutionIntegrationTest {

    @Autowired
    private ShellExecTool shellExecTool;

    @Test
    void testShellCommandExecution() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("command", "echo 'Hello from shell'");
        params.put("timeout", 30);

        Map<String, Object> result = shellExecTool.execute(params);

        assertNotNull(result);
        assertTrue(result.containsKey("success"));
        // ShellExecTool returns Python code (CodeAct pattern)
        assertTrue(result.containsKey("pythonCode"));
    }

    @Test
    void testShellCommandWithPipes() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("command", "ls | head -n 5");
        params.put("timeout", 30);

        Map<String, Object> result = shellExecTool.execute(params);

        assertNotNull(result);
        assertTrue(result.containsKey("pythonCode"));
    }
}
