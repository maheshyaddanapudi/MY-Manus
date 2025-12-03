package ai.mymanus.tool.impl.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Tool for reading file contents.
 *
 * Manus AI Equivalent: file_read(path: str) -> str
 *
 * Features:
 * - Reads entire file content as string
 * - UTF-8 encoding by default
 * - Returns file metadata (size, modified time)
 * - Security: Sandboxed to workspace directory
 */
@Slf4j
@Component
public class FileReadTool extends FileTool {

    public FileReadTool(@Value("${sandbox.host.workspace-dir}") String workspaceDir) {
        super(workspaceDir);
    }

    @Override
    public String getName() {
        return "file_read";
    }

    @Override
    public String getDescription() {
        return "Read the entire contents of a file. Returns the file content as a string.";
    }

    @Override
    public String getPythonSignature() {
        return "file_read(sessionId: str, path: str) -> str";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = (String) parameters.get("sessionId");
        String filePath = (String) parameters.get("path");

        if (sessionId == null || sessionId.trim().isEmpty()) {
            return error("sessionId parameter required", null);
        }

        log.info("📖 Reading file: {} (session: {})", filePath, sessionId);

        try {
            // Validate and resolve path with security checks
            Path resolvedPath = validateAndResolvePath(sessionId, filePath);

            // Check if file exists
            if (!Files.exists(resolvedPath)) {
                log.warn("⚠️ File not found: {}", resolvedPath);
                return error("File not found: " + filePath, null);
            }

            // Check if path is a directory
            if (Files.isDirectory(resolvedPath)) {
                log.warn("⚠️ Cannot read directory as file: {}", resolvedPath);
                return error("Path is a directory, not a file: " + filePath, null);
            }

            // Check if file is readable
            if (!Files.isReadable(resolvedPath)) {
                log.warn("⚠️ File not readable: {}", resolvedPath);
                return error("File is not readable: " + filePath, null);
            }

            // Read file content
            String content = Files.readString(resolvedPath, StandardCharsets.UTF_8);

            log.info("✅ Successfully read file: {} ({} bytes)", filePath, content.length());

            // Build result
            var result = success("File read successfully");
            result.put("path", filePath);
            result.put("content", content);
            result.put("length", content.length());
            addFileMetadata(result, resolvedPath);

            return result;

        } catch (SecurityException e) {
            log.error("🚨 Security violation reading file: {}", filePath, e);
            return error("Security violation: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Error reading file: {}", filePath, e);
            return error("Failed to read file: " + e.getMessage(), e);
        }
    }
}
