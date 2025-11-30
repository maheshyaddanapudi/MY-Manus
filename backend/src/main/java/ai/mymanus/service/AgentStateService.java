package ai.mymanus.service;

import ai.mymanus.model.AgentState;
import ai.mymanus.model.Message;
import ai.mymanus.model.ToolExecution;
import ai.mymanus.repository.AgentStateRepository;
import ai.mymanus.repository.MessageRepository;
import ai.mymanus.repository.ToolExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing agent state persistence.
 * Handles sessions, messages, and tool executions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentStateService {

    private final AgentStateRepository agentStateRepository;
    private final MessageRepository messageRepository;
    private final ToolExecutionRepository toolExecutionRepository;

    /**
     * Create a new agent session with optional title
     */
    @Transactional
    public AgentState createSession(String sessionId, String title) {
        log.info("Creating new session: {} with title: {}", sessionId, title);

        AgentState state = AgentState.builder()
                .sessionId(sessionId != null ? sessionId : UUID.randomUUID().toString())
                .title(title != null ? title : "New Conversation")
                .executionContext(new HashMap<>())
                .metadata(new HashMap<>())
                .build();

        return agentStateRepository.save(state);
    }

    /**
     * Create a new agent session with default title
     */
    @Transactional
    public AgentState createSession(String sessionId) {
        return createSession(sessionId, null);
    }

    /**
     * Get or create session by ID
     */
    @Transactional
    public AgentState getOrCreateSession(String sessionId) {
        return agentStateRepository.findBySessionId(sessionId)
                .orElseGet(() -> createSession(sessionId));
    }

    /**
     * Get or create state by session ID (alias for getOrCreateSession)
     */
    @Transactional
    public AgentState getOrCreateState(String sessionId) {
        return getOrCreateSession(sessionId);
    }

    /**
     * Get session by ID
     */
    public AgentState getSession(String sessionId) {
        return agentStateRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }

    /**
     * Update agent state
     */
    @Transactional
    public AgentState updateState(AgentState state) {
        return agentStateRepository.save(state);
    }

    /**
     * Update execution context (Python variables)
     */
    @Transactional
    public void updateExecutionContext(String sessionId, Map<String, Object> context) {
        AgentState state = getSession(sessionId);
        state.setExecutionContext(context);
        agentStateRepository.save(state);
        log.info("Updated execution context for session: {}", sessionId);
    }

    /**
     * Get execution context
     */
    public Map<String, Object> getExecutionContext(String sessionId) {
        AgentState state = getSession(sessionId);
        return state.getExecutionContext() != null
                ? state.getExecutionContext()
                : new HashMap<>();
    }

    /**
     * Add a message to the conversation
     */
    @Transactional
    public Message addMessage(String sessionId, Message.MessageRole role, String content) {
        AgentState state = getSession(sessionId);

        Message message = Message.builder()
                .agentState(state)
                .role(role)
                .content(content)
                .build();

        Message saved = messageRepository.save(message);
        log.info("Added {} message to session: {}", role, sessionId);
        return saved;
    }

    /**
     * Get all messages for a session
     */
    public List<Message> getMessages(String sessionId) {
        AgentState state = getSession(sessionId);
        return messageRepository.findByAgentStateIdOrderByTimestampAsc(state.getId());
    }

    /**
     * Record a tool execution
     */
    @Transactional
    public ToolExecution recordToolExecution(String sessionId,
                                              String toolName,
                                              Map<String, Object> parameters,
                                              Map<String, Object> result,
                                              ToolExecution.ExecutionStatus status,
                                              Integer durationMs) {
        AgentState state = getSession(sessionId);

        ToolExecution execution = ToolExecution.builder()
                .agentState(state)
                .toolName(toolName)
                .parameters(parameters)
                .result(result)
                .status(status)
                .durationMs(durationMs)
                .build();

        ToolExecution saved = toolExecutionRepository.save(execution);
        log.info("Recorded tool execution: {} - {}", toolName, status);
        return saved;
    }

    /**
     * Get all tool executions for a session
     */
    public List<ToolExecution> getToolExecutions(String sessionId) {
        AgentState state = getSession(sessionId);
        return toolExecutionRepository.findByAgentStateIdOrderByTimestampAsc(state.getId());
    }

    /**
     * Update session metadata
     */
    @Transactional
    public void updateMetadata(String sessionId, String key, Object value) {
        AgentState state = getSession(sessionId);
        Map<String, Object> metadata = state.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        state.setMetadata(metadata);
        agentStateRepository.save(state);
    }

    /**
     * Get session metadata
     */
    public Map<String, Object> getMetadata(String sessionId) {
        AgentState state = getSession(sessionId);
        return state.getMetadata() != null
                ? state.getMetadata()
                : new HashMap<>();
    }

    /**
     * Delete a session and all associated data
     */
    @Transactional
    public void deleteSession(String sessionId) {
        AgentState state = getSession(sessionId);
        agentStateRepository.delete(state);
        log.info("Deleted session: {}", sessionId);
    }

    /**
     * Get conversation history as strings
     */
    public List<String> getConversationHistory(String sessionId) {
        List<Message> messages = getMessages(sessionId);
        return messages.stream()
                .map(Message::getContent)
                .toList();
    }

    /**
     * List all sessions ordered by most recently updated
     */
    public List<AgentState> listAllSessions() {
        return agentStateRepository.findAll().stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .toList();
    }

    /**
     * Update session title
     */
    @Transactional
    public void updateSessionTitle(String sessionId, String title) {
        AgentState state = getSession(sessionId);
        state.setTitle(title);
        agentStateRepository.save(state);
        log.info("Updated session {} title to: {}", sessionId, title);
    }

    /**
     * Auto-generate and set session title from first user message
     */
    @Transactional
    public void autoGenerateTitle(String sessionId, String firstMessage) {
        AgentState state = getSession(sessionId);

        // Only auto-generate if still using default title
        if (state.getTitle() == null || state.getTitle().equals("New Conversation")) {
            String title = generateTitleFromMessage(firstMessage);
            state.setTitle(title);
            agentStateRepository.save(state);
            log.info("Auto-generated title for session {}: {}", sessionId, title);
        }
    }

    /**
     * Generate a short title from a message (max 50 chars)
     */
    private String generateTitleFromMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "New Conversation";
        }

        // Take first line or first 50 characters
        String title = message.split("\n")[0];
        if (title.length() > 50) {
            title = title.substring(0, 47) + "...";
        }

        return title;
    }
}
