package ai.mymanus.controller;

import ai.mymanus.model.Event;
import ai.mymanus.model.Message;
import ai.mymanus.model.Session;
import ai.mymanus.model.ToolExecution;
import ai.mymanus.service.CodeActAgentService;
import ai.mymanus.service.EventService;
import ai.mymanus.repository.MessageRepository;
import ai.mymanus.repository.SessionRepository;
import ai.mymanus.repository.ToolExecutionRepository;
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
    private SessionRepository sessionRepository;

    @MockBean
    private ToolExecutionRepository toolExecutionRepository;

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

        doNothing().when(codeActAgentService)
            .processUserMessage(anyString(), anyString());

        mockMvc.perform(post("/api/agent/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(messageJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(codeActAgentService, times(1))
            .processUserMessage(eq("test-session-123"), eq("Read /workspace/test.txt"));
    }

    @Test
    void testGetEventStream() throws Exception {
        List<Event> events = new ArrayList<>();

        Map<String, Object> userData = new HashMap<>();
        userData.put("text", "Hello");

        Event event = new Event();
        event.setSessionId(testSessionId);
        event.setType(Event.EventType.USER_MESSAGE);
        event.setSequence(1);
        event.setData(userData);

        events.add(event);

        when(eventService.getEventStream(testSessionId))
            .thenReturn(events);

        mockMvc.perform(get("/api/agent/events/" + testSessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].sessionId").value(testSessionId));

        verify(eventService, times(1)).getEventStream(testSessionId);
    }

    @Test
    void testGetMessages() throws Exception {
        List<Message> messages = new ArrayList<>();

        Message message = new Message();
        message.setSessionId(testSessionId);
        message.setRole(Message.Role.USER);
        message.setContent("Test message");

        messages.add(message);

        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(testSessionId))
            .thenReturn(messages);

        mockMvc.perform(get("/api/agent/messages/" + testSessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].content").value("Test message"));

        verify(messageRepository, times(1))
            .findBySessionIdOrderByCreatedAtAsc(testSessionId);
    }

    @Test
    void testGetToolExecutions() throws Exception {
        List<ToolExecution> executions = new ArrayList<>();

        ToolExecution execution = new ToolExecution();
        execution.setSessionId(testSessionId);
        execution.setToolName("file_read");
        execution.setIteration(1);

        executions.add(execution);

        when(toolExecutionRepository.findBySessionIdOrderByCreatedAtAsc(testSessionId))
            .thenReturn(executions);

        mockMvc.perform(get("/api/agent/tool-executions/" + testSessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].toolName").value("file_read"));

        verify(toolExecutionRepository, times(1))
            .findBySessionIdOrderByCreatedAtAsc(testSessionId);
    }

    @Test
    void testCreateSession() throws Exception {
        String sessionJson = """
            {
                "title": "New Conversation"
            }
            """;

        Session session = new Session();
        session.setSessionId("new-session-123");
        session.setTitle("New Conversation");

        when(sessionRepository.save(any(Session.class)))
            .thenReturn(session);

        mockMvc.perform(post("/api/agent/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("new-session-123"))
            .andExpect(jsonPath("$.title").value("New Conversation"));

        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    @Test
    void testGetAllSessions() throws Exception {
        List<Session> sessions = new ArrayList<>();

        Session session1 = new Session();
        session1.setSessionId("session-1");
        session1.setTitle("Conversation 1");

        Session session2 = new Session();
        session2.setSessionId("session-2");
        session2.setTitle("Conversation 2");

        sessions.add(session1);
        sessions.add(session2);

        when(sessionRepository.findAllByOrderByUpdatedAtDesc())
            .thenReturn(sessions);

        mockMvc.perform(get("/api/agent/sessions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].sessionId").value("session-1"));

        verify(sessionRepository, times(1)).findAllByOrderByUpdatedAtDesc();
    }

    @Test
    void testDeleteSession() throws Exception {
        doNothing().when(sessionRepository).deleteBySessionId(testSessionId);
        doNothing().when(eventService).deleteEventsBySessionId(testSessionId);

        mockMvc.perform(delete("/api/agent/sessions/" + testSessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(sessionRepository, times(1)).deleteBySessionId(testSessionId);
        verify(eventService, times(1)).deleteEventsBySessionId(testSessionId);
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
    void testMissingSessionId() throws Exception {
        String messageJson = """
            {
                "message": "Test message"
            }
            """;

        mockMvc.perform(post("/api/agent/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content(messageJson))
            .andExpect(status().isBadRequest());
    }
}
