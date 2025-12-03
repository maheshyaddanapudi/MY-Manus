package ai.mymanus.tool.impl.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Tool to list files and directories in a tree structure
 * Supports filtering and depth limiting
 */
@Component
@Slf4j
public class FileListTool extends FileTool {

    public FileListTool(@Value("${sandbox.host.workspace-dir}") String workspaceDir) {
        super(workspaceDir);
    }

    private static final int MAX_DEPTH = 10;

    @Override
    public String getName() {
        return "file_list";
    }

    @Override
    public String getDescription() {
        return "List files and directories in tree structure. Returns file paths, sizes, and types.";
    }

    @Override
    public String getPythonSignature() {
        return "file_list(sessionId: str, path: str = \".\", maxDepth: int = 3, includeHidden: bool = False) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = (String) parameters.get("sessionId");
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return error("sessionId parameter required", null);
        }

        try {
            String pathStr = (String) parameters.getOrDefault("path", ".");
            int maxDepth = ((Number) parameters.getOrDefault("maxDepth", 3)).intValue();
            boolean includeHidden = (Boolean) parameters.getOrDefault("includeHidden", false);

            // Validate and resolve path with security checks
            Path resolvedPath = validateAndResolvePath(sessionId, pathStr);

            if (!Files.exists(resolvedPath)) {
                return error("Path does not exist: " + pathStr, null);
            }

            if (!Files.isDirectory(resolvedPath)) {
                return error("Path is not a directory: " + pathStr, null);
            }

            // Limit depth to prevent infinite recursion
            maxDepth = Math.min(maxDepth, MAX_DEPTH);

            List<Map<String, Object>> fileTree = buildFileTree(resolvedPath, maxDepth, includeHidden);

            var result = success("File tree generated successfully");
            result.put("files", fileTree);
            result.put("totalFiles", fileTree.size());
            result.put("rootPath", pathStr);

            log.info("📂 Listed {} files from {} (session: {})", fileTree.size(), pathStr, sessionId);

            return result;

        } catch (Exception e) {
            log.error("❌ Error listing files", e);
            return error("Failed to list files: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> buildFileTree(Path root, int maxDepth, boolean includeHidden) throws IOException {
        List<Map<String, Object>> fileList = new ArrayList<>();

        Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), maxDepth, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!includeHidden && dir.getFileName().toString().startsWith(".")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("path", root.relativize(dir).toString());
                fileInfo.put("name", dir.getFileName().toString());
                fileInfo.put("type", "directory");
                fileInfo.put("size", 0);
                fileInfo.put("lastModified", attrs.lastModifiedTime().toMillis());
                fileInfo.put("depth", root.relativize(dir).getNameCount());

                fileList.add(fileInfo);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!includeHidden && file.getFileName().toString().startsWith(".")) {
                    return FileVisitResult.CONTINUE;
                }

                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("path", root.relativize(file).toString());
                fileInfo.put("name", file.getFileName().toString());
                fileInfo.put("type", "file");
                fileInfo.put("size", attrs.size());
                fileInfo.put("lastModified", attrs.lastModifiedTime().toMillis());
                fileInfo.put("depth", root.relativize(file).getNameCount());
                fileInfo.put("extension", getFileExtension(file.getFileName().toString()));

                fileList.add(fileInfo);

                return FileVisitResult.CONTINUE;
            }
        });

        return fileList;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }
}
