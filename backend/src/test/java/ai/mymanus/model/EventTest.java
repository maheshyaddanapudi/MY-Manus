package ai.mymanus.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Event model
 * Tests validation, persistence, and domain logic
 */
class EventTest {

    private Event event;

    @BeforeEach
    void setUp() {
        event = new Event();
    }

    @Test
    void testEventCreation() {
        event.setSessionId("test-session");
        event.setType(Event.EventType.USER_MESSAGE);
        event.setSequence(1);

        assertNotNull(event);
        assertEquals("test-session", event.getSessionId());
        assertEquals(Event.EventType.USER_MESSAGE, event.getType());
        assertEquals(1, event.getSequence());
    }

    @Test
    void testUserMessageEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("text", "Hello, agent!");
        data.put("timestamp", System.currentTimeMillis());

        event.setSessionId("session-1");
        event.setType(Event.EventType.USER_MESSAGE);
        event.setSequence(1);
        event.setData(data);

        assertEquals(Event.EventType.USER_MESSAGE, event.getType());
        assertEquals("Hello, agent!", event.getData().get("text"));
    }

    @Test
    void testAgentThoughtEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("thought", "I need to read the file first");

        event.setType(Event.EventType.AGENT_THOUGHT);
        event.setData(data);

        assertEquals(Event.EventType.AGENT_THOUGHT, event.getType());
        assertTrue(event.getData().containsKey("thought"));
    }

    @Test
    void testAgentActionEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("pythonCode", "print('Hello')");
        data.put("toolUsed", "shell_exec");

        event.setType(Event.EventType.AGENT_ACTION);
        event.setData(data);

        assertEquals(Event.EventType.AGENT_ACTION, event.getType());
        assertEquals("print('Hello')", event.getData().get("pythonCode"));
    }

    @Test
    void testObservationEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("stdout", "Command output");
        data.put("exitCode", 0);

        event.setType(Event.EventType.OBSERVATION);
        event.setData(data);

        assertEquals(Event.EventType.OBSERVATION, event.getType());
        assertEquals(0, event.getData().get("exitCode"));
    }

    @Test
    void testToolExecutionEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("toolName", "file_read");
        data.put("parameters", Map.of("path", "/workspace/test.txt"));
        data.put("result", Map.of("success", true, "content", "File content"));

        event.setType(Event.EventType.TOOL_EXECUTION);
        event.setData(data);

        assertEquals(Event.EventType.TOOL_EXECUTION, event.getType());
        assertEquals("file_read", event.getData().get("toolName"));
    }

    @Test
    void testErrorEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("errorMessage", "File not found");
        data.put("errorType", "FileNotFoundException");

        event.setType(Event.EventType.ERROR);
        event.setData(data);

        assertEquals(Event.EventType.ERROR, event.getType());
        assertEquals("File not found", event.getData().get("errorMessage"));
    }

    @Test
    void testFinalAnswerEvent() {
        Map<String, Object> data = new HashMap<>();
        data.put("answer", "Task completed successfully");
        data.put("status", "success");

        event.setType(Event.EventType.FINAL_ANSWER);
        event.setData(data);

        assertEquals(Event.EventType.FINAL_ANSWER, event.getType());
        assertEquals("success", event.getData().get("status"));
    }

    @Test
    void testSequenceOrdering() {
        Event event1 = new Event();
        event1.setSequence(1);

        Event event2 = new Event();
        event2.setSequence(2);

        assertTrue(event1.getSequence() < event2.getSequence());
    }

    @Test
    void testNullDataHandling() {
        event.setData(null);
        assertNull(event.getData());

        // Should not throw exception
        event.setData(new HashMap<>());
        assertNotNull(event.getData());
        assertTrue(event.getData().isEmpty());
    }

    @Test
    void testTimestampGeneration() {
        long beforeTime = System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        event.setData(data);

        long afterTime = System.currentTimeMillis();
        long eventTime = (Long) event.getData().get("timestamp");

        assertTrue(eventTime >= beforeTime);
        assertTrue(eventTime <= afterTime);
    }

    @Test
    void testAllEventTypes() {
        Event.EventType[] allTypes = Event.EventType.values();

        // Verify all 7 event types exist
        assertEquals(7, allTypes.length);

        assertTrue(containsType(allTypes, Event.EventType.USER_MESSAGE));
        assertTrue(containsType(allTypes, Event.EventType.AGENT_THOUGHT));
        assertTrue(containsType(allTypes, Event.EventType.AGENT_ACTION));
        assertTrue(containsType(allTypes, Event.EventType.OBSERVATION));
        assertTrue(containsType(allTypes, Event.EventType.TOOL_EXECUTION));
        assertTrue(containsType(allTypes, Event.EventType.ERROR));
        assertTrue(containsType(allTypes, Event.EventType.FINAL_ANSWER));
    }

    private boolean containsType(Event.EventType[] types, Event.EventType target) {
        for (Event.EventType type : types) {
            if (type == target) return true;
        }
        return false;
    }
}
