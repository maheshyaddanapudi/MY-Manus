package ai.mymanus.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Session model
 * Tests session management and lifecycle
 */
class SessionTest {

    private Session session;

    @BeforeEach
    void setUp() {
        session = new Session();
    }

    @Test
    void testSessionCreation() {
        session.setSessionId("test-session-123");
        session.setTitle("New Conversation");

        assertNotNull(session);
        assertEquals("test-session-123", session.getSessionId());
        assertEquals("New Conversation", session.getTitle());
    }

    @Test
    void testDefaultTitle() {
        session.setSessionId("session-1");
        session.setTitle("New Conversation");

        assertEquals("New Conversation", session.getTitle());
    }

    @Test
    void testCustomTitle() {
        session.setTitle("Debug Application Error");
        assertEquals("Debug Application Error", session.getTitle());
    }

    @Test
    void testSessionIdUniqueness() {
        Session session1 = new Session();
        session1.setSessionId("session-1");

        Session session2 = new Session();
        session2.setSessionId("session-2");

        assertNotEquals(session1.getSessionId(), session2.getSessionId());
    }

    @Test
    void testActiveStatus() {
        session.setActive(true);
        assertTrue(session.isActive());

        session.setActive(false);
        assertFalse(session.isActive());
    }

    @Test
    void testCreatedAtTimestamp() {
        long beforeTime = System.currentTimeMillis();

        session.setCreatedAt(new java.util.Date());

        long afterTime = System.currentTimeMillis();

        assertNotNull(session.getCreatedAt());
        assertTrue(session.getCreatedAt().getTime() >= beforeTime);
        assertTrue(session.getCreatedAt().getTime() <= afterTime);
    }

    @Test
    void testUpdatedAtTimestamp() {
        long beforeTime = System.currentTimeMillis();

        session.setUpdatedAt(new java.util.Date());

        long afterTime = System.currentTimeMillis();

        assertNotNull(session.getUpdatedAt());
        assertTrue(session.getUpdatedAt().getTime() >= beforeTime);
        assertTrue(session.getUpdatedAt().getTime() <= afterTime);
    }

    @Test
    void testTimestampOrdering() throws InterruptedException {
        session.setCreatedAt(new java.util.Date());

        Thread.sleep(10); // Small delay

        session.setUpdatedAt(new java.util.Date());

        assertTrue(session.getUpdatedAt().getTime() >= session.getCreatedAt().getTime());
    }

    @Test
    void testNullTitle() {
        session.setTitle(null);
        assertNull(session.getTitle());
    }

    @Test
    void testEmptyTitle() {
        session.setTitle("");
        assertEquals("", session.getTitle());
    }

    @Test
    void testLongTitle() {
        String longTitle = "a".repeat(500);
        session.setTitle(longTitle);
        assertEquals(500, session.getTitle().length());
    }

    @Test
    void testTitleWithSpecialCharacters() {
        String specialTitle = "Debug 🐛 - API Error (500) #urgent";
        session.setTitle(specialTitle);
        assertEquals(specialTitle, session.getTitle());
    }

    @Test
    void testSessionLifecycle() {
        // Create session
        session.setSessionId("lifecycle-test");
        session.setTitle("Test Session");
        session.setActive(true);
        session.setCreatedAt(new java.util.Date());

        assertEquals("lifecycle-test", session.getSessionId());
        assertTrue(session.isActive());

        // Deactivate session
        session.setActive(false);
        assertFalse(session.isActive());
    }
}
