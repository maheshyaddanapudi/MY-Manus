package ai.mymanus.controller;

import ai.mymanus.service.SessionReplayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for session replay functionality
 */
@RestController
@RequestMapping("/api/replay")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SessionReplayController {

    private final SessionReplayService replayService;

    /**
     * Get state at specific event
     */
    @GetMapping("/{sessionId}/event/{eventId}")
    public ResponseEntity<Map<String, Object>> getStateAtEvent(
            @PathVariable String sessionId,
            @PathVariable java.util.UUID eventId) {

        try {
            Map<String, Object> snapshot = replayService.getStateAtEvent(sessionId, eventId);
            return ResponseEntity.ok(snapshot);
        } catch (Exception e) {
            log.error("Error getting state at event", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get state at specific iteration
     */
    @GetMapping("/{sessionId}/iteration/{iteration}")
    public ResponseEntity<Map<String, Object>> getStateAtIteration(
            @PathVariable String sessionId,
            @PathVariable int iteration) {

        try {
            Map<String, Object> snapshot = replayService.getStateAtIteration(sessionId, iteration);
            return ResponseEntity.ok(snapshot);
        } catch (Exception e) {
            log.error("Error getting state at iteration", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get all replay points for session
     */
    @GetMapping("/{sessionId}/points")
    public ResponseEntity<List<Map<String, Object>>> getReplayPoints(@PathVariable String sessionId) {
        try {
            List<Map<String, Object>> points = replayService.getReplayPoints(sessionId);
            return ResponseEntity.ok(points);
        } catch (Exception e) {
            log.error("Error getting replay points", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Step forward one event
     */
    @GetMapping("/{sessionId}/step-forward/{currentEventId}")
    public ResponseEntity<Map<String, Object>> stepForward(
            @PathVariable String sessionId,
            @PathVariable java.util.UUID currentEventId) {

        try {
            Map<String, Object> snapshot = replayService.stepForward(sessionId, currentEventId);
            return ResponseEntity.ok(snapshot);
        } catch (Exception e) {
            log.error("Error stepping forward", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Step backward one event
     */
    @GetMapping("/{sessionId}/step-backward/{currentEventId}")
    public ResponseEntity<Map<String, Object>> stepBackward(
            @PathVariable String sessionId,
            @PathVariable java.util.UUID currentEventId) {

        try {
            Map<String, Object> snapshot = replayService.stepBackward(sessionId, currentEventId);
            return ResponseEntity.ok(snapshot);
        } catch (Exception e) {
            log.error("Error stepping backward", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
