package ai.mymanus.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Event model
 * Tests validation, persistence, and domain logic
 */
class EventTest {

    private Event event;
    private AgentState testAgentState;

    @BeforeEach
    void setUp() {
        testAgentState = AgentState.builder()
            .sessionId("test-session")
            .status(AgentState.Status.RUNNING)
            .build();
            
        event = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.USER_MESSAGE)
            .iteration(1)
            .sequence(1)
            .build();
    }

    @Test
    void testEventCreation() {
        assertNotNull(event);
        assertEquals(testAgentState, event.getAgentState());
        assertEquals(Event.EventType.USER_MESSAGE, event.getType());
        assertEquals(1, event.getSequence());
        assertEquals(1, event.getIteration());
    }

    @Test
    void testUserMessageEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("text", "Hello, agent!");
        data.put("timestamp", System.currentTimeMillis());

        Event userEvent = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.USER_MESSAGE)
            .iteration(1)
            .sequence(1)
            .data(data)
            .content("Hello, agent!")
            .build();

        assertEquals(Event.EventType.USER_MESSAGE, userEvent.getType());
        assertEquals("Hello, agent!", userEvent.getData().get("text"));
        assertEquals("Hello, agent!", userEvent.getContent());
    }

    @Test
    void testAgentThoughtEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("thought", "I need to read the file first");

        Event thoughtEvent = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.AGENT_THOUGHT)
            .iteration(1)
            .sequence(2)
            .data(data)
            .content("I need to read the file first")
            .build();

        assertEquals(Event.EventType.AGENT_THOUGHT, thoughtEvent.getType());
        assertTrue(thoughtEvent.getData().containsKey("thought"));
    }

    @Test
    void testAgentActionEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("tool", "file_read");
        data.put("parameters", Map.of("path", "/workspace/test.txt"));

        Event actionEvent = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.AGENT_ACTION)
            .iteration(1)
            .sequence(3)
            .data(data)
            .build();

        assertEquals(Event.EventType.AGENT_ACTION, actionEvent.getType());
        assertEquals("file_read", actionEvent.getData().get("tool"));
    }

    @Test
    void testObservationEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("result", "File content here");
        data.put("success", true);

        Event observationEvent = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.OBSERVATION)
            .iteration(1)
            .sequence(4)
            .data(data)
            .success(true)
            .durationMs(150L)
            .build();

        assertEquals(Event.EventType.OBSERVATION, observationEvent.getType());
        assertTrue(observationEvent.getSuccess());
        assertEquals(150L, observationEvent.getDurationMs());
    }

    @Test
    void testEventWithError() {
        Event errorEvent = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.OBSERVATION)
            .iteration(1)
            .sequence(5)
            .success(false)
            .error("File not found")
            .build();

        assertFalse(errorEvent.getSuccess());
        assertEquals("File not found", errorEvent.getError());
    }

    @Test
    void testEventSequencing() {
        Event event1 = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.USER_MESSAGE)
            .iteration(1)
            .sequence(1)
            .build();

        Event event2 = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.AGENT_THOUGHT)
            .iteration(1)
            .sequence(2)
            .build();

        Event event3 = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.AGENT_ACTION)
            .iteration(1)
            .sequence(3)
            .build();

        assertTrue(event1.getSequence() < event2.getSequence());
        assertTrue(event2.getSequence() < event3.getSequence());
    }

    @Test
    void testMultipleIterations() {
        Event iteration1Event = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.USER_MESSAGE)
            .iteration(1)
            .sequence(1)
            .build();

        Event iteration2Event = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.USER_MESSAGE)
            .iteration(2)
            .sequence(1)
            .build();

        assertEquals(1, iteration1Event.getIteration());
        assertEquals(2, iteration2Event.getIteration());
    }

    @Test
    void testEventTimestamp() {
        LocalDateTime beforeTime = LocalDateTime.now();

        Event timedEvent = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.USER_MESSAGE)
            .iteration(1)
            .sequence(1)
            .timestamp(LocalDateTime.now())
            .build();

        LocalDateTime afterTime = LocalDateTime.now();

        assertNotNull(timedEvent.getTimestamp());
        assertTrue(!timedEvent.getTimestamp().isBefore(beforeTime));
        assertTrue(!timedEvent.getTimestamp().isAfter(afterTime));
    }

    @Test
    void testEventWithContent() {
        String content = "This is the event content";
        Event contentEvent = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.USER_MESSAGE)
            .iteration(1)
            .sequence(1)
            .content(content)
            .build();

        assertEquals(content, contentEvent.getContent());
    }

    @Test
    void testEventWithNullData() {
        Event nullDataEvent = Event.builder()
            .agentState(testAgentState)
            .type(Event.EventType.USER_MESSAGE)
            .iteration(1)
            .sequence(1)
            .data(null)
            .build();

        assertNull(nullDataEvent.getData());
    }

    @Test
    void testAllEventTypes() {
        Event.EventType[] allTypes = Event.EventType.values();

        assertTrue(allTypes.length >= 4);
        assertTrue(containsType(allTypes, Event.EventType.USER_MESSAGE));
        assertTrue(containsType(allTypes, Event.EventType.AGENT_THOUGHT));
        assertTrue(containsType(allTypes, Event.EventType.AGENT_ACTION));
        assertTrue(containsType(allTypes, Event.EventType.OBSERVATION));
    }

    private boolean containsType(Event.EventType[] types, Event.EventType target) {
        for (Event.EventType type : types) {
            if (type == target) return true;
        }
        return false;
    }
}
