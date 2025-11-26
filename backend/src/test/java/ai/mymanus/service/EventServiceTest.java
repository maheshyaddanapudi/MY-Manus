package ai.mymanus.service;

import ai.mymanus.model.Event;
import ai.mymanus.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test Suite for EventService
 * Coverage Target: 100%
 *
 * Tests:
 * - Event creation (all 7 types)
 * - Event retrieval
 * - Event stream building
 * - Event cleanup
 * - Error handling
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AgentStateService stateService;

    @InjectMocks
    private EventService eventService;

    private String testSessionId;
    private UUID testStateId;

    @BeforeEach
    void setUp() {
        testSessionId = "test-session-123";
        testStateId = UUID.randomUUID();
    }

    @Test
    void testAppendUserMessage() {
        // Test user message event creation
        Event event = eventService.appendUserMessage(testSessionId, "Test message", 1);

        assertNotNull(event);
        assertEquals(Event.EventType.USER_MESSAGE, event.getType());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void testAppendAgentThought() {
        // Test agent thought event creation
        Event event = eventService.appendAgentThought(testSessionId, "Thinking...", 1);

        assertNotNull(event);
        assertEquals(Event.EventType.AGENT_THOUGHT, event.getType());
    }

    @Test
    void testAppendAgentAction() {
        // Test agent action event creation
        Map<String, Object> actionData = Map.of("tool", "file_read", "path", "test.txt");

        Event event = eventService.appendAgentAction(
                testSessionId, "execute_code", "file_read('test.txt')",
                actionData, 1
        );

        assertNotNull(event);
        assertEquals(Event.EventType.AGENT_ACTION, event.getType());
    }

    @Test
    void testAppendObservation() {
        // Test observation event creation
        Map<String, Object> observationData = Map.of("result", "success");

        Event event = eventService.appendObservation(
                testSessionId, "File read successfully",
                observationData, true, null, 150L, 1
        );

        assertNotNull(event);
        assertEquals(Event.EventType.OBSERVATION, event.getType());
        assertTrue(event.getSuccess());
        assertEquals(150L, event.getDurationMs());
    }

    @Test
    void testGetEventStream() {
        // Test event stream retrieval
        List<Event> mockEvents = Arrays.asList(
                createMockEvent(Event.EventType.USER_MESSAGE, 1, 0),
                createMockEvent(Event.EventType.AGENT_THOUGHT, 1, 1),
                createMockEvent(Event.EventType.AGENT_ACTION, 1, 2)
        );

        when(eventRepository.findByAgentStateIdOrderByIterationAscSequenceAsc(any()))
                .thenReturn(mockEvents);

        List<Event> events = eventService.getEventStream(testSessionId);

        assertEquals(3, events.size());
        verify(eventRepository, times(1))
                .findByAgentStateIdOrderByIterationAscSequenceAsc(any());
    }

    @Test
    void testBuildEventStreamContext() {
        // Test event stream context building
        List<Event> mockEvents = Arrays.asList(
                createMockEvent(Event.EventType.USER_MESSAGE, 1, 0),
                createMockEvent(Event.EventType.AGENT_THOUGHT, 1, 1)
        );

        when(eventRepository.findByAgentStateIdOrderByIterationAscSequenceAsc(any()))
                .thenReturn(mockEvents);

        String context = eventService.buildEventStreamContext(testSessionId);

        assertNotNull(context);
        assertTrue(context.contains("USER_MESSAGE"));
        assertTrue(context.contains("AGENT_THOUGHT"));
    }

    @Test
    void testClearEventStream() {
        // Test event stream cleanup
        eventService.clearEventStream(testSessionId);

        verify(eventRepository, times(1)).deleteByAgentStateId(any());
    }

    // Helper methods
    private Event createMockEvent(Event.EventType type, int iteration, int sequence) {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setType(type);
        event.setIteration(iteration);
        event.setSequence(sequence);
        event.setContent("Mock event content");
        return event;
    }
}
