package ai.mymanus.controller;

import ai.mymanus.dto.ChatRequest;
import ai.mymanus.dto.ChatResponse;
import ai.mymanus.model.Event;
import ai.mymanus.model.Message;
import ai.mymanus.model.ToolExecution;
import ai.mymanus.service.AgentStateService;
import ai.mymanus.service.CodeActAgentService;
import ai.mymanus.service.EventService;
import org.springframework.beans.factory.annotation.Value;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for agent interactions.
 * Provides comprehensive endpoints for chat, session management, and monitoring.
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@Tag(name = "Agent", description = "AI Agent interaction and management endpoints")
@RequiredArgsConstructor
public class AgentController {

    private final CodeActAgentService agentService;
    private final AgentStateService stateService;
    private final EventService eventService;

    @Value("${workspace.dir:/workspace}")
    private String workspaceDir;

    @PostMapping("/session/{sessionId}/stop")
    @Operation(
            summary = "Stop agent execution",
            description = """
                    Request the agent to stop processing for a specific session.
                    The agent will stop at the next iteration boundary.

                    **Note:** This is a graceful stop - the current iteration may complete
                    before the agent stops.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Stop requested successfully"),
                    @ApiResponse(responseCode = "404", description = "Session not found or not running")
            }
    )
    public ResponseEntity<Map<String, Object>> stopAgent(
            @PathVariable
            @Parameter(description = "Session ID to stop", required = true)
            String sessionId) {

        log.info("Stop request received for session: {}", sessionId);

        boolean stopped = agentService.stopAgent(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);

        if (stopped) {
            response.put("status", "stopping");
            response.put("message", "Stop request sent. Agent will stop at next iteration.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "not_running");
            response.put("message", "Session is not currently running");
            return ResponseEntity.status(404).body(response);
        }
    }

    @GetMapping("/session/{sessionId}/running")
    @Operation(
            summary = "Check if agent is running",
            description = "Check if the agent is currently processing a request for this session.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status retrieved successfully")
            }
    )
    public ResponseEntity<Map<String, Object>> isAgentRunning(
            @PathVariable
            @Parameter(description = "Session ID to check", required = true)
            String sessionId) {

        boolean running = agentService.isRunning(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("running", running);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat")
    @Operation(
            summary = "Send message to agent",
            description = """
                    Send a message to the AI agent and receive a response.
                    The agent uses CodeAct architecture - it writes and executes Python code to solve tasks.
                    Real-time updates are sent via WebSocket to /topic/agent/{sessionId}.

                    **Features:**
                    - Automatic code generation and execution
                    - State persistence between messages
                    - Tool usage (web search, file operations, etc.)
                    - Up to 20 iterations per task
                    - Sandbox isolation for security
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Request accepted, agent processing started",
                            content = @Content(schema = @Schema(implementation = ChatResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                    @ApiResponse(responseCode = "500", description = "Internal server error during processing")
            }
    )
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody
            @Parameter(description = "Chat request containing session ID and user message", required = true)
            ChatRequest request) {

        log.info("Received chat request: sessionId={}, message length={}",
                request.getSessionId(), request.getMessage().length());

        String sessionId = (request.getSessionId() != null && !request.getSessionId().isEmpty())
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        try {
            String response = agentService.processQuery(sessionId, request.getMessage());

            ChatResponse chatResponse = ChatResponse.builder()
                    .sessionId(sessionId)
                    .message(response)
                    .status("completed")
                    .build();

            log.info("Chat completed successfully for session: {}", sessionId);
            return ResponseEntity.ok(chatResponse);

        } catch (Exception e) {
            log.error("Error processing chat for session: {}", sessionId, e);
            ChatResponse errorResponse = ChatResponse.builder()
                    .sessionId(sessionId)
                    .message("Error: " + e.getMessage())
                    .status("error")
                    .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/session/{sessionId}")
    @Operation(
            summary = "Get session status",
            description = """
                    Retrieve comprehensive status information for an agent session.

                    **Returns:**
                    - Session existence status
                    - Message count
                    - Execution context (Python variables)
                    - Custom metadata
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Session status retrieved"),
                    @ApiResponse(responseCode = "404", description = "Session not found")
            }
    )
    public ResponseEntity<Map<String, Object>> getSessionStatus(
            @PathVariable
            @Parameter(description = "Session ID to retrieve status for", required = true)
            String sessionId) {

        try {
            Map<String, Object> status = agentService.getSessionStatus(sessionId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.warn("Session not found: {}", sessionId);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Session not found");
            error.put("sessionId", sessionId);
            return ResponseEntity.status(404).body(error);
        }
    }

    @GetMapping("/session/{sessionId}/messages")
    @Operation(
            summary = "Get conversation history",
            description = """
                    Retrieve all messages in a session's conversation history in chronological order.
                    Includes user messages, assistant responses, and system messages.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Session not found")
            }
    )
    public ResponseEntity<List<Message>> getMessages(
            @PathVariable
            @Parameter(description = "Session ID", required = true)
            String sessionId) {

        try {
            List<Message> messages = stateService.getMessages(sessionId);
            log.info("Retrieved {} messages for session: {}", messages.size(), sessionId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.warn("Failed to retrieve messages for session: {}", sessionId, e);
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping("/session/{sessionId}/context")
    @Operation(
            summary = "Get execution context",
            description = """
                    Retrieve the current Python execution context (variables and their values) for a session.
                    Variables persist between code executions within the same session.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Context retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Session not found")
            }
    )
    public ResponseEntity<Map<String, Object>> getExecutionContext(
            @PathVariable
            @Parameter(description = "Session ID", required = true)
            String sessionId) {

        try {
            Map<String, Object> context = stateService.getExecutionContext(sessionId);
            return ResponseEntity.ok(context);
        } catch (Exception e) {
            log.warn("Failed to retrieve context for session: {}", sessionId, e);
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping("/session/{sessionId}/tools")
    @Operation(
            summary = "Get tool execution history",
            description = """
                    Retrieve all tool executions for a session with their parameters, results, and status.
                    Useful for debugging and understanding what tools the agent used.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tool executions retrieved"),
                    @ApiResponse(responseCode = "404", description = "Session not found")
            }
    )
    public ResponseEntity<List<ToolExecution>> getToolExecutions(
            @PathVariable
            @Parameter(description = "Session ID", required = true)
            String sessionId) {

        try {
            List<ToolExecution> executions = stateService.getToolExecutions(sessionId);
            return ResponseEntity.ok(executions);
        } catch (Exception e) {
            log.warn("Failed to retrieve tool executions for session: {}", sessionId, e);
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping("/session/{sessionId}/workspace")
    @Operation(
            summary = "Get workspace path",
            description = """  
                    Get the workspace directory path for a specific session.
                    
                    Returns the absolute path to the session's workspace directory
                    where all files, code, and outputs are stored.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workspace path retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Session not found")
            }
    )
    public ResponseEntity<Map<String, String>> getWorkspacePath(
            @PathVariable
            @Parameter(description = "Session ID", required = true)
            String sessionId) {

        try {
            // Verify session exists
            stateService.getSession(sessionId);
            
            String workspacePath = workspaceDir + "/" + sessionId;
            Map<String, String> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("workspacePath", workspacePath);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Failed to retrieve workspace path for session: {}", sessionId, e);
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping("/session/{sessionId}/events")
    @Operation(
            summary = "Get event stream",
            description = """
                    Retrieve the complete event stream for a session in chronological order.

                    **Event Stream Architecture:**
                    The event stream captures the entire agent execution flow following Manus AI's pattern:
                    [UserMessage → AgentThought → AgentAction → Observation → ...]

                    **Event Types:**
                    - USER_MESSAGE: User sends a query
                    - AGENT_THOUGHT: Agent's reasoning/planning
                    - AGENT_ACTION: Tool/code execution initiated
                    - OBSERVATION: Result from action execution
                    - AGENT_RESPONSE: Agent's final response
                    - SYSTEM: System messages
                    - ERROR: Error events

                    **Use Cases:**
                    - Session replay and debugging
                    - Understanding agent decision-making
                    - Visualizing execution timeline
                    - Performance analysis
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Event stream retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Session not found")
            }
    )
    public ResponseEntity<List<Event>> getEventStream(
            @PathVariable
            @Parameter(description = "Session ID", required = true)
            String sessionId) {

        try {
            List<Event> events = eventService.getEventStream(sessionId);
            log.info("Retrieved {} events for session: {}", events.size(), sessionId);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.warn("Failed to retrieve event stream for session: {}", sessionId, e);
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping("/session/{sessionId}/events/iteration/{iteration}")
    @Operation(
            summary = "Get events for specific iteration",
            description = """
                    Retrieve all events for a specific iteration within a session.

                    **Iteration Concept:**
                    Each iteration represents one complete cycle of:
                    1. Agent thinks (AGENT_THOUGHT)
                    2. Agent acts (AGENT_ACTION) - ONE action only
                    3. System observes (OBSERVATION)

                    This follows Manus AI's ONE action per iteration pattern.

                    **Use Cases:**
                    - Debugging specific iteration failures
                    - Understanding action-observation pairs
                    - Performance analysis per iteration
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Events retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Session not found")
            }
    )
    public ResponseEntity<List<Event>> getEventsForIteration(
            @PathVariable
            @Parameter(description = "Session ID", required = true)
            String sessionId,
            @PathVariable
            @Parameter(description = "Iteration number (starts at 1)", required = true)
            int iteration) {

        try {
            List<Event> events = eventService.getEventsForIteration(sessionId, iteration);
            log.info("Retrieved {} events for session: {}, iteration: {}",
                    events.size(), sessionId, iteration);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.warn("Failed to retrieve events for session: {}, iteration: {}",
                    sessionId, iteration, e);
            return ResponseEntity.status(404).build();
        }
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(
            summary = "Clear session",
            description = """
                    Delete a session and all its associated data including:
                    - Conversation messages
                    - Execution context (Python variables)
                    - Tool execution history
                    - Custom metadata

                    This action cannot be undone.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Session cleared successfully"),
                    @ApiResponse(responseCode = "404", description = "Session not found")
            }
    )
    public ResponseEntity<Map<String, String>> clearSession(
            @PathVariable
            @Parameter(description = "Session ID to clear", required = true)
            String sessionId) {

        try {
            agentService.clearSession(sessionId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Session cleared successfully");
            response.put("sessionId", sessionId);
            log.info("Cleared session: {}", sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to clear session: {}", sessionId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    @PostMapping("/session")
    @Operation(
            summary = "Create new session",
            description = """
                    Create a new agent session with an optional custom session ID and title.
                    If no session ID is provided, a random UUID will be generated.

                    Sessions store:
                    - Conversation history (via Spring AI chat memory)
                    - Python execution context
                    - Tool usage history
                    - Custom metadata
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Session created successfully")
            }
    )
    public ResponseEntity<Map<String, String>> createSession(
            @RequestParam(required = false)
            @Parameter(description = "Optional custom session ID. If not provided, a UUID will be generated.")
            String sessionId,
            @RequestParam(required = false)
            @Parameter(description = "Optional session title. Defaults to 'New Conversation'.")
            String title) {

        String id = (sessionId != null && !sessionId.isEmpty())
                ? sessionId
                : UUID.randomUUID().toString();

        stateService.createSession(id, title);

        Map<String, String> response = new HashMap<>();
        response.put("sessionId", id);
        response.put("message", "Session created successfully");

        log.info("Created new session: {} with title: {}", id, title);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    @Operation(
            summary = "List all sessions",
            description = """
                    Retrieve all agent sessions ordered by most recently updated.

                    **Returns:**
                    - List of all sessions with:
                      - sessionId
                      - title
                      - createdAt
                      - updatedAt
                      - message count

                    Useful for implementing a conversation list UI similar to ChatGPT/Claude.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sessions retrieved successfully")
            }
    )
    public ResponseEntity<List<Map<String, Object>>> listSessions() {
        try {
            var sessions = stateService.listAllSessions();

            List<Map<String, Object>> sessionList = sessions.stream()
                    .map(session -> {
                        Map<String, Object> sessionData = new HashMap<>();
                        sessionData.put("sessionId", session.getSessionId());
                        sessionData.put("title", session.getTitle());
                        sessionData.put("createdAt", session.getCreatedAt());
                        sessionData.put("updatedAt", session.getUpdatedAt());

                        // Get message count
                        try {
                            int messageCount = stateService.getMessages(session.getSessionId()).size();
                            sessionData.put("messageCount", messageCount);
                        } catch (Exception e) {
                            sessionData.put("messageCount", 0);
                        }

                        return sessionData;
                    })
                    .toList();

            log.info("Retrieved {} sessions", sessionList.size());
            return ResponseEntity.ok(sessionList);

        } catch (Exception e) {
            log.error("Error listing sessions", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/session/{sessionId}/title")
    @Operation(
            summary = "Update session title",
            description = """
                    Update the title of an existing session.

                    **Use Cases:**
                    - User manually renames conversation
                    - Auto-generated title needs correction
                    - Organizing conversations
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Title updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Session not found")
            }
    )
    public ResponseEntity<Map<String, String>> updateSessionTitle(
            @PathVariable
            @Parameter(description = "Session ID to update", required = true)
            String sessionId,
            @RequestParam
            @Parameter(description = "New title for the session", required = true)
            String title) {

        try {
            stateService.updateSessionTitle(sessionId, title);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Title updated successfully");
            response.put("sessionId", sessionId);
            response.put("title", title);

            log.info("Updated title for session {}: {}", sessionId, title);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update title for session: {}", sessionId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }
}
