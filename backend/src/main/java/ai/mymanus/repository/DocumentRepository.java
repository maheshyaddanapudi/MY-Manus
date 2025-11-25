package ai.mymanus.repository;

import ai.mymanus.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Find all documents for a session
     */
    List<Document> findBySessionIdOrderByUploadedAtDesc(String sessionId);

    /**
     * Count documents in a session
     */
    long countBySessionId(String sessionId);

    /**
     * Delete all documents for a session
     */
    void deleteBySessionId(String sessionId);
}
