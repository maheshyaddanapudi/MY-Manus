package ai.mymanus.integration;

import ai.mymanus.MyManusApplication;
import ai.mymanus.config.IntegrationTestConfiguration;
import ai.mymanus.model.AgentState;
import ai.mymanus.repository.AgentStateRepository;
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
 * Integration test for multi-session management
 */
@SpringBootTest(classes = MyManusApplication.class)
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class SessionManagementIntegrationTest {

    @Autowired
    private AgentStateRepository agentStateRepository;

    @Test
    @Transactional
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
    @Transactional
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
