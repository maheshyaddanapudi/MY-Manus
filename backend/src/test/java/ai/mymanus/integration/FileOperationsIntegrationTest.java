package ai.mymanus.integration;

import ai.mymanus.tool.impl.file.FileReadTool;
import ai.mymanus.tool.impl.file.FileWriteTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for file operations
 */
@SpringBootTest
@ActiveProfiles("test")
class FileOperationsIntegrationTest {

    @Autowired
    private FileWriteTool fileWriteTool;

    @Autowired
    private FileReadTool fileReadTool;

    @Test
    void testWriteAndReadFile() throws Exception {
        String testFile = "integration-test.txt";
        String testContent = "Integration test content";

        // Write file
        Map<String, Object> writeParams = new HashMap<>();
        writeParams.put("path", testFile);
        writeParams.put("content", testContent);

        Map<String, Object> writeResult = fileWriteTool.execute(writeParams);
        assertTrue((Boolean) writeResult.get("success"));

        // Read file
        Map<String, Object> readParams = new HashMap<>();
        readParams.put("path", testFile);

        Map<String, Object> readResult = fileReadTool.execute(readParams);
        assertTrue((Boolean) readResult.get("success"));
        assertEquals(testContent, readResult.get("content"));
    }
}
