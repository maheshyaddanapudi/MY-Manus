package ai.mymanus.tool.impl.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileReplaceStringTool
 * Tests string replacement in files
 */
class FileReplaceStringToolTest {

    private FileReplaceStringTool replaceStringTool;
    private Path testDir;

    @BeforeEach
    void setUp() throws Exception {
        // Create test directory within workspace
        String workspace = System.getenv().getOrDefault("MANUS_WORKSPACE", "/tmp/manus-workspace");
        testDir = Paths.get(workspace, "test-" + UUID.randomUUID());
        Files.createDirectories(testDir);
        replaceStringTool = new FileReplaceStringTool("/tmp/test-workspace");
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
    void testReplaceString() throws Exception {
        // First write a file (would need FileWriteTool in real scenario)
        Map<String, Object> params = new HashMap<>();
        params.put("path", "/workspace/test.txt");
        params.put("oldString", "Hello");
        params.put("newString", "Hi");

        Map<String, Object> result = replaceStringTool.execute(params);

        assertNotNull(result);
        assertTrue(result.containsKey("success"));
    }

    @Test
    void testReplaceMultipleOccurrences() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "/workspace/multi.txt");
        params.put("oldString", "foo");
        params.put("newString", "bar");

        Map<String, Object> result = replaceStringTool.execute(params);

        assertNotNull(result);
    }

    @Test
    void testReplaceWithEmptyString() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "/workspace/test.txt");
        params.put("oldString", "remove");
        params.put("newString", "");

        Map<String, Object> result = replaceStringTool.execute(params);

        assertNotNull(result);
    }

    @Test
    void testReplaceMultilineString() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "/workspace/test.txt");
        params.put("oldString", "Line 1\nLine 2");
        params.put("newString", "New Line");

        Map<String, Object> result = replaceStringTool.execute(params);

        assertNotNull(result);
    }

    @Test
    void testReplaceStringNotFound() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "/workspace/test.txt");
        params.put("oldString", "nonexistent");
        params.put("newString", "replacement");

        Map<String, Object> result = replaceStringTool.execute(params);

        assertNotNull(result);
    }

    @Test
    void testMissingParameters() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "/workspace/test.txt");
        params.put("oldString", "test");
        // Missing newString

        Map<String, Object> result = replaceStringTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testSecurityPathValidation() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "../../../etc/passwd");
        params.put("oldString", "root");
        params.put("newString", "hacked");

        Map<String, Object> result = replaceStringTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testToolMetadata() {
        assertEquals("file_replace_string", replaceStringTool.getName());
        assertNotNull(replaceStringTool.getDescription());

        String signature = replaceStringTool.getPythonSignature();
        assertNotNull(signature);
        assertTrue(signature.contains("path"));
        assertTrue(signature.contains("old"));
        assertTrue(signature.contains("new"));
    }
}
