package ai.mymanus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Event in the event stream.
 * Manus AI pattern: [UserMessage → AgentAction → Observation → AgentAction → Observation → ...]
 *
 * Event Stream is the primary data structure for agent execution.
 * Each event is immutable and appended sequentially.
 */
@Entity
@Table(name = "events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_state_id", nullable = false)
    private AgentState agentState;

    /**
     * Type of event in the stream
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    /**
     * Iteration number (starts at 1)
     */
    @Column(nullable = false)
    private Integer iteration;

    /**
     * Sequence number within iteration (for ordering)
     */
    @Column(nullable = false)
    private Integer sequence;

    /**
     * Content of the event (message text, code, output, etc.)
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Structured data associated with the event
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    /**
     * Timestamp when event was created
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    /**
     * Duration in milliseconds (for actions and observations)
     */
    private Long durationMs;

    /**
     * Success status (for actions and observations)
     */
    private Boolean success;

    /**
     * Error message if action/observation failed
     */
    @Column(columnDefinition = "TEXT")
    private String error;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    /**
     * Event types in the stream
     */
    public enum EventType {
        /**
         * User sends a message to the agent
         */
        USER_MESSAGE,

        /**
         * Agent generates a thought/reasoning
         */
        AGENT_THOUGHT,

        /**
         * Agent decides on an action (tool to execute)
         */
        AGENT_ACTION,

        /**
         * Observation from action execution (result)
         */
        OBSERVATION,

        /**
         * Agent's final response to user
         */
        AGENT_RESPONSE,

        /**
         * System message or status update
         */
        SYSTEM,

        /**
         * Error occurred during execution
         */
        ERROR
    }
}
