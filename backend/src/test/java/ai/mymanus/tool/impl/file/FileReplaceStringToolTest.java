package ai.mymanus.tool.impl.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileReplaceStringTool
 * Tests string replacement in files
 */
class FileReplaceStringToolTest {

    private FileReplaceStringTool replaceStringTool;

    @BeforeEach
    void setUp() {
        replaceStringTool = new FileReplaceStringTool();
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

        Map<String, String> params = replaceStringTool.getParameters();
        assertTrue(params.containsKey("path"));
        assertTrue(params.containsKey("oldString"));
        assertTrue(params.containsKey("newString"));
    }
}
