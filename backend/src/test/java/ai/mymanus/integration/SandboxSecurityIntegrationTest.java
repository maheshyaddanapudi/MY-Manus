package ai.mymanus.integration;

import ai.mymanus.MyManusApplication;
import ai.mymanus.config.IntegrationTestConfiguration;
import ai.mymanus.tool.impl.file.FileReadTool;
import ai.mymanus.tool.impl.file.FileWriteTool;
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
 * Integration test for sandbox security validation
 */
@SpringBootTest(classes = MyManusApplication.class)
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class SandboxSecurityIntegrationTest {

    @Autowired
    private FileReadTool fileReadTool;

    @Autowired
    private FileWriteTool fileWriteTool;

    @Test
    void testPathTraversalPrevention() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "../../../etc/passwd");

        Map<String, Object> result = fileReadTool.execute(params);

        assertFalse((Boolean) result.get("success"));
        String error = result.get("error").toString();
        assertTrue(error.toLowerCase().contains("security") ||
                   error.toLowerCase().contains("workspace"));
    }

    @Test
    void testWorkspaceRestriction() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "/etc/passwd");

        Map<String, Object> result = fileReadTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testValidWorkspacePath() throws Exception {
        Map<String, Object> writeParams = new HashMap<>();
        // Use relative path which will be resolved to workspace root
        writeParams.put("path", "safe-file.txt");
        writeParams.put("content", "Safe content");

        Map<String, Object> result = fileWriteTool.execute(writeParams);

        assertTrue((Boolean) result.get("success"));
    }
}
