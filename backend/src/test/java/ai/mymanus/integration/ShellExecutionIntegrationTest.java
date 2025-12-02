package ai.mymanus.integration;

import ai.mymanus.MyManusApplication;
import ai.mymanus.config.IntegrationTestConfiguration;
import ai.mymanus.tool.impl.shell.ShellExecTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for shell execution
 */
@SpringBootTest(classes = MyManusApplication.class)
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ShellExecutionIntegrationTest {

    @Autowired
    private ShellExecTool shellExecTool;

    @Test
    void testShellCommandExecution() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", "test-session-" + System.currentTimeMillis());
        params.put("command", "echo 'Hello from shell'");
        params.put("timeout", 30);

        Map<String, Object> result = shellExecTool.execute(params);

        assertNotNull(result);
        assertTrue(result.containsKey("success"));
        // ShellExecTool returns stdout, stderr, exitCode
        assertTrue(result.containsKey("stdout"));
        assertTrue(result.containsKey("stderr"));
        assertTrue(result.containsKey("exitCode"));
    }

    @Test
    void testShellCommandWithPipes() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", "test-session-" + System.currentTimeMillis());
        params.put("command", "ls | head -n 5");
        params.put("timeout", 30);

        Map<String, Object> result = shellExecTool.execute(params);

        assertNotNull(result);
        assertTrue(result.containsKey("success"));
        assertTrue(result.containsKey("stdout"));
    }
}
