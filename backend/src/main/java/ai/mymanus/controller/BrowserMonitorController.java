package ai.mymanus.controller;

import ai.mymanus.model.ConsoleLog;
import ai.mymanus.model.NetworkRequest;
import ai.mymanus.repository.ConsoleLogRepository;
import ai.mymanus.repository.NetworkRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for browser monitoring (console logs and network requests)
 */
@RestController
@RequestMapping("/api/browser")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BrowserMonitorController {

    private final ConsoleLogRepository consoleLogRepository;
    private final NetworkRequestRepository networkRequestRepository;

    /**
     * Get console logs for session
     */
    @GetMapping("/{sessionId}/console-logs")
    public ResponseEntity<List<ConsoleLog>> getConsoleLogs(@PathVariable String sessionId) {
        try {
            List<ConsoleLog> logs = consoleLogRepository.findBySessionIdOrderByTimestampDesc(sessionId);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("Error getting console logs", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Clear console logs for session
     */
    @DeleteMapping("/{sessionId}/console-logs")
    public ResponseEntity<Map<String, Object>> clearConsoleLogs(@PathVariable String sessionId) {
        try {
            consoleLogRepository.deleteBySessionId(sessionId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Error clearing console logs", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get network requests for session
     */
    @GetMapping("/{sessionId}/network-requests")
    public ResponseEntity<List<NetworkRequest>> getNetworkRequests(@PathVariable String sessionId) {
        try {
            List<NetworkRequest> requests = networkRequestRepository.findBySessionIdOrderByTimestampDesc(sessionId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("Error getting network requests", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Clear network requests for session
     */
    @DeleteMapping("/{sessionId}/network-requests")
    public ResponseEntity<Map<String, Object>> clearNetworkRequests(@PathVariable String sessionId) {
        try {
            networkRequestRepository.deleteBySessionId(sessionId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Error clearing network requests", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Add console log (called by browser executor)
     */
    @PostMapping("/{sessionId}/console")
    public ResponseEntity<ConsoleLog> addConsoleLog(
            @PathVariable String sessionId,
            @RequestBody ConsoleLog consoleLog) {

        try {
            consoleLog.setSessionId(sessionId);
            ConsoleLog saved = consoleLogRepository.save(consoleLog);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error adding console log", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Add network request (called by browser executor)
     */
    @PostMapping("/{sessionId}/network-requests")
    public ResponseEntity<NetworkRequest> addNetworkRequest(
            @PathVariable String sessionId,
            @RequestBody NetworkRequest request) {

        try {
            request.setSessionId(sessionId);
            NetworkRequest saved = networkRequestRepository.save(request);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error adding network request", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
