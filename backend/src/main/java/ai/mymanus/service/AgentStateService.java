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
     * Create a new agent session
     */
    @Transactional
    public AgentState createSession(String sessionId) {
        log.info("Creating new session: {}", sessionId);

        AgentState state = AgentState.builder()
                .sessionId(sessionId != null ? sessionId : UUID.randomUUID().toString())
                .executionContext(new HashMap<>())
                .metadata(new HashMap<>())
                .build();

        return agentStateRepository.save(state);
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
     * Get session by ID
     */
    public AgentState getSession(String sessionId) {
        return agentStateRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
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
}
