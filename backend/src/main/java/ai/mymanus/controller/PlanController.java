package ai.mymanus.controller;

import ai.mymanus.service.TodoMdStructure;
import ai.mymanus.service.TodoMdWatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for plan/todo.md management
 */
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PlanController {

    private final TodoMdWatcher todoMdWatcher;

    /**
     * Get current plan for session
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<TodoMdStructure> getCurrentPlan(@PathVariable String sessionId) {
        return todoMdWatcher.getCurrentTodo(sessionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Start watching todo.md for session
     */
    @PostMapping("/{sessionId}/watch")
    public ResponseEntity<Map<String, Object>> startWatching(@PathVariable String sessionId) {
        todoMdWatcher.startWatching(sessionId);
        return ResponseEntity.ok(Map.of(
            "status", "watching",
            "sessionId", sessionId
        ));
    }

    /**
     * Stop watching todo.md for session
     */
    @PostMapping("/{sessionId}/stop")
    public ResponseEntity<Map<String, Object>> stopWatching(@PathVariable String sessionId) {
        todoMdWatcher.stopWatching(sessionId);
        return ResponseEntity.ok(Map.of(
            "status", "stopped",
            "sessionId", sessionId
        ));
    }

    /**
     * Check if watching
     */
    @GetMapping("/{sessionId}/status")
    public ResponseEntity<Map<String, Object>> getWatchStatus(@PathVariable String sessionId) {
        boolean watching = todoMdWatcher.isWatching(sessionId);
        return ResponseEntity.ok(Map.of(
            "watching", watching,
            "sessionId", sessionId
        ));
    }
}
