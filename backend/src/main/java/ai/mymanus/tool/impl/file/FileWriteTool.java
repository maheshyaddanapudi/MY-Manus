package ai.mymanus.tool.impl.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * Tool for writing content to files.
 *
 * Manus AI Equivalent: file_write(path: str, content: str) -> None
 *
 * Features:
 * - Writes content to file (creates if doesn't exist)
 * - Overwrites existing files
 * - Creates parent directories if needed
 * - UTF-8 encoding by default
 * - Security: Sandboxed to workspace directory
 */
@Slf4j
@Component
public class FileWriteTool extends FileTool {

    public FileWriteTool(@Value("${sandbox.host.workspace-dir}") String workspaceDir) {
        super(workspaceDir);
    }

    @Override
    public String getName() {
        return "file_write";
    }

    @Override
    public String getDescription() {
        return "Write content to a file. Creates the file if it doesn't exist, overwrites if it does. " +
               "Parent directories are created automatically.";
    }

    @Override
    public String getPythonSignature() {
        return "file_write(sessionId: str, path: str, content: str) -> None";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = (String) parameters.get("sessionId");
        String filePath = (String) parameters.get("path");
        String content = (String) parameters.get("content");

        if (sessionId == null || sessionId.trim().isEmpty()) {
            return error("sessionId parameter required", null);
        }

        if (content == null) {
            content = ""; // Allow writing empty files
        }

        log.info("✍️ Writing file: {} ({} bytes, session: {})", filePath, content.length(), sessionId);

        try {
            // Validate and resolve path with security checks
            Path resolvedPath = validateAndResolvePath(sessionId, filePath);

            // Create parent directories if they don't exist
            Path parentDir = resolvedPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                log.info("📁 Created parent directories: {}", parentDir);
            }

            // Check if path exists and is a directory
            if (Files.exists(resolvedPath) && Files.isDirectory(resolvedPath)) {
                log.warn("⚠️ Cannot write to directory: {}", resolvedPath);
                return error("Path is a directory, not a file: " + filePath, null);
            }

            // Write content to file
            Files.writeString(
                    resolvedPath,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            log.info("✅ Successfully wrote file: {} ({} bytes)", filePath, content.length());

            // Build result
            var result = success("File written successfully");
            result.put("path", filePath);
            result.put("bytesWritten", content.length());
            result.put("lines", content.split("\n").length);
            addFileMetadata(result, resolvedPath);

            return result;

        } catch (SecurityException e) {
            log.error("🚨 Security violation writing file: {}", filePath, e);
            return error("Security violation: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Error writing file: {}", filePath, e);
            return error("Failed to write file: " + e.getMessage(), e);
        }
    }
}
