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
import java.util.List;
import java.util.Map;

/**
 * Document entity for RAG/Knowledge Base
 */
@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Session this document belongs to
     */
    @Column(nullable = false)
    private String sessionId;

    /**
     * Original filename
     */
    @Column(nullable = false)
    private String filename;

    /**
     * Document type (pdf, txt, md, code, etc.)
     */
    private String type;

    /**
     * Full text content
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Metadata (author, created date, etc.)
     */
    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "json")
    private Map<String, Object> metadata;

    /**
     * Upload timestamp
     */
    private LocalDateTime uploadedAt;

    /**
     * File size in bytes
     */
    private Long fileSize;

    /**
     * Whether embeddings have been generated
     */
    private boolean indexed;

    /**
     * Document chunks for RAG retrieval
     */
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentChunk> chunks;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
