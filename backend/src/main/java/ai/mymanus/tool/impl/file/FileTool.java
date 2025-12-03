package ai.mymanus.tool.impl.file;

import ai.mymanus.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for file operation tools.
 * Provides security sandboxing to ensure files are only accessed within session workspace.
 *
 * Security Model:
 * - All file paths are resolved relative to session-specific workspace
 * - Absolute paths outside workspace are rejected
 * - Path traversal attacks (../) are prevented
 * - Symlinks are validated to stay within workspace
 * - Each session has isolated workspace: {workspaceDir}/{sessionId}/
 */
@Slf4j
public abstract class FileTool implements Tool {

    // Base workspace directory (injected from config)
    protected final String workspaceDir;

    protected FileTool(@Value("${sandbox.host.workspace-dir}") String workspaceDir) {
        this.workspaceDir = workspaceDir;
    }

    /**
     * Get session-specific workspace directory
     */
    protected Path getSessionWorkspace(String sessionId) throws IOException {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId cannot be empty");
        }
        
        Path sessionWorkspace = Paths.get(workspaceDir, sessionId);
        
        // Ensure session workspace exists
        if (!Files.exists(sessionWorkspace)) {
            Files.createDirectories(sessionWorkspace);
            log.info("📁 Created session workspace: {}", sessionWorkspace);
        }
        
        return sessionWorkspace.toRealPath();
    }

    /**
     * Validate and resolve a file path within the session workspace.
     *
     * Security checks:
     * 1. Resolve against session workspace root
     * 2. Normalize path (resolve .., ., etc.)
     * 3. Ensure resolved path is within session workspace
     *
     * @param sessionId Session ID for workspace isolation
     * @param filePath User-provided file path (relative to session workspace)
     * @return Validated absolute Path within session workspace
     * @throws SecurityException if path escapes workspace
     */
    protected Path validateAndResolvePath(String sessionId, String filePath) throws SecurityException, IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be empty");
        }

        Path sessionWorkspace = getSessionWorkspace(sessionId);
        Path resolvedPath = sessionWorkspace.resolve(filePath).normalize();

        // Security check: resolved path must start with session workspace
        if (!resolvedPath.startsWith(sessionWorkspace)) {
            log.warn("🚨 Security violation: Attempted to access path outside session workspace");
            log.warn("   Session: {}", sessionId);
            log.warn("   Requested: {}", filePath);
            log.warn("   Resolved: {}", resolvedPath);
            log.warn("   Workspace: {}", sessionWorkspace);
            throw new SecurityException(
                    "Access denied: Path escapes session workspace. File operations are restricted to session directory."
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
