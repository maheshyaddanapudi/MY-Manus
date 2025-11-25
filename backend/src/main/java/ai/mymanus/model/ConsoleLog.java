package ai.mymanus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Browser console log entry
 */
@Entity
@Table(name = "console_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsoleLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Session ID
     */
    @Column(nullable = false)
    private String sessionId;

    /**
     * Browser session ID (from BrowserSession)
     */
    private String browserSessionId;

    /**
     * Timestamp
     */
    private LocalDateTime timestamp;

    /**
     * Log level
     */
    @Enumerated(EnumType.STRING)
    private LogLevel level;

    /**
     * Log message
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Source file
     */
    private String source;

    /**
     * Line number in source
     */
    private Integer lineNumber;

    public enum LogLevel {
        LOG, INFO, WARN, ERROR, DEBUG
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
