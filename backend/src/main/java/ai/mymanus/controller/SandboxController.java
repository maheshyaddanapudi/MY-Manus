package ai.mymanus.controller;

import ai.mymanus.service.sandbox.PythonSandboxExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for sandbox container monitoring and management.
 * Provides endpoints to view and manage active sandbox containers.
 */
@Slf4j
@RestController
@RequestMapping("/api/sandbox")
@Tag(name = "Sandbox", description = "Sandbox container monitoring and management")
@RequiredArgsConstructor
public class SandboxController {

    private final PythonSandboxExecutor sandboxExecutor;

    @GetMapping("/stats")
    @Operation(
            summary = "Get sandbox statistics",
            description = """
                    Retrieve statistics about active sandbox containers.

                    **Returns:**
                    - Total number of active containers
                    - List of session IDs with containers
                    - Container details (ID, running status)

                    Useful for monitoring resource usage and debugging.
                    """
    )
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = sandboxExecutor.getContainerStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting sandbox stats", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/cleanup/{sessionId}")
    @Operation(
            summary = "Cleanup container for session",
            description = """
                    Force cleanup of sandbox container for a specific session.

                    **Use Cases:**
                    - Forcefully terminate a stuck container
                    - Clean up resources when session is abandoned
                    - Manually trigger container recreation

                    ⚠️ This will terminate any running code execution!
                    """
    )
    public ResponseEntity<Map<String, String>> cleanupContainer(
            @PathVariable
            @Parameter(description = "Session ID to cleanup")
            String sessionId) {

        try {
            sandboxExecutor.destroySessionContainer(sessionId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Container destroyed successfully");
            response.put("sessionId", sessionId);

            log.info("Manually cleaned up container for session {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error cleaning up container for session {}", sessionId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("sessionId", sessionId);
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/cleanup/all")
    @Operation(
            summary = "Cleanup all containers",
            description = """
                    Force cleanup of ALL sandbox containers.

                    **Warning:** This will:
                    - Terminate all running code executions
                    - Remove all sandbox containers
                    - Free up system resources

                    ⚠️ Only use this for maintenance or emergency cleanup!
                    """
    )
    public ResponseEntity<Map<String, Object>> cleanupAllContainers() {
        try {
            Map<String, Object> statsBefore = sandboxExecutor.getContainerStats();
            int containerCount = (int) statsBefore.get("totalContainers");

            sandboxExecutor.cleanupAllContainers();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "All containers destroyed");
            response.put("containersDestroyed", containerCount);

            log.warn("Manually cleaned up all {} containers", containerCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error cleaning up all containers", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
