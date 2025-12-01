package ai.mymanus.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Message model
 * Tests message creation and validation
 */
class MessageTest {

    private Message message;
    private AgentState testAgentState;

    @BeforeEach
    void setUp() {
        testAgentState = AgentState.builder()
            .sessionId("test-session")
            .status(AgentState.Status.RUNNING)
            .build();
            
        message = Message.builder()
            .agentState(testAgentState)
            .role(Message.MessageRole.USER)
            .content("Hello, agent!")
            .build();
    }

    @Test
    void testMessageCreation() {
        assertNotNull(message);
        assertEquals(testAgentState, message.getAgentState());
        assertEquals(Message.MessageRole.USER, message.getRole());
        assertEquals("Hello, agent!", message.getContent());
    }

    @Test
    void testUserMessage() {
        Message userMsg = Message.builder()
            .agentState(testAgentState)
            .role(Message.MessageRole.USER)
            .content("Please read the file")
            .build();

        assertEquals(Message.MessageRole.USER, userMsg.getRole());
        assertEquals("Please read the file", userMsg.getContent());
    }

    @Test
    void testAssistantMessage() {
        Message assistantMsg = Message.builder()
            .agentState(testAgentState)
            .role(Message.MessageRole.ASSISTANT)
            .content("I'll read the file for you")
            .build();

        assertEquals(Message.MessageRole.ASSISTANT, assistantMsg.getRole());
        assertEquals("I'll read the file for you", assistantMsg.getContent());
    }

    @Test
    void testSystemMessage() {
        Message systemMsg = Message.builder()
            .agentState(testAgentState)
            .role(Message.MessageRole.SYSTEM)
            .content("You are a helpful AI agent")
            .build();

        assertEquals(Message.MessageRole.SYSTEM, systemMsg.getRole());
        assertEquals("You are a helpful AI agent", systemMsg.getContent());
    }

    @Test
    void testAllRoles() {
        Message.MessageRole[] allRoles = Message.MessageRole.values();

        // Verify all 3 roles exist
        assertEquals(3, allRoles.length);

        assertTrue(containsRole(allRoles, Message.MessageRole.USER));
        assertTrue(containsRole(allRoles, Message.MessageRole.ASSISTANT));
        assertTrue(containsRole(allRoles, Message.MessageRole.SYSTEM));
    }

    @Test
    void testEmptyContent() {
        Message msg = Message.builder()
            .agentState(testAgentState)
            .role(Message.MessageRole.USER)
            .content("")
            .build();
            
        assertEquals("", msg.getContent());
    }

    @Test
    void testNullContent() {
        Message msg = Message.builder()
            .agentState(testAgentState)
            .role(Message.MessageRole.USER)
            .content(null)
            .build();
            
        assertNull(msg.getContent());
    }

    @Test
    void testLongContent() {
        String longContent = "a".repeat(10000);
        Message msg = Message.builder()
            .agentState(testAgentState)
            .role(Message.MessageRole.USER)
            .content(longContent)
            .build();
            
        assertEquals(10000, msg.getContent().length());
    }

    @Test
    void testMessageWithNewlines() {
        String multilineContent = "Line 1\nLine 2\nLine 3";
        Message msg = Message.builder()
            .agentState(testAgentState)
            .role(Message.MessageRole.USER)
            .content(multilineContent)
            .build();
            
        assertEquals(multilineContent, msg.getContent());
        assertTrue(msg.getContent().contains("\n"));
    }

    @Test
    void testMessageWithSpecialCharacters() {
        String specialContent = "Hello 世界! 🌍 <>&\"'";
        Message msg = Message.builder()
            .agentState(testAgentState)
            .role(Message.MessageRole.USER)
            .content(specialContent)
            .build();
            
        assertEquals(specialContent, msg.getContent());
    }

    @Test
    void testAgentStateAssociation() {
        AgentState state1 = AgentState.builder()
            .sessionId("session-123")
            .build();
            
        AgentState state2 = AgentState.builder()
            .sessionId("session-456")
            .build();

        Message msg1 = Message.builder()
            .agentState(state1)
            .role(Message.MessageRole.USER)
            .content("Message 1")
            .build();
            
        Message msg2 = Message.builder()
            .agentState(state2)
            .role(Message.MessageRole.USER)
            .content("Message 2")
            .build();

        assertEquals(state1, msg1.getAgentState());
        assertEquals(state2, msg2.getAgentState());
        assertNotEquals(msg1.getAgentState(), msg2.getAgentState());
    }

    @Test
    void testTimestampGeneration() {
        LocalDateTime beforeTime = LocalDateTime.now();

        Message msg = Message.builder()
            .agentState(testAgentState)
            .role(Message.MessageRole.USER)
            .content("Test")
            .timestamp(LocalDateTime.now())
            .build();

        LocalDateTime afterTime = LocalDateTime.now();

        assertNotNull(msg.getTimestamp());
        assertTrue(!msg.getTimestamp().isBefore(beforeTime));
        assertTrue(!msg.getTimestamp().isAfter(afterTime));
    }

    @Test
    void testBuilderPattern() {
        Message msg = Message.builder()
            .agentState(testAgentState)
            .role(Message.MessageRole.USER)
            .content("Builder test")
            .timestamp(LocalDateTime.now())
            .build();

        assertNotNull(msg);
        assertEquals(testAgentState, msg.getAgentState());
        assertEquals(Message.MessageRole.USER, msg.getRole());
        assertEquals("Builder test", msg.getContent());
        assertNotNull(msg.getTimestamp());
    }

    private boolean containsRole(Message.MessageRole[] roles, Message.MessageRole target) {
        for (Message.MessageRole role : roles) {
            if (role == target) return true;
        }
        return false;
    }
}
