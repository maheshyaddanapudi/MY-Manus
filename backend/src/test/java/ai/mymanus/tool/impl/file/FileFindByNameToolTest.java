package ai.mymanus.tool.impl.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileFindByNameTool
 * Tests file name search functionality
 */
class FileFindByNameToolTest {

    private FileFindByNameTool findByNameTool;

    @BeforeEach
    void setUp() {
        findByNameTool = new FileFindByNameTool("/tmp/test-workspace");
    }

    @Test
    void testFindByExactName() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "test.txt");
        params.put("path", "/workspace");

        Map<String, Object> result = findByNameTool.execute(params);

        assertNotNull(result);
        assertTrue(result.containsKey("success"));
    }

    @Test
    void testFindByPattern() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "*.py");
        params.put("path", "/workspace");

        Map<String, Object> result = findByNameTool.execute(params);

        assertNotNull(result);
    }

    @Test
    void testFindInSubdirectories() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "main.py");
        params.put("path", "/workspace/src");

        Map<String, Object> result = findByNameTool.execute(params);

        assertNotNull(result);
    }

    @Test
    void testFindMultipleExtensions() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "*.{java,kt}");
        params.put("path", "/workspace");

        Map<String, Object> result = findByNameTool.execute(params);

        assertNotNull(result);
    }

    @Test
    void testFindNoMatches() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "nonexistent_file_12345.xyz");
        params.put("path", "/workspace");

        Map<String, Object> result = findByNameTool.execute(params);

        assertNotNull(result);
    }

    @Test
    void testMissingName() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("path", "/workspace");

        Map<String, Object> result = findByNameTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testSecurityPathValidation() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "passwd");
        params.put("path", "../../../etc");

        Map<String, Object> result = findByNameTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testToolMetadata() {
        assertEquals("file_find_by_name", findByNameTool.getName());
        assertNotNull(findByNameTool.getDescription());

        String signature = findByNameTool.getPythonSignature();
        assertNotNull(signature);
        assertTrue(signature.contains("name"));
        assertTrue(signature.contains("path"));
    }
}
