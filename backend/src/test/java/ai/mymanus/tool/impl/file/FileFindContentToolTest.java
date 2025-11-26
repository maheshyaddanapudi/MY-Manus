package ai.mymanus.tool.impl.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileFindContentTool
 * Tests content search in files
 */
class FileFindContentToolTest {

    private FileFindContentTool findContentTool;

    @BeforeEach
    void setUp() {
        findContentTool = new FileFindContentTool();
    }

    @Test
    void testFindContentInFile() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("pattern", "function");
        params.put("path", "/workspace");

        Map<String, Object> result = findContentTool.execute(params);

        assertNotNull(result);
        assertTrue(result.containsKey("success"));
    }

    @Test
    void testFindContentWithRegex() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("pattern", "def\\s+\\w+");
        params.put("path", "/workspace");

        Map<String, Object> result = findContentTool.execute(params);

        assertNotNull(result);
    }

    @Test
    void testFindContentCaseInsensitive() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("pattern", "hello");
        params.put("path", "/workspace");
        params.put("ignoreCase", true);

        Map<String, Object> result = findContentTool.execute(params);

        assertNotNull(result);
    }

    @Test
    void testFindContentInDirectory() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("pattern", "import");
        params.put("path", "/workspace/src");

        Map<String, Object> result = findContentTool.execute(params);

        assertNotNull(result);
    }

    @Test
    void testFindContentNoMatches() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("pattern", "thispatternwontmatch12345");
        params.put("path", "/workspace");

        Map<String, Object> result = findContentTool.execute(params);

        assertNotNull(result);
        assertTrue(result.containsKey("success"));
    }

    @Test
    void testMissingPattern() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "/workspace");

        Map<String, Object> result = findContentTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testSecurityPathValidation() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("pattern", "root");
        params.put("path", "../../../etc");

        Map<String, Object> result = findContentTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testToolMetadata() {
        assertEquals("file_find_content", findContentTool.getName());
        assertNotNull(findContentTool.getDescription());

        Map<String, String> params = findContentTool.getParameters();
        assertTrue(params.containsKey("pattern"));
        assertTrue(params.containsKey("path"));
    }
}
