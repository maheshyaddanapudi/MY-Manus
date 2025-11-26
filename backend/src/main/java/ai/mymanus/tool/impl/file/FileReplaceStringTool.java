package ai.mymanus.tool.impl.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * Tool for finding and replacing text in files.
 *
 * Manus AI Equivalent: file_replace_string(path: str, old: str, new: str) -> dict
 *
 * Features:
 * - Find and replace text in file
 * - Returns count of replacements made
 * - Supports multi-line replacements
 * - Preserves file if no matches found
 * - Security: Sandboxed to workspace directory
 */
@Slf4j
@Component
public class FileReplaceStringTool extends FileTool {

    @Override
    public String getName() {
        return "file_replace_string";
    }

    @Override
    public String getDescription() {
        return "Find and replace text in a file. Returns the number of replacements made. " +
               "The original file is only modified if replacements are found.";
    }

    @Override
    public String getPythonSignature() {
        return "file_replace_string(path: str, old: str, new: str) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String filePath = (String) parameters.get("path");
        String oldText = (String) parameters.get("old");
        String newText = (String) parameters.get("new");

        if (oldText == null || oldText.isEmpty()) {
            return error("Parameter 'old' cannot be empty", null);
        }

        if (newText == null) {
            newText = ""; // Allow replacing with empty string (deletion)
        }

        log.info("🔄 Replacing in file: {} (old: '{}...' -> new: '{}...')",
                filePath,
                oldText.substring(0, Math.min(oldText.length(), 20)),
                newText.substring(0, Math.min(newText.length(), 20)));

        try {
            // Validate and resolve path with security checks
            Path resolvedPath = validateAndResolvePath(filePath);

            // Check if file exists
            if (!Files.exists(resolvedPath)) {
                log.warn("⚠️ File not found: {}", resolvedPath);
                return error("File not found: " + filePath, null);
            }

            // Check if path is a directory
            if (Files.isDirectory(resolvedPath)) {
                log.warn("⚠️ Cannot operate on directory: {}", resolvedPath);
                return error("Path is a directory, not a file: " + filePath, null);
            }

            // Read file content
            String originalContent = Files.readString(resolvedPath, StandardCharsets.UTF_8);

            // Count occurrences before replacement
            int count = countOccurrences(originalContent, oldText);

            if (count == 0) {
                log.info("ℹ️ No occurrences found of text to replace");
                var result = success("No replacements made (text not found)");
                result.put("path", filePath);
                result.put("replacements", 0);
                result.put("modified", false);
                return result;
            }

            // Perform replacement
            String newContent = originalContent.replace(oldText, newText);

            // Write back to file
            Files.writeString(
                    resolvedPath,
                    newContent,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            log.info("✅ Successfully replaced {} occurrence(s) in file: {}", count, filePath);

            // Build result
            var result = success("Text replaced successfully");
            result.put("path", filePath);
            result.put("replacements", count);
            result.put("modified", true);
            result.put("oldLength", originalContent.length());
            result.put("newLength", newContent.length());
            result.put("sizeDelta", newContent.length() - originalContent.length());
            addFileMetadata(result, resolvedPath);

            return result;

        } catch (SecurityException e) {
            log.error("🚨 Security violation replacing in file: {}", filePath, e);
            return error("Security violation: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Error replacing in file: {}", filePath, e);
            return error("Failed to replace text: " + e.getMessage(), e);
        }
    }

    /**
     * Count occurrences of substring in text
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
