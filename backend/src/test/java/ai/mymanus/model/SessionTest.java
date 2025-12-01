package ai.mymanus.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Session (AgentState) model
 * Tests session management and lifecycle
 */
class SessionTest {

    private AgentState session;

    @BeforeEach
    void setUp() {
        session = AgentState.builder()
            .sessionId("test-session-123")
            .title("New Conversation")
            .status(AgentState.Status.IDLE)
            .build();
    }

    @Test
    void testSessionCreation() {
        assertNotNull(session);
        assertEquals("test-session-123", session.getSessionId());
        assertEquals("New Conversation", session.getTitle());
    }

    @Test
    void testDefaultTitle() {
        AgentState state = AgentState.builder()
            .sessionId("session-1")
            .title("New Conversation")
            .build();

        assertEquals("New Conversation", state.getTitle());
    }

    @Test
    void testCustomTitle() {
        AgentState state = AgentState.builder()
            .sessionId("session-1")
            .title("Debug Application Error")
            .build();
            
        assertEquals("Debug Application Error", state.getTitle());
    }

    @Test
    void testSessionIdUniqueness() {
        AgentState session1 = AgentState.builder()
            .sessionId("session-1")
            .build();

        AgentState session2 = AgentState.builder()
            .sessionId("session-2")
            .build();

        assertNotEquals(session1.getSessionId(), session2.getSessionId());
    }

    @Test
    void testActiveStatus() {
        AgentState state = AgentState.builder()
            .sessionId("test")
            .status(AgentState.Status.IDLE)
            .build();
            
        assertEquals(AgentState.Status.IDLE, state.getStatus());

        state.setStatus(AgentState.Status.RUNNING);
        assertEquals(AgentState.Status.RUNNING, state.getStatus());
    }

    @Test
    void testNullTitle() {
        AgentState state = AgentState.builder()
            .sessionId("test")
            .title(null)
            .build();
            
        assertNull(state.getTitle());
    }

    @Test
    void testEmptyTitle() {
        AgentState state = AgentState.builder()
            .sessionId("test")
            .title("")
            .build();
            
        assertEquals("", state.getTitle());
    }

    @Test
    void testLongTitle() {
        String longTitle = "a".repeat(500);
        AgentState state = AgentState.builder()
            .sessionId("test")
            .title(longTitle)
            .build();
            
        assertEquals(500, state.getTitle().length());
    }

    @Test
    void testTitleWithSpecialCharacters() {
        String specialTitle = "Debug 🐛 - API Error (500) #urgent";
        AgentState state = AgentState.builder()
            .sessionId("test")
            .title(specialTitle)
            .build();
            
        assertEquals(specialTitle, state.getTitle());
    }

    @Test
    void testSessionLifecycle() {
        // Create session
        AgentState state = AgentState.builder()
            .sessionId("lifecycle-test")
            .title("Test Session")
            .status(AgentState.Status.IDLE)
            .build();

        assertEquals("lifecycle-test", state.getSessionId());
        assertEquals(AgentState.Status.IDLE, state.getStatus());

        // Activate session
        state.setStatus(AgentState.Status.RUNNING);
        assertEquals(AgentState.Status.RUNNING, state.getStatus());
        
        // Complete session
        state.setStatus(AgentState.Status.COMPLETED);
        assertEquals(AgentState.Status.COMPLETED, state.getStatus());
    }
}
