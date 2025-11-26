package ai.mymanus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * Document chunk with vector embedding for RAG
 */
@Entity
@Table(name = "document_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent document
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    /**
     * Chunk text content
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Chunk index in document
     */
    private int chunkIndex;

    /**
     * Start position in original document
     */
    private int startPosition;

    /**
     * End position in original document
     */
    private int endPosition;

    /**
     * Vector embedding (using pgvector or stored as JSON array)
     * In production, use pgvector extension for efficient similarity search
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private double[] embedding;

    /**
     * Additional metadata
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
