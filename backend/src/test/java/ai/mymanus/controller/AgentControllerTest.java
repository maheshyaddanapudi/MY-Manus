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
import java.util.Set;

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

    private String testSessionId;

    @BeforeEach
    void setUp() {
        testSessionId = UUID.randomUUID().toString();
    }

    @Test
    void testSendMessage() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setSessionId(testSessionId);
        request.setMessage("Hello, agent!");

        when(codeActAgentService.processQuery(anyString(), anyString()))
            .thenReturn("Hello! How can I help you?");

        mockMvc.perform(post("/api/agent/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Hello! How can I help you?"))
            .andExpect(jsonPath("$.sessionId").value(testSessionId));

        verify(codeActAgentService, times(1)).processQuery(testSessionId, "Hello, agent!");
    }

    @Test
    void testGetSession() throws Exception {
        Map<String, Object> status = new HashMap<>();
        status.put("sessionId", testSessionId);
        status.put("exists", true);
        status.put("messageCount", 5);
        status.put("context", Set.of("x", "y"));
        status.put("metadata", Map.of("title", "Test Session"));

        when(codeActAgentService.getSessionStatus(testSessionId))
            .thenReturn(status);

        mockMvc.perform(get("/api/agent/session/" + testSessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(testSessionId))
            .andExpect(jsonPath("$.exists").value(true))
            .andExpect(jsonPath("$.messageCount").value(5));

        verify(codeActAgentService, times(1)).getSessionStatus(testSessionId);
    }

    @Test
    void testGetMessages() throws Exception {
        List<Message> messages = new ArrayList<>();
        // Messages don't need to be fully populated for this test
        
        when(agentStateService.getMessages(testSessionId))
            .thenReturn(messages);

        mockMvc.perform(get("/api/agent/session/" + testSessionId + "/messages"))
            .andExpect(status().isOk());

        verify(agentStateService, times(1)).getMessages(testSessionId);
    }

    @Test
    void testGetEvents() throws Exception {
        List<Event> events = new ArrayList<>();
        
        when(eventService.getEventStream(testSessionId))
            .thenReturn(events);

        mockMvc.perform(get("/api/agent/session/" + testSessionId + "/events"))
            .andExpect(status().isOk());

        verify(eventService, times(1)).getEventStream(testSessionId);
    }

    @Test
    void testCreateSession() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("title", "New Session");

        AgentState agentState = AgentState.builder()
            .sessionId(testSessionId)
            .build();

        when(agentStateService.createSession(anyString()))
            .thenReturn(agentState);

        mockMvc.perform(post("/api/agent/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").exists());
    }

    @Test
    void testDeleteSession() throws Exception {
        doNothing().when(codeActAgentService).clearSession(testSessionId);

        mockMvc.perform(delete("/api/agent/session/" + testSessionId))
            .andExpect(status().isOk());

        verify(codeActAgentService, times(1)).clearSession(testSessionId);
    }

    @Test
    void testGetAllSessions() throws Exception {
        List<AgentState> sessions = new ArrayList<>();
        
        when(agentStateService.listAllSessions())
            .thenReturn(sessions);

        mockMvc.perform(get("/api/agent/sessions"))
            .andExpect(status().isOk());

        verify(agentStateService, times(1)).listAllSessions();
    }

    @Test
    void testEmptyMessage() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setSessionId(testSessionId);
        request.setMessage("");

        // Controller accepts empty messages
        when(codeActAgentService.processQuery(anyString(), anyString()))
            .thenReturn("I didn't receive any message.");

        mockMvc.perform(post("/api/agent/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void testInvalidMessageFormat() throws Exception {
        String invalidJson = "{\"invalid\": }";

        mockMvc.perform(post("/api/agent/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }
}
