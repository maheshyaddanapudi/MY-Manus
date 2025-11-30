package ai.mymanus.tool.impl.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
 * Tool for searching file contents within workspace.
 *
 * Manus AI Equivalent: file_find_content(pattern: str, path: str = ".") -> list[dict]
 *
 * Features:
 * - Recursive search through directories
 * - Supports regex patterns
 * - Returns file paths with line numbers and context
 * - Limit results to prevent overwhelming output
 * - Security: Sandboxed to workspace directory
 */
@Slf4j
@Component
public class FileFindContentTool extends FileTool {

    private static final int MAX_RESULTS = 100;
    private static final int CONTEXT_LINES = 2;

    @Override
    public String getName() {
        return "file_find_content";
    }

    @Override
    public String getDescription() {
        return "Search for text content within files in the workspace. " +
               "Supports regex patterns. Returns file paths, line numbers, and matching text. " +
               "Default path is '.' (current workspace).";
    }

    @Override
    public String getPythonSignature() {
        return "file_find_content(pattern: str, path: str = '.', regex: bool = False) -> list[dict]";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String pattern = (String) parameters.get("pattern");
        String searchPath = (String) parameters.getOrDefault("path", ".");
        Boolean useRegex = (Boolean) parameters.getOrDefault("regex", false);

        if (pattern == null || pattern.isEmpty()) {
            return error("Search pattern cannot be empty", null);
        }

        log.info("🔍 Searching for content: '{}' in path: {} (regex: {})",
                pattern, searchPath, useRegex);

        try {
            // Validate and resolve path with security checks
            Path resolvedPath = validateAndResolvePath(searchPath);

            // Check if path exists
            if (!Files.exists(resolvedPath)) {
                log.warn("⚠️ Path not found: {}", resolvedPath);
                return error("Path not found: " + searchPath, null);
            }

            List<Map<String, Object>> matches = new ArrayList<>();

            // Compile pattern if using regex
            Pattern regexPattern = useRegex ? Pattern.compile(pattern) : null;

            // Search files
            if (Files.isDirectory(resolvedPath)) {
                // Recursive search in directory
                searchDirectory(resolvedPath, pattern, regexPattern, matches);
            } else {
                // Search single file
                searchFile(resolvedPath, pattern, regexPattern, matches);
            }

            log.info("✅ Found {} match(es) for pattern '{}'", matches.size(), pattern);

            // Build result
            var result = success("Content search completed");
            result.put("pattern", pattern);
            result.put("path", searchPath);
            result.put("regex", useRegex);
            result.put("matchCount", matches.size());
            result.put("matches", matches);
            result.put("truncated", matches.size() >= MAX_RESULTS);

            return result;

        } catch (SecurityException e) {
            log.error("🚨 Security violation searching content: {}", searchPath, e);
            return error("Security violation: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Error searching content: {}", searchPath, e);
            return error("Failed to search content: " + e.getMessage(), e);
        }
    }

    /**
     * Search directory recursively
     */
    private void searchDirectory(Path dir, String pattern, Pattern regexPattern,
                                  List<Map<String, Object>> matches) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isTextFile)
                    .forEach(file -> {
                        try {
                            if (matches.size() < MAX_RESULTS) {
                                searchFile(file, pattern, regexPattern, matches);
                            }
                        } catch (IOException e) {
                            log.warn("⚠️ Could not read file: {}", file, e);
                        }
                    });
        }
    }

    /**
     * Search single file for pattern
     */
    private void searchFile(Path file, String pattern, Pattern regexPattern,
                            List<Map<String, Object>> matches) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        for (int i = 0; i < lines.size() && matches.size() < MAX_RESULTS; i++) {
            String line = lines.get(i);
            boolean found = regexPattern != null
                    ? regexPattern.matcher(line).find()
                    : line.contains(pattern);

            if (found) {
                Map<String, Object> match = new HashMap<>();
                match.put("file", getRelativePath(file));
                match.put("line", i + 1); // 1-indexed
                match.put("content", line.trim());

                // Add context (surrounding lines)
                List<String> context = new ArrayList<>();
                for (int j = Math.max(0, i - CONTEXT_LINES);
                     j < Math.min(lines.size(), i + CONTEXT_LINES + 1); j++) {
                    context.add(lines.get(j));
                }
                match.put("context", String.join("\n", context));

                matches.add(match);
            }
        }
    }

    /**
     * Check if file is likely a text file (not binary)
     */
    private boolean isTextFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".txt") || name.endsWith(".md") ||
               name.endsWith(".java") || name.endsWith(".py") ||
               name.endsWith(".js") || name.endsWith(".ts") ||
               name.endsWith(".json") || name.endsWith(".xml") ||
               name.endsWith(".yml") || name.endsWith(".yaml") ||
               name.endsWith(".properties") || name.endsWith(".sh") ||
               name.endsWith(".html") || name.endsWith(".css");
    }

    /**
     * Get relative path from workspace root
     */
    private String getRelativePath(Path file) {
        try {
            Path workspaceRoot = Paths.get(WORKSPACE_ROOT).toRealPath();
            return workspaceRoot.relativize(file).toString();
        } catch (IOException e) {
            return file.toString();
        }
    }
}
