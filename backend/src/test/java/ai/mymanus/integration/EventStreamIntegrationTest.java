package ai.mymanus.integration;

import ai.mymanus.MyManusApplication;
import ai.mymanus.config.IntegrationTestConfiguration;
import ai.mymanus.model.Event;
import ai.mymanus.service.AgentStateService;
import ai.mymanus.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for event persistence and retrieval
 */
@SpringBootTest(classes = MyManusApplication.class)
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class EventStreamIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private AgentStateService agentStateService;

    @Test
    @Transactional
    void testEventStreamPersistence() {
        String sessionId = "event-test-" + System.currentTimeMillis();

        // Create session first
        agentStateService.getOrCreateSession(sessionId);

        // Create events
        Event userMsg = eventService.appendUserMessage(sessionId, "Test message", 1);
        assertNotNull(userMsg);

        // Retrieve events
        List<Event> events = eventService.getEventStream(sessionId);
        assertFalse(events.isEmpty());
        assertEquals(sessionId, events.get(0).getAgentState().getSessionId());
    }

    @Test
    @Transactional
    void testEventOrdering() {
        String sessionId = "ordering-test-" + System.currentTimeMillis();

        // Create session first
        agentStateService.getOrCreateSession(sessionId);

        eventService.appendUserMessage(sessionId, "Message 1", 1);
        eventService.appendUserMessage(sessionId, "Message 2", 1);
        eventService.appendUserMessage(sessionId, "Message 3", 1);

        List<Event> events = eventService.getEventStream(sessionId);
        assertEquals(3, events.size());
        // Events should be ordered by sequence within the same iteration
        assertTrue(events.get(0).getSequence() <= events.get(1).getSequence());
        assertTrue(events.get(1).getSequence() <= events.get(2).getSequence());
    }
}
