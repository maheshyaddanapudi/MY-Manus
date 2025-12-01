package ai.mymanus.controller;

import ai.mymanus.model.Event;
import ai.mymanus.model.Message;
import ai.mymanus.model.AgentState;
import ai.mymanus.service.CodeActAgentService;
import ai.mymanus.service.EventService;
import ai.mymanus.repository.MessageRepository;
import ai.mymanus.repository.AgentStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AgentController
 * Tests REST API endpoints
 */
@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CodeActAgentService codeActAgentService;

    @MockBean
    private EventService eventService;

    @MockBean
    private MessageRepository messageRepository;

    @MockBean
    private AgentStateRepository agentStateRepository;

    private String testSessionId = "test-session-123";

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    @Test
    void testSendMessage() throws Exception {
        String messageJson = """
            {
                "sessionId": "test-session-123",
                "message": "Read /workspace/test.txt"
            }
            """;

        when(codeActAgentService.processQuery(anyString(), anyString()))
            .thenReturn("Task completed");

        mockMvc.perform(post("/api/agent/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(messageJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(codeActAgentService, times(1))
            .processQuery(eq("test-session-123"), eq("Read /workspace/test.txt"));
    }

    @Test
    void testGetEventStream() throws Exception {
        List<Event> events = new ArrayList<>();

        // Create AgentState for Event
        AgentState agentState = AgentState.builder()
            .sessionId(testSessionId)
            .build();

        Map<String, Object> userData = new HashMap<>();
        userData.put("text", "Hello");

        Event event = Event.builder()
            .agentState(agentState)
            .type(Event.EventType.USER_MESSAGE)
            .sequence(1)
            .data(userData)
            .iteration(1)
            .build();

        events.add(event);

        when(eventService.getEventStream(testSessionId))
            .thenReturn(events);

        mockMvc.perform(get("/api/agent/events/" + testSessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(eventService, times(1)).getEventStream(testSessionId);
    }

    @Test
    void testGetMessages() throws Exception {
        List<Message> messages = new ArrayList<>();

        AgentState agentState = AgentState.builder()
            .sessionId(testSessionId)
            .build();

        Message message = Message.builder()
            .agentState(agentState)
            .role(Message.MessageRole.USER)
            .content("Test message")
            .build();

        messages.add(message);

        when(messageRepository.findByAgentStateIdOrderByTimestampAsc(any()))
            .thenReturn(messages);

        mockMvc.perform(get("/api/agent/messages/" + testSessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].content").value("Test message"));

        verify(messageRepository, times(1))
            .findByAgentStateIdOrderByTimestampAsc(any());
    }

    @Test
    void testCreateSession() throws Exception {
        String sessionJson = """
            {
                "title": "New Conversation"
            }
            """;

        AgentState agentState = AgentState.builder()
            .sessionId("new-session-123")
            .title("New Conversation")
            .build();

        when(agentStateRepository.save(any(AgentState.class)))
            .thenReturn(agentState);

        mockMvc.perform(post("/api/agent/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("new-session-123"))
            .andExpect(jsonPath("$.title").value("New Conversation"));

        verify(agentStateRepository, times(1)).save(any(AgentState.class));
    }

    @Test
    void testGetAllSessions() throws Exception {
        List<AgentState> sessions = new ArrayList<>();

        AgentState session1 = AgentState.builder()
            .sessionId("session-1")
            .title("Conversation 1")
            .build();

        AgentState session2 = AgentState.builder()
            .sessionId("session-2")
            .title("Conversation 2")
            .build();

        sessions.add(session1);
        sessions.add(session2);

        when(agentStateRepository.findAll())
            .thenReturn(sessions);

        mockMvc.perform(get("/api/agent/sessions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].sessionId").value("session-1"));

        verify(agentStateRepository, times(1)).findAll();
    }

    @Test
    void testDeleteSession() throws Exception {
        doNothing().when(agentStateRepository).deleteBySessionId(testSessionId);

        mockMvc.perform(delete("/api/agent/sessions/" + testSessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(agentStateRepository, times(1)).deleteBySessionId(testSessionId);
    }

    @Test
    void testInvalidMessageFormat() throws Exception {
        String invalidJson = "{ invalid json }";

        mockMvc.perform(post("/api/agent/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testEmptyMessage() throws Exception {
        String emptyMessageJson = """
            {
                "sessionId": "test-session-123",
                "message": ""
            }
            """;

        mockMvc.perform(post("/api/agent/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyMessageJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingSessionId() throws Exception {
        String noSessionJson = """
            {
                "message": "Test message"
            }
            """;

        mockMvc.perform(post("/api/agent/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(noSessionJson))
            .andExpect(status().isBadRequest());
    }
}
