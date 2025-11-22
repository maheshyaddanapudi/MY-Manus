package ai.mymanus.integration;

import ai.mymanus.model.Session;
import ai.mymanus.repository.SessionRepository;
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
    private SessionRepository sessionRepository;

    @Test
    void testCreateMultipleSessions() {
        Session session1 = new Session();
        session1.setSessionId("session-1-" + System.currentTimeMillis());
        session1.setTitle("Conversation 1");
        session1.setActive(true);

        Session session2 = new Session();
        session2.setSessionId("session-2-" + System.currentTimeMillis());
        session2.setTitle("Conversation 2");
        session2.setActive(true);

        sessionRepository.save(session1);
        sessionRepository.save(session2);

        List<Session> sessions = sessionRepository.findAll();
        assertTrue(sessions.size() >= 2);
    }

    @Test
    void testSessionLifecycle() {
        String sessionId = "lifecycle-" + System.currentTimeMillis();

        // Create
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setTitle("Test Session");
        session.setActive(true);
        sessionRepository.save(session);

        // Read
        Session retrieved = sessionRepository.findBySessionId(sessionId).orElse(null);
        assertNotNull(retrieved);
        assertEquals("Test Session", retrieved.getTitle());

        // Delete
        sessionRepository.deleteBySessionId(sessionId);
        assertTrue(sessionRepository.findBySessionId(sessionId).isEmpty());
    }
}
