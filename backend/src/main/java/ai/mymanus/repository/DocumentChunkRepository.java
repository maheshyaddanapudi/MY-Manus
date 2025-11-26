package ai.mymanus.repository;

import ai.mymanus.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /**
     * Find all chunks for a document
     */
    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(Long documentId);

    /**
     * Find chunks by session (through document)
     */
    @Query("SELECT c FROM DocumentChunk c WHERE c.document.sessionId = :sessionId")
    List<DocumentChunk> findBySessionId(@Param("sessionId") String sessionId);

    /**
     * Count chunks for a document
     */
    long countByDocumentId(Long documentId);
}
