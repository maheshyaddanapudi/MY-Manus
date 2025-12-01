package ai.mymanus.integration;

import ai.mymanus.model.AgentState;
import ai.mymanus.repository.AgentStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for multi-session management
 */
@SpringBootTest
@ActiveProfiles("test")
class SessionManagementIntegrationTest {

    @Autowired
    private AgentStateRepository agentStateRepository;

    @Test
    void testCreateMultipleSessions() {
        AgentState session1 = AgentState.builder()
            .sessionId("session-1-" + System.currentTimeMillis())
            .title("Conversation 1")
            .build();

        AgentState session2 = AgentState.builder()
            .sessionId("session-2-" + System.currentTimeMillis())
            .title("Conversation 2")
            .build();

        agentStateRepository.save(session1);
        agentStateRepository.save(session2);

        List<AgentState> sessions = agentStateRepository.findAll();
        assertTrue(sessions.size() >= 2);
    }

    @Test
    void testSessionLifecycle() {
        String sessionId = "lifecycle-" + System.currentTimeMillis();

        // Create
        AgentState session = AgentState.builder()
            .sessionId(sessionId)
            .title("Test Session")
            .build();
        agentStateRepository.save(session);

        // Read
        AgentState retrieved = agentStateRepository.findBySessionId(sessionId).orElse(null);
        assertNotNull(retrieved);
        assertEquals("Test Session", retrieved.getTitle());

        // Delete
        agentStateRepository.deleteBySessionId(sessionId);
        assertTrue(agentStateRepository.findBySessionId(sessionId).isEmpty());
    }
}
