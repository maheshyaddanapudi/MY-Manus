package ai.mymanus.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File watcher for todo.md files
 * Monitors changes and pushes updates via WebSocket
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TodoMdWatcher {

    private final TodoMdParser todoMdParser;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;
    private final Map<String, WatchService> sessionWatchers = new ConcurrentHashMap<>();
    private final Map<String, Boolean> watcherRunning = new ConcurrentHashMap<>();

    private Counter planUpdatesCounter;

    @jakarta.annotation.PostConstruct
    public void initMetrics() {
        planUpdatesCounter = Counter.builder("plan.updates")
            .description("Number of plan updates detected")
            .register(meterRegistry);
    }

    /**
     * Start watching todo.md for a session
     */
    public void startWatching(String sessionId) {
        // Prevent duplicate watchers
        if (watcherRunning.getOrDefault(sessionId, false)) {
            log.debug("Watcher already running for session: {}", sessionId);
            return;
        }

        Path todoPath = Paths.get("/workspace", sessionId, "todo.md");

        if (!Files.exists(todoPath)) {
            log.debug("No todo.md yet for session: {}, will watch parent directory", sessionId);
            todoPath = Paths.get("/workspace", sessionId);

            // Create directory if it doesn't exist
            try {
                Files.createDirectories(todoPath);
            } catch (IOException e) {
                log.error("Failed to create workspace directory for session: {}", sessionId, e);
                return;
            }
        }

        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path dir = Files.isDirectory(todoPath) ? todoPath : todoPath.getParent();

            dir.register(watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE);

            sessionWatchers.put(sessionId, watchService);
            watcherRunning.put(sessionId, true);

            // Start watching in background thread
            CompletableFuture.runAsync(() -> watchFile(sessionId, watchService, dir));

            // Send initial state if file exists
            Path actualTodoPath = dir.resolve("todo.md");
            if (Files.exists(actualTodoPath)) {
                sendUpdate(sessionId, actualTodoPath);
            }

            log.info("Started watching todo.md for session: {}", sessionId);

        } catch (IOException e) {
            log.error("Failed to start watching todo.md for session: {}", sessionId, e);
        }
    }

    /**
     * Watch file changes
     */
    private void watchFile(String sessionId, WatchService watchService, Path dir) {
        try {
            WatchKey key;
            while (watcherRunning.getOrDefault(sessionId, false) && (key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changed = (Path) event.context();
                    if (changed.toString().equals("todo.md")) {
                        log.debug("todo.md changed for session: {}", sessionId);

                        // Small delay to allow file write to complete
                        Thread.sleep(100);

                        Path todoPath = dir.resolve("todo.md");
                        if (Files.exists(todoPath)) {
                            sendUpdate(sessionId, todoPath);
                            planUpdatesCounter.increment();
                        }
                    }
                }
                boolean valid = key.reset();
                if (!valid) {
                    log.warn("Watch key no longer valid for session: {}", sessionId);
                    break;
                }
            }
        } catch (InterruptedException e) {
            log.info("Watch service interrupted for session: {}", sessionId);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error in watch service for session: {}", sessionId, e);
        } finally {
            watcherRunning.put(sessionId, false);
        }
    }

    /**
     * Send plan update via WebSocket
     */
    private void sendUpdate(String sessionId, Path todoPath) {
        try {
            // Read and parse file
            String content = Files.readString(todoPath);
            TodoMdStructure structure = todoMdParser.parse(content);

            // Send to frontend via WebSocket
            messagingTemplate.convertAndSend(
                "/topic/plan/" + sessionId,
                structure
            );

            log.debug("Sent todo.md update for session: {} - {} tasks", sessionId, structure.getTasks().size());

        } catch (IOException e) {
            log.error("Failed to read todo.md for session: {}", sessionId, e);
        }
    }

    /**
     * Stop watching when session ends
     */
    public void stopWatching(String sessionId) {
        watcherRunning.put(sessionId, false);

        WatchService watchService = sessionWatchers.remove(sessionId);
        if (watchService != null) {
            try {
                watchService.close();
                log.info("Stopped watching todo.md for session: {}", sessionId);
            } catch (IOException e) {
                log.error("Failed to close watch service for session: {}", sessionId, e);
            }
        }
    }

    /**
     * Get current todo.md structure
     */
    public Optional<TodoMdStructure> getCurrentTodo(String sessionId) {
        Path todoPath = Paths.get("/workspace", sessionId, "todo.md");

        if (!Files.exists(todoPath)) {
            log.debug("No todo.md found for session: {}", sessionId);
            return Optional.empty();
        }

        try {
            String content = Files.readString(todoPath);
            return Optional.of(todoMdParser.parse(content));
        } catch (IOException e) {
            log.error("Failed to read todo.md for session: {}", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * Check if watcher is running
     */
    public boolean isWatching(String sessionId) {
        return watcherRunning.getOrDefault(sessionId, false);
    }
}
