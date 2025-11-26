package ai.mymanus.controller;

import ai.mymanus.service.sandbox.SandboxExecutor;
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
@Tag(name = "Sandbox", description = "Sandbox environment monitoring and management")
@RequiredArgsConstructor
public class SandboxController {

    private final SandboxExecutor sandboxExecutor;

    @GetMapping("/stats")
    @Operation(
            summary = "Get sandbox statistics",
            description = """
                    Retrieve statistics about active sandbox environments.

                    **Docker Mode Returns:**
                    - Total number of active containers
                    - List of session IDs with containers
                    - Container details (ID, running status)

                    **Host Mode Returns:**
                    - Python executable path
                    - Workspace directory
                    - Number of active session workspaces

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
            summary = "Cleanup sandbox for session",
            description = """
                    Force cleanup of sandbox environment for a specific session.

                    **Use Cases:**
                    - Forcefully terminate a stuck execution
                    - Clean up resources when session is abandoned
                    - Manually trigger environment recreation

                    **Docker Mode:** Stops and removes the container
                    **Host Mode:** Deletes the workspace directory

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
            summary = "Cleanup all sandbox environments",
            description = """
                    Force cleanup of ALL sandbox environments.

                    **Warning:** This will:
                    - Terminate all running code executions
                    - Remove all sandbox environments
                    - Free up system resources

                    **Docker Mode:** Stops and removes all containers
                    **Host Mode:** Deletes all workspace directories

                    ⚠️ Only use this for maintenance or emergency cleanup!
                    """
    )
    public ResponseEntity<Map<String, Object>> cleanupAllContainers() {
        try {
            Map<String, Object> statsBefore = sandboxExecutor.getContainerStats();

            // Get count based on mode (totalContainers for docker, activeSessions for host)
            int count = statsBefore.containsKey("totalContainers")
                    ? (int) statsBefore.get("totalContainers")
                    : (int) statsBefore.getOrDefault("activeSessions", 0);

            sandboxExecutor.cleanupAllContainers();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "All sandbox environments destroyed");
            response.put("environmentsDestroyed", count);

            log.warn("Manually cleaned up {} sandbox environments", count);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error cleaning up all containers", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
