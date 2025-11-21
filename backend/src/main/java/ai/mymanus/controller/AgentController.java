package ai.mymanus.controller;

import ai.mymanus.dto.ChatRequest;
import ai.mymanus.dto.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "Agent", description = "AI Agent interaction endpoints")
public class AgentController {

    @PostMapping("/chat")
    @Operation(
            summary = "Send message to agent",
            description = "Send a message to the AI agent and receive a response",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful response",
                            content = @Content(schema = @Schema(implementation = ChatResponse.class))
                    )
            }
    )
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody
            @Parameter(description = "Chat request with session ID and message")
            ChatRequest request) {

        // Placeholder implementation
        ChatResponse response = ChatResponse.builder()
                .sessionId(request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString())
                .message("Echo: " + request.getMessage())
                .status("pending")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/session/{sessionId}")
    @Operation(
            summary = "Get session status",
            description = "Retrieve the current status of an agent session"
    )
    public ResponseEntity<Map<String, Object>> getSessionStatus(
            @PathVariable
            @Parameter(description = "Session ID")
            String sessionId) {

        Map<String, Object> status = new HashMap<>();
        status.put("sessionId", sessionId);
        status.put("status", "active");
        status.put("messageCount", 0);

        return ResponseEntity.ok(status);
    }
}
