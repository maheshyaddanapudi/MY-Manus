package ai.mymanus.integration;

import ai.mymanus.model.Event;
import ai.mymanus.model.Message;
import ai.mymanus.model.AgentState;
import ai.mymanus.repository.EventRepository;
import ai.mymanus.repository.MessageRepository;
import ai.mymanus.repository.AgentStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for PostgreSQL persistence
 */
@SpringBootTest
@ActiveProfiles("test")
class DatabasePersistenceIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AgentStateRepository agentStateRepository;

    @Test
    void testEventPersistence() {
        Map<String, Object> data = new HashMap<>();
        data.put("text", "Test message");

        Event event = new Event();
        event.setSessionId("db-test-" + System.currentTimeMillis());
        event.setType(Event.EventType.USER_MESSAGE);
        event.setSequence(1);
        event.setData(data);

        Event saved = eventRepository.save(event);
        assertNotNull(saved.getId());
        assertEquals("Test message", saved.getData().get("text"));
    }

    @Test
    void testMessagePersistence() {
        Message message = new Message();
        message.setSessionId("db-test-" + System.currentTimeMillis());
        message.setRole(Message.Role.USER);
        message.setContent("Test content");

        Message saved = messageRepository.save(message);
        assertNotNull(saved.getId());
        assertEquals("Test content", saved.getContent());
    }

    @Test
    void testAgentStatePersistence() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("counter", 5);

        AgentState state = new AgentState();
        state.setSessionId("state-test-" + System.currentTimeMillis());
        state.setIteration(1);
        state.setStatus(AgentState.Status.RUNNING);
        state.setPythonVariables(variables);

        AgentState saved = agentStateRepository.save(state);
        assertNotNull(saved.getId());
        assertEquals(5, saved.getPythonVariables().get("counter"));
    }
}
