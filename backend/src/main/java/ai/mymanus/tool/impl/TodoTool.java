package ai.mymanus.tool.impl;

import ai.mymanus.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Todo.md Planner Tool - Maintains a todo list for the agent
 *
 * Manus AI Equivalent: Agents use todo.md to track multi-step tasks
 *
 * Features:
 * - Read/write /workspace/todo.md
 * - Agent can track progress across iterations
 * - Helpful for complex multi-step workflows
 */
@Slf4j
@Component
public class TodoTool implements Tool {

    private final String workspaceDir;

    public TodoTool(@org.springframework.beans.factory.annotation.Value("${sandbox.host.workspace-dir}") String workspaceDir) {
        this.workspaceDir = workspaceDir;
    }

    @Override
    public String getName() {
        return "todo";
    }

    @Override
    public String getDescription() {
        return "Read or write the todo.md file. Use 'read' action to view todos, 'write' action to update them. " +
               "Helps track multi-step tasks across iterations.";
    }

    @Override
    public String getPythonSignature() {
        return "todo(sessionId: str, action: str, content: str = None) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = (String) parameters.get("sessionId");
        String action = (String) parameters.get("action");
        String content = (String) parameters.get("content");

        if (sessionId == null || sessionId.isEmpty()) {
            return error("sessionId parameter required", null);
        }

        if (action == null) {
            return error("action parameter required (read/write)", null);
        }

        log.info("📝 Todo action: {} for session: {}", action, sessionId);

        try {
            Path todoPath = Paths.get(workspaceDir, sessionId, "todo.md");

            // Ensure parent directory exists
            Files.createDirectories(todoPath.getParent());

            if ("read".equals(action)) {
                return readTodo(todoPath);
            } else if ("write".equals(action)) {
                return writeTodo(todoPath, content);
            } else {
                return error("Invalid action. Use 'read' or 'write'", null);
            }

        } catch (Exception e) {
            log.error("❌ Todo operation failed", e);
            return error("Todo operation failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> readTodo(Path todoPath) throws IOException {
        var result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("action", "read");

        if (Files.exists(todoPath)) {
            String content = Files.readString(todoPath);
            result.put("content", content);
            result.put("exists", true);
            log.info("✅ Read todo.md ({} bytes)", content.length());
        } else {
            result.put("content", "");
            result.put("exists", false);
            log.info("ℹ️ todo.md does not exist yet");
        }

        return result;
    }

    private Map<String, Object> writeTodo(Path todoPath, String content) throws IOException {
        if (content == null) {
            return error("content parameter required for write action", null);
        }

        Files.writeString(todoPath, content);

        var result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("action", "write");
        result.put("bytesWritten", content.length());

        log.info("✅ Wrote todo.md ({} bytes)", content.length());

        return result;
    }

    private Map<String, Object> error(String message, Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", message);
        if (e != null) {
            result.put("errorMessage", e.getMessage());
        }
        return result;
    }

    @Override
    public boolean requiresNetwork() {
        return false;
    }
}
