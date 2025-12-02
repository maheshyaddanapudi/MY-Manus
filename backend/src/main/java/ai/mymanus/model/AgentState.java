package ai.mymanus.model;

import jakarta.persistence.*;
import ai.mymanus.config.JsonMapConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "agent_states")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String sessionId;

    @Column(length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.IDLE;

    @Column(nullable = false)
    @Builder.Default
    private Integer iteration = 0;

    @Column(length = 500)
    private String currentTask;

    @Column(length = 2000)
    private String lastError;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "json")
    private Map<String, Object> executionContext;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "json")
    private Map<String, Object> metadata;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Agent execution status
     */
    public enum Status {
        IDLE,           // Session created, not started
        RUNNING,        // Agent actively executing
        WAITING_INPUT,  // Paused, waiting for user input
        COMPLETED,      // Task finished successfully
        ERROR           // Execution failed
    }
}
