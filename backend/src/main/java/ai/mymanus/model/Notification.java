package ai.mymanus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User notification entity for in-app and browser notifications
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_session_id", columnList = "sessionId"),
    @Index(name = "idx_user_id_read", columnList = "userId,isRead")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User ID (for multi-user support)
     */
    private String userId;

    /**
     * Session ID this notification belongs to
     */
    @Column(nullable = false)
    private String sessionId;

    /**
     * Notification type
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /**
     * Notification title
     */
    @Column(nullable = false)
    private String title;

    /**
     * Notification message
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    /**
     * Priority level
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    /**
     * Whether the notification has been read
     */
    @Builder.Default
    private boolean isRead = false;

    /**
     * Whether to show as browser notification
     */
    @Builder.Default
    private boolean browserNotification = true;

    /**
     * Action URL (optional - for navigation on click)
     */
    private String actionUrl;

    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Read timestamp
     */
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum NotificationType {
        TASK_COMPLETED,      // Task execution completed
        TASK_FAILED,         // Task execution failed
        AGENT_WAITING,       // Agent waiting for user input
        PLAN_ADJUSTED,       // Plan was adjusted
        TOOL_ERROR,          // Tool execution error
        SYSTEM,              // System notification
        INFO                 // General information
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    /**
     * Mark notification as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
