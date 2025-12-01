package ai.mymanus.integration;

import ai.mymanus.model.Event;
import ai.mymanus.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for event persistence and retrieval
 */
@SpringBootTest
@ActiveProfiles("test")
class EventStreamIntegrationTest {

    @Autowired
    private EventService eventService;

    @Test
    void testEventStreamPersistence() {
        String sessionId = "event-test-" + System.currentTimeMillis();

        // Create events
        Event userMsg = eventService.appendUserMessage(sessionId, "Test message", 1);
        assertNotNull(userMsg);

        // Retrieve events
        List<Event> events = eventService.getEventStream(sessionId);
        assertFalse(events.isEmpty());
        assertEquals(sessionId, events.get(0).getAgentState().getSessionId());
    }

    @Test
    void testEventOrdering() {
        String sessionId = "ordering-test-" + System.currentTimeMillis();

        eventService.appendUserMessage(sessionId, "Message 1", 1);
        eventService.appendUserMessage(sessionId, "Message 2", 2);
        eventService.appendUserMessage(sessionId, "Message 3", 3);

        List<Event> events = eventService.getEventStream(sessionId);
        assertEquals(3, events.size());
        assertTrue(events.get(0).getSequence() < events.get(1).getSequence());
        assertTrue(events.get(1).getSequence() < events.get(2).getSequence());
    }
}
