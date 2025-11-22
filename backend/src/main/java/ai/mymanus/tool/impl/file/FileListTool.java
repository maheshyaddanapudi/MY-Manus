package ai.mymanus.tool.impl.file;

import ai.mymanus.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool to list files and directories in a tree structure
 * Supports filtering and depth limiting
 */
@Component
@Slf4j
public class FileListTool implements Tool {

    private static final String WORKSPACE = "/workspace";
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
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("path", "string - Directory path to list (default: /workspace)");
        params.put("maxDepth", "number - Maximum depth to traverse (default: 3)");
        params.put("includeHidden", "boolean - Include hidden files (default: false)");
        return params;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        try {
            String pathStr = (String) parameters.getOrDefault("path", WORKSPACE);
            int maxDepth = ((Number) parameters.getOrDefault("maxDepth", 3)).intValue();
            boolean includeHidden = (Boolean) parameters.getOrDefault("includeHidden", false);

            // Security: ensure path is within workspace
            Path path = Paths.get(pathStr).normalize();
            if (!path.toString().startsWith(WORKSPACE)) {
                return error("Security: Path must be within /workspace", null);
            }

            if (!Files.exists(path)) {
                return error("Path does not exist: " + pathStr, null);
            }

            if (!Files.isDirectory(path)) {
                return error("Path is not a directory: " + pathStr, null);
            }

            // Limit depth to prevent infinite recursion
            maxDepth = Math.min(maxDepth, MAX_DEPTH);

            List<Map<String, Object>> fileTree = buildFileTree(path, maxDepth, includeHidden);

            var result = success("File tree generated successfully");
            result.put("files", fileTree);
            result.put("totalFiles", fileTree.size());
            result.put("rootPath", pathStr);

            log.info("📂 Listed {} files from {}", fileTree.size(), pathStr);

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
                fileInfo.put("path", dir.toString());
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
                fileInfo.put("path", file.toString());
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

    private Map<String, Object> success(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        return result;
    }

    private Map<String, Object> error(String message, Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", message);
        if (e != null) {
            result.put("exception", e.getClass().getSimpleName());
        }
        return result;
    }
}
