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

/**
 * Browser network request entry
 */
@Entity
@Table(name = "network_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkRequest {

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
     * HTTP method
     */
    private String method;

    /**
     * Request URL
     */
    @Column(columnDefinition = "TEXT")
    private String url;

    /**
     * HTTP status code
     */
    private Integer status;

    /**
     * Status text
     */
    private String statusText;

    /**
     * Resource type (document, script, stylesheet, image, etc.)
     */
    private String type;

    /**
     * Response size in bytes
     */
    private Long size;

    /**
     * Request duration in milliseconds
     */
    private Long duration;

    /**
     * Request headers
     */
    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "json")
    private Map<String, String> requestHeaders;

    /**
     * Response headers
     */
    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "json")
    private Map<String, String> responseHeaders;

    /**
     * Request body (for POST, PUT, etc.)
     */
    @Column(columnDefinition = "TEXT")
    private String requestBody;

    /**
     * Response body
     */
    @Column(columnDefinition = "TEXT")
    private String responseBody;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
