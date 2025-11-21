package ai.mymanus.controller;

import ai.mymanus.dto.ChatRequest;
import ai.mymanus.dto.ChatResponse;
import ai.mymanus.model.Message;
import ai.mymanus.model.ToolExecution;
import ai.mymanus.service.AgentStateService;
import ai.mymanus.service.CodeActAgentService;
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
                    Create a new agent session with an optional custom session ID.
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
            String sessionId) {

        String id = (sessionId != null && !sessionId.isEmpty())
                ? sessionId
                : UUID.randomUUID().toString();

        stateService.createSession(id);

        Map<String, String> response = new HashMap<>();
        response.put("sessionId", id);
        response.put("message", "Session created successfully");

        log.info("Created new session: {}", id);
        return ResponseEntity.ok(response);
    }
}
