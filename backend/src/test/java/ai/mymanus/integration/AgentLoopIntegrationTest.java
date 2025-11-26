package ai.mymanus.integration;

import ai.mymanus.service.CodeActAgentService;
import ai.mymanus.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for end-to-end agent execution loop
 */
@SpringBootTest
@ActiveProfiles("test")
class AgentLoopIntegrationTest {

    @Autowired
    private CodeActAgentService codeActAgentService;

    @Autowired
    private EventService eventService;

    @Test
    void testCompleteAgentLoop() {
        String sessionId = "integration-test-" + System.currentTimeMillis();
        String userMessage = "Calculate 2 + 2";

        assertDoesNotThrow(() -> {
            codeActAgentService.processUserMessage(sessionId, userMessage);
        });

        // Verify events were created
        var events = eventService.getEventStream(sessionId);
        assertNotNull(events);
        assertFalse(events.isEmpty());
    }
}
