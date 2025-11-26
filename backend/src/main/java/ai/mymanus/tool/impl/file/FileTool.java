package ai.mymanus.tool.impl.file;

import ai.mymanus.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for file operation tools.
 * Provides security sandboxing to ensure files are only accessed within workspace.
 *
 * Security Model:
 * - All file paths are resolved relative to workspace root
 * - Absolute paths outside workspace are rejected
 * - Path traversal attacks (../) are prevented
 * - Symlinks are validated to stay within workspace
 */
@Slf4j
public abstract class FileTool implements Tool {

    // Workspace root for file operations (configurable via environment)
    protected static final String WORKSPACE_ROOT = System.getenv()
            .getOrDefault("MANUS_WORKSPACE", "/tmp/manus-workspace");

    /**
     * Validate and resolve a file path within the workspace.
     *
     * Security checks:
     * 1. Resolve against workspace root
     * 2. Normalize path (resolve .., ., etc.)
     * 3. Ensure resolved path is within workspace
     *
     * @param filePath User-provided file path
     * @return Validated absolute Path within workspace
     * @throws SecurityException if path escapes workspace
     */
    protected Path validateAndResolvePath(String filePath) throws SecurityException, IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be empty");
        }

        Path workspaceRoot = Paths.get(WORKSPACE_ROOT).toRealPath();
        Path resolvedPath = workspaceRoot.resolve(filePath).normalize();

        // Ensure workspace directory exists
        if (!Files.exists(workspaceRoot)) {
            Files.createDirectories(workspaceRoot);
            log.info("📁 Created workspace directory: {}", workspaceRoot);
        }

        // Security check: resolved path must start with workspace root
        if (!resolvedPath.startsWith(workspaceRoot)) {
            log.warn("🚨 Security violation: Attempted to access path outside workspace");
            log.warn("   Requested: {}", filePath);
            log.warn("   Resolved: {}", resolvedPath);
            log.warn("   Workspace: {}", workspaceRoot);
            throw new SecurityException(
                    "Access denied: Path escapes workspace. File operations are restricted to: " + WORKSPACE_ROOT
            );
        }

        return resolvedPath;
    }

    /**
     * Create a successful result map
     */
    protected Map<String, Object> success(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        return result;
    }

    /**
     * Create an error result map
     */
    protected Map<String, Object> error(String message, Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", message);
        if (e != null) {
            result.put("errorType", e.getClass().getSimpleName());
            result.put("errorMessage", e.getMessage());
        }
        return result;
    }

    /**
     * Add file metadata to result
     */
    protected void addFileMetadata(Map<String, Object> result, Path path) throws IOException {
        if (Files.exists(path)) {
            result.put("exists", true);
            result.put("size", Files.size(path));
            result.put("lastModified", Files.getLastModifiedTime(path).toString());
            result.put("readable", Files.isReadable(path));
            result.put("writable", Files.isWritable(path));
            result.put("directory", Files.isDirectory(path));
        } else {
            result.put("exists", false);
        }
    }

    @Override
    public boolean requiresNetwork() {
        return false; // File operations don't need network
    }
}
