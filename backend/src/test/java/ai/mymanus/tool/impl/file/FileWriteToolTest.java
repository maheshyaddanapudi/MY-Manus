package ai.mymanus.tool.impl.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileWriteTool
 * Tests file writing functionality and security
 */
class FileWriteToolTest {

    private Path testDir;

    private FileWriteTool fileWriteTool;

    @BeforeEach
    void setUp() throws Exception {
        // Create test directory within workspace
        String workspace = System.getenv().getOrDefault("MANUS_WORKSPACE", "/tmp/manus-workspace");
        testDir = Paths.get(workspace, "test-" + UUID.randomUUID());
        Files.createDirectories(testDir);
        fileWriteTool = new FileWriteTool();
    }
    @AfterEach
    void tearDown() throws Exception {
        // Clean up test directory
        if (testDir != null && Files.exists(testDir)) {
            Files.walk(testDir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                });
        }
    }


    @Test
    void testWriteNewFile() throws Exception {
        String content = "Hello, World!";
        String fileName = "test.txt";

        Map<String, Object> params = new HashMap<>();
        params.put("path", fileName);
        params.put("content", content);

        Map<String, Object> result = fileWriteTool.execute(params);

        assertTrue((Boolean) result.get("success"));
        assertEquals("File written successfully", result.get("message"));
    }

    @Test
    void testWriteMultilineContent() throws Exception {
        String content = "Line 1\nLine 2\nLine 3";
        String fileName = "multiline.txt";

        Map<String, Object> params = new HashMap<>();
        params.put("path", fileName);
        params.put("content", content);

        Map<String, Object> result = fileWriteTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testOverwriteExistingFile() throws Exception {
        String fileName = "overwrite.txt";

        // Write first time
        Map<String, Object> params1 = new HashMap<>();
        params1.put("path", fileName);
        params1.put("content", "Original content");
        fileWriteTool.execute(params1);

        // Overwrite
        Map<String, Object> params2 = new HashMap<>();
        params2.put("path", fileName);
        params2.put("content", "New content");
        Map<String, Object> result = fileWriteTool.execute(params2);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testWriteEmptyContent() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "empty.txt");
        params.put("content", "");

        Map<String, Object> result = fileWriteTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testWriteWithSpecialCharacters() throws Exception {
        String content = "Hello 世界! <>&\"' 🌍";

        Map<String, Object> params = new HashMap<>();
        params.put("path", "special.txt");
        params.put("content", content);

        Map<String, Object> result = fileWriteTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testWriteToSubdirectory() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "subdir/test.txt");
        params.put("content", "Content in subdirectory");

        Map<String, Object> result = fileWriteTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testSecurityPathTraversal() throws Exception {
        // Test path traversal attack prevention
        Map<String, Object> params = new HashMap<>();
        params.put("path", "../../../etc/passwd");
        params.put("content", "malicious content");

        Map<String, Object> result = fileWriteTool.execute(params);

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("error").toString().toLowerCase().contains("security") ||
                   result.get("error").toString().toLowerCase().contains("workspace"));
    }

    @Test
    void testMissingPath() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("content", "Content without path");

        Map<String, Object> result = fileWriteTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testMissingContent() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "test.txt");

        Map<String, Object> result = fileWriteTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testLargeFile() throws Exception {
        String largeContent = "a".repeat(100000);

        Map<String, Object> params = new HashMap<>();
        params.put("path", "large.txt");
        params.put("content", largeContent);

        Map<String, Object> result = fileWriteTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testToolMetadata() {
        assertEquals("file_write", fileWriteTool.getName());
        assertNotNull(fileWriteTool.getDescription());
        assertTrue(fileWriteTool.getDescription().contains("write") ||
                   fileWriteTool.getDescription().contains("Write"));

        String signature = fileWriteTool.getPythonSignature();
        assertNotNull(signature);
        assertTrue(signature.contains("path"));
        assertTrue(signature.contains("content"));
    }
}
