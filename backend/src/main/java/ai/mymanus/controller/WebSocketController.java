package ai.mymanus.controller;

import ai.mymanus.dto.AgentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for real-time agent updates.
 * Clients subscribe to /topic/agent/{sessionId} to receive events.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle subscribe requests
     */
    @MessageMapping("/agent/subscribe/{sessionId}")
    @SendTo("/topic/agent/{sessionId}")
    public AgentEvent subscribe(@DestinationVariable String sessionId) {
        log.info("Client subscribed to session: {}", sessionId);
        return AgentEvent.builder()
                .type("connected")
                .content("Successfully connected to session")
                .build();
    }

    /**
     * Send event to specific session
     */
    public void sendToSession(String sessionId, AgentEvent event) {
        messagingTemplate.convertAndSend("/topic/agent/" + sessionId, event);
    }
}
