package ai.mymanus.controller;

import ai.mymanus.tool.impl.file.FileListTool;
import ai.mymanus.tool.impl.file.FileReadTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API controller for file browsing operations.
 * Provides read-only access to workspace files for UI file explorer.
 * 
 * EXTENSIBILITY NOTE: Designed to support future file editing capabilities.
 * To add editing later:
 * 1. Add POST /write endpoint using FileWriteTool
 * 2. Add DELETE /delete endpoint
 * 3. Add POST /create endpoint
 * 4. Consider adding file locking mechanism for concurrent editing
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@Tag(name = "Files", description = "File browsing and viewing endpoints (read-only)")
@RequiredArgsConstructor
public class FileController {

    private final FileListTool fileListTool;
    private final FileReadTool fileReadTool;

    @GetMapping("/list")
    @Operation(
            summary = "List files and directories",
            description = """
                    Browse workspace files in a tree structure.
                    
                    **Security:**
                    - Restricted to /workspace directory only
                    - Cannot access files outside workspace
                    
                    **Parameters:**
                    - path: Directory to list (default: /workspace)
                    - maxDepth: How deep to recurse (default: 3, max: 10)
                    - includeHidden: Show hidden files (default: false)
                    
                    **Returns:**
                    - files: Array of file/directory objects with path, name, type, size, depth
                    - totalFiles: Total number of files/directories
                    - rootPath: The path that was listed
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "File tree retrieved successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid path or parameters"),
                    @ApiResponse(responseCode = "403", description = "Path outside workspace (security violation)")
            }
    )
    public ResponseEntity<Map<String, Object>> listFiles(
            @RequestParam(defaultValue = "/workspace")
            @Parameter(description = "Directory path to list", example = "/workspace")
            String path,
            
            @RequestParam(defaultValue = "3")
            @Parameter(description = "Maximum depth to recurse (1-10)", example = "3")
            int maxDepth,
            
            @RequestParam(defaultValue = "false")
            @Parameter(description = "Include hidden files (starting with .)", example = "false")
            boolean includeHidden) {

        try {
            Map<String, Object> params = Map.of(
                    "path", path,
                    "maxDepth", maxDepth,
                    "includeHidden", includeHidden
            );

            Map<String, Object> result = fileListTool.execute(params);

            if (!(Boolean) result.get("success")) {
                log.warn("File list failed: {}", result.get("error"));
                return ResponseEntity.badRequest().body(result);
            }

            log.info("📂 Listed {} files from {}", result.get("totalFiles"), path);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error listing files from {}", path, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Failed to list files: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/read")
    @Operation(
            summary = "Read file content",
            description = """
                    Read the content of a file from the workspace.
                    
                    **Security:**
                    - Restricted to /workspace directory only
                    - Cannot read files outside workspace
                    
                    **Returns:**
                    - content: File content as string
                    - path: The file path that was read
                    - size: File size in bytes
                    - lines: Number of lines in file
                    
                    **Note:** This is read-only. To enable editing in the future,
                    a corresponding /write endpoint will be added.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "File content retrieved successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid path or file not found"),
                    @ApiResponse(responseCode = "403", description = "Path outside workspace (security violation)")
            }
    )
    public ResponseEntity<Map<String, Object>> readFile(
            @RequestParam
            @Parameter(description = "File path to read", required = true, example = "/workspace/example.py")
            String path) {

        try {
            Map<String, Object> params = Map.of("path", path);
            Map<String, Object> result = fileReadTool.execute(params);

            if (!(Boolean) result.get("success")) {
                log.warn("File read failed: {}", result.get("error"));
                return ResponseEntity.badRequest().body(result);
            }

            log.info("📄 Read file: {} ({} bytes)", path, result.get("size"));
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error reading file {}", path, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Failed to read file: " + e.getMessage()
            ));
        }
    }

    // FUTURE EXTENSIBILITY: Uncomment and implement when adding editing capabilities
    /*
    @PostMapping("/write")
    @Operation(summary = "Write file content (FUTURE)")
    public ResponseEntity<Map<String, Object>> writeFile(
            @RequestParam String path,
            @RequestBody String content) {
        // TODO: Implement using FileWriteTool
        // TODO: Add file locking mechanism
        // TODO: Consider agent pause/resume when user edits
        return ResponseEntity.status(501).body(Map.of(
                "success", false,
                "error", "File editing not yet implemented"
        ));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete file (FUTURE)")
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestParam String path) {
        // TODO: Implement file deletion
        // TODO: Add confirmation mechanism
        return ResponseEntity.status(501).body(Map.of(
                "success", false,
                "error", "File deletion not yet implemented"
        ));
    }

    @PostMapping("/create")
    @Operation(summary = "Create new file (FUTURE)")
    public ResponseEntity<Map<String, Object>> createFile(
            @RequestParam String path,
            @RequestParam(defaultValue = "file") String type) {
        // TODO: Implement file/directory creation
        return ResponseEntity.status(501).body(Map.of(
                "success", false,
                "error", "File creation not yet implemented"
        ));
    }
    */
}
