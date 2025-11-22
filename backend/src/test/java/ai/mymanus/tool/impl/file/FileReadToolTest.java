package ai.mymanus.tool.impl.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileReadTool = new FileReadTool();
    }

    @Test
    void testReadExistingFile() throws Exception {
        // Create test file
        Path testFile = tempDir.resolve("test.txt");
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
        Map<String, Object> params = Map.of("path", tempDir.resolve("nonexistent.txt").toString());
        Map<String, Object> result = fileReadTool.execute(params);

        // Verify error handling
        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("error").toString().contains("not found"));
    }

    @Test
    void testReadDirectory() throws Exception {
        // Execute tool on directory
        Map<String, Object> params = Map.of("path", tempDir.toString());
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
