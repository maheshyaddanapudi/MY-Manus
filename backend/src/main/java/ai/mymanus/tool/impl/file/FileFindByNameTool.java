package ai.mymanus.tool.impl.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Tool for finding files by name within workspace.
 *
 * Manus AI Equivalent: file_find_by_name(pattern: str, path: str = ".") -> list[str]
 *
 * Features:
 * - Recursive search through directories
 * - Supports glob patterns (*.txt) and regex
 * - Returns relative file paths
 * - Limit results to prevent overwhelming output
 * - Security: Sandboxed to workspace directory
 */
@Slf4j
@Component
public class FileFindByNameTool extends FileTool {

    public FileFindByNameTool(@Value("${sandbox.host.workspace-dir}") String workspaceDir) {
        super(workspaceDir);
    }

    private static final int MAX_RESULTS = 100;

    @Override
    public String getName() {
        return "file_find_by_name";
    }

    @Override
    public String getDescription() {
        return "Find files by name pattern within the workspace. " +
               "Supports glob patterns (*.txt, test*.py) and regex. " +
               "Returns list of relative file paths. Default path is '.' (current workspace).";
    }

    @Override
    public String getPythonSignature() {
        return "file_find_by_name(pattern: str, path: str = '.', regex: bool = False) -> list[str]";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = (String) parameters.get("sessionId");
        String pattern = (String) parameters.get("pattern");
        String searchPath = (String) parameters.getOrDefault("path", ".");
        Boolean useRegex = (Boolean) parameters.getOrDefault("regex", false);

        if (pattern == null || pattern.isEmpty()) {
            return error("Search pattern cannot be empty", null);
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return error("sessionId parameter required", null);
        }


        log.info("🔍 Searching for files: '{}' in path: {} (regex: {})",
                pattern, searchPath, useRegex);

        try {
            // Validate and resolve path with security checks
            Path resolvedPath = validateAndResolvePath(sessionId, searchPath);

            // Check if path exists
            if (!Files.exists(resolvedPath)) {
                log.warn("⚠️ Path not found: {}", resolvedPath);
                return error("Path not found: " + searchPath, null);
            }

            // Check if it's a directory
            if (!Files.isDirectory(resolvedPath)) {
                log.warn("⚠️ Path is not a directory: {}", resolvedPath);
                return error("Path must be a directory: " + searchPath, null);
            }

            List<String> matches = new ArrayList<>();

            // Compile pattern
            Pattern searchPattern = useRegex
                    ? Pattern.compile(pattern)
                    : globToRegex(pattern);

            // Search files recursively
            try (Stream<Path> paths = Files.walk(resolvedPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(file -> {
                            String fileName = file.getFileName().toString();
                            return searchPattern.matcher(fileName).matches();
                        })
                        .limit(MAX_RESULTS)
                        .forEach(file -> matches.add(getRelativePath(file)));
            }

            log.info("✅ Found {} file(s) matching pattern '{}'", matches.size(), pattern);

            // Build result
            var result = success("File search completed");
            result.put("pattern", pattern);
            result.put("path", searchPath);
            result.put("regex", useRegex);
            result.put("matchCount", matches.size());
            result.put("files", matches);
            result.put("truncated", matches.size() >= MAX_RESULTS);

            return result;

        } catch (SecurityException e) {
            log.error("🚨 Security violation searching files: {}", searchPath, e);
            return error("Security violation: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Error searching files: {}", searchPath, e);
            return error("Failed to search files: " + e.getMessage(), e);
        }
    }

    /**
     * Convert glob pattern to regex
     * Examples:
     *   *.txt -> ^.*\.txt$
     *   test*.py -> ^test.*\.py$
     *   file?.md -> ^file.\.md$
     */
    private Pattern globToRegex(String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                    regex.append("\\.");
                    break;
                case '\\':
                    regex.append("\\\\");
                    break;
                default:
                    if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                        regex.append(c);
                    } else {
                        regex.append("\\").append(c);
                    }
            }
        }
        regex.append("$");
        return Pattern.compile(regex.toString());
    }

    /**
     * Get relative path from workspace root
     */
    private String getRelativePath(Path file) {
        try {
            Path workspaceRoot = Paths.get(workspaceDir).toRealPath();
            return workspaceRoot.relativize(file).toString();
        } catch (IOException e) {
            return file.toString();
        }
    }
}
