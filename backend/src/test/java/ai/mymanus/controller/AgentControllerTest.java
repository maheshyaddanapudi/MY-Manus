package ai.mymanus.controller;

import ai.mymanus.config.TestSecurityConfig;
import ai.mymanus.dto.ChatRequest;
import ai.mymanus.dto.ChatResponse;
import ai.mymanus.model.Event;
import ai.mymanus.model.Message;
import ai.mymanus.model.AgentState;
import ai.mymanus.service.AgentStateService;
import ai.mymanus.service.CodeActAgentService;
import ai.mymanus.service.EventService;
import ai.mymanus.repository.MessageRepository;
import ai.mymanus.repository.AgentStateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AgentController
 * Tests REST API endpoints with correct URL mappings
 */
@WebMvcTest(AgentController.class)
@Import(TestSecurityConfig.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CodeActAgentService codeActAgentService;

    @MockBean
    private AgentStateService agentStateService;

    @MockBean
    private EventService eventService;

    @MockBean
    private MessageRepository messageRepository;

    @MockBean
    private AgentStateRepository agentStateRepository;

    private String testSessionId = "test-session-123";
    private UUID testUUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    @Test
    void testSendMessage() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setSessionId(testSessionId);
        request.setMessage("Read /workspace/test.txt");

        when(codeActAgentService.processQuery(anyString(), anyString()))
            .thenReturn("Task completed");

        mockMvc.perform(post("/api/agent/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(testSessionId))
            .andExpect(jsonPath("$.status").value("completed"));

        verify(codeActAgentService, times(1))
            .processQuery(eq(testSessionId), eq("Read /workspace/test.txt"));
    }

    @Test
    void testGetSession() throws Exception {
        AgentState agentState = AgentState.builder()
            .id(testUUID)
            .sessionId(testSessionId)
            .title("Test Session")
            .status(AgentState.Status.IDLE)
            .iteration(0)
            .build();

        when(agentStateService.getSession(testSessionId))
            .thenReturn(agentState);

        mockMvc.perform(get("/api/agent/session/" + testSessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(testSessionId))
            .andExpect(jsonPath("$.title").value("Test Session"));

        verify(agentStateService, times(1)).getSession(testSessionId);
    }

    @Test
    void testGetMessages() throws Exception {
        List<Message> messages = new ArrayList<>();

        AgentState agentState = AgentState.builder()
            .id(testUUID)
            .sessionId(testSessionId)
            .build();

        Message message = Message.builder()
            .agentState(agentState)
            .role(Message.MessageRole.USER)
            .content("Test message")
            .build();

        messages.add(message);

        when(agentStateService.getSession(testSessionId))
            .thenReturn(agentState);
        when(messageRepository.findByAgentStateIdOrderByTimestampAsc(testUUID))
            .thenReturn(messages);

        mockMvc.perform(get("/api/agent/session/" + testSessionId + "/messages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].content").value("Test message"));

        verify(agentStateService, times(1)).getSession(testSessionId);
        verify(messageRepository, times(1)).findByAgentStateIdOrderByTimestampAsc(testUUID);
    }

    @Test
    void testGetEvents() throws Exception {
        List<Event> events = new ArrayList<>();

        AgentState agentState = AgentState.builder()
            .id(testUUID)
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

        mockMvc.perform(get("/api/agent/session/" + testSessionId + "/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(eventService, times(1)).getEventStream(testSessionId);
    }

    @Test
    void testCreateSession() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("title", "New Conversation");

        AgentState agentState = AgentState.builder()
            .id(testUUID)
            .sessionId("new-session-123")
            .title("New Conversation")
            .status(AgentState.Status.IDLE)
            .iteration(0)
            .build();

        when(agentStateRepository.save(any(AgentState.class)))
            .thenReturn(agentState);

        mockMvc.perform(post("/api/agent/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").exists())
            .andExpect(jsonPath("$.title").value("New Conversation"));

        verify(agentStateRepository, times(1)).save(any(AgentState.class));
    }

    @Test
    void testGetAllSessions() throws Exception {
        List<AgentState> sessions = new ArrayList<>();

        AgentState session1 = AgentState.builder()
            .id(UUID.randomUUID())
            .sessionId("session-1")
            .title("Conversation 1")
            .status(AgentState.Status.IDLE)
            .iteration(0)
            .build();

        AgentState session2 = AgentState.builder()
            .id(UUID.randomUUID())
            .sessionId("session-2")
            .title("Conversation 2")
            .status(AgentState.Status.IDLE)
            .iteration(0)
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

        mockMvc.perform(delete("/api/agent/session/" + testSessionId))
            .andExpect(status().isOk());

        verify(agentStateRepository, times(1)).deleteBySessionId(testSessionId);
    }

    @Test
    void testInvalidMessageFormat() throws Exception {
        String invalidJson = "{ invalid json }";

        mockMvc.perform(post("/api/agent/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testEmptyMessage() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setSessionId(testSessionId);
        request.setMessage("");

        mockMvc.perform(post("/api/agent/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
