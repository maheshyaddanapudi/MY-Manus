package ai.mymanus.tool.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TodoTool
 * Tests todo.md planner functionality
 */
class TodoToolTest {

    @TempDir
    Path tempWorkspace;

    private TodoTool todoTool;

    @BeforeEach
    void setUp() {
        todoTool = new TodoTool();
    }

    @Test
    void testReadTodo() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("action", "read");

        Map<String, Object> result = todoTool.execute(params);

        assertNotNull(result);
        assertTrue(result.containsKey("success"));
    }

    @Test
    void testWriteTodo() throws Exception {
        String todoContent = """
            # Todo List
            - [ ] Task 1
            - [ ] Task 2
            - [x] Task 3 (completed)
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("action", "write");
        params.put("content", todoContent);

        Map<String, Object> result = todoTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testWriteAndReadTodo() throws Exception {
        // Write todo
        String todoContent = "- [ ] Implement feature X";

        Map<String, Object> writeParams = new HashMap<>();
        writeParams.put("action", "write");
        writeParams.put("content", todoContent);
        todoTool.execute(writeParams);

        // Read todo
        Map<String, Object> readParams = new HashMap<>();
        readParams.put("action", "read");

        Map<String, Object> result = todoTool.execute(readParams);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testUpdateTodo() throws Exception {
        // Write initial todo
        Map<String, Object> params1 = new HashMap<>();
        params1.put("action", "write");
        params1.put("content", "- [ ] Task 1");
        todoTool.execute(params1);

        // Update todo
        Map<String, Object> params2 = new HashMap<>();
        params2.put("action", "write");
        params2.put("content", "- [x] Task 1 (completed)\n- [ ] Task 2");

        Map<String, Object> result = todoTool.execute(params2);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testEmptyTodo() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("action", "write");
        params.put("content", "");

        Map<String, Object> result = todoTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testInvalidAction() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("action", "invalid");

        Map<String, Object> result = todoTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testMissingAction() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("content", "Some content");

        Map<String, Object> result = todoTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testWriteMissingContent() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("action", "write");

        Map<String, Object> result = todoTool.execute(params);

        assertFalse((Boolean) result.get("success"));
    }

    @Test
    void testMarkdownFormatting() throws Exception {
        String todoContent = """
            # Project Todo List

            ## Phase 1
            - [x] Setup project
            - [x] Create models

            ## Phase 2
            - [ ] Implement tools
            - [ ] Add tests

            ## Notes
            Remember to commit after each phase.
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("action", "write");
        params.put("content", todoContent);

        Map<String, Object> result = todoTool.execute(params);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testToolMetadata() {
        assertEquals("todo", todoTool.getName());
        assertNotNull(todoTool.getDescription());

        String signature = todoTool.getPythonSignature();
        assertNotNull(signature);
        assertTrue(signature.contains("action"));
        assertTrue(signature.contains("content"));
    }
}
