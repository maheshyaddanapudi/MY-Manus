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

        // Create AgentState first (Event requires it)
        AgentState agentState = AgentState.builder()
            .sessionId("db-test-" + System.currentTimeMillis())
            .status(AgentState.Status.RUNNING)
            .build();
        agentState = agentStateRepository.save(agentState);

        Event event = Event.builder()
            .agentState(agentState)
            .type(Event.EventType.USER_MESSAGE)
            .sequence(1)
            .data(data)
            .iteration(1)
            .build();

        Event saved = eventRepository.save(event);
        assertNotNull(saved.getId());
        assertEquals("Test message", saved.getData().get("text"));
    }

    @Test
    void testMessagePersistence() {
        // Create AgentState first
        AgentState state = AgentState.builder()
            .sessionId("db-test-" + System.currentTimeMillis())
            .status(AgentState.Status.IDLE)
            .iteration(0)
            .build();
        AgentState savedState = agentStateRepository.save(state);

        Message message = new Message();
        message.setAgentState(savedState);
        message.setRole(Message.MessageRole.USER);
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
        state.setExecutionContext(variables);

        AgentState saved = agentStateRepository.save(state);
        assertNotNull(saved.getId());
        assertEquals(5, saved.getExecutionContext().get("counter"));
    }
}
