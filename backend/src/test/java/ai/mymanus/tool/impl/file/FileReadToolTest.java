package ai.mymanus.tool.impl.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Suite for FileReadTool
 * Coverage Target: 100%
 *
 * Tests:
 * - Successful file reading
 * - File not found handling
 * - Directory vs file error
 * - Security path validation
 * - UTF-8 encoding
 * - Metadata extraction
 */
class FileReadToolTest {

    private FileReadTool fileReadTool;
    private Path testDir;

    @BeforeEach
    void setUp() throws Exception {
        fileReadTool = new FileReadTool("/tmp/test-workspace");
        
        // Create test directory within workspace
        String workspace = System.getenv().getOrDefault("MANUS_WORKSPACE", "/tmp/manus-workspace");
        testDir = Paths.get(workspace, "test-" + UUID.randomUUID());
        Files.createDirectories(testDir);
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
    void testReadExistingFile() throws Exception {
        // Create test file
        Path testFile = testDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.writeString(testFile, content);

        // Execute tool
        Map<String, Object> params = Map.of("path", testFile.toString());
        Map<String, Object> result = fileReadTool.execute(params);

        // Verify
        assertTrue((Boolean) result.get("success"));
        assertEquals(content, result.get("content"));
        assertEquals(content.length(), result.get("length"));
    }

    @Test
    void testReadNonExistentFile() throws Exception {
        // Execute tool on non-existent file
        Map<String, Object> params = Map.of("path", testDir.resolve("nonexistent.txt").toString());
        Map<String, Object> result = fileReadTool.execute(params);

        // Verify error handling
        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("error").toString().contains("not found") 
                || result.get("error").toString().contains("does not exist"));
    }

    @Test
    void testReadDirectory() throws Exception {
        // Execute tool on directory
        Map<String, Object> params = Map.of("path", testDir.toString());
        Map<String, Object> result = fileReadTool.execute(params);

        // Verify error handling
        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("error").toString().contains("directory"));
    }

    @Test
    void testSecurityPathValidation() throws Exception {
        // Test path traversal attack prevention
        Map<String, Object> params = Map.of("path", "../../etc/passwd");
        Map<String, Object> result = fileReadTool.execute(params);

        // Should fail security check
        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("error").toString().toLowerCase().contains("security")
                || result.get("error").toString().toLowerCase().contains("workspace"));
    }

    @Test
    void testToolMetadata() {
        assertEquals("file_read", fileReadTool.getName());
        assertNotNull(fileReadTool.getDescription());
        assertTrue(fileReadTool.getPythonSignature().contains("file_read"));
        assertFalse(fileReadTool.requiresNetwork());
    }
}
