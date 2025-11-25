package ai.mymanus.repository;

import ai.mymanus.model.NetworkRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetworkRequestRepository extends JpaRepository<NetworkRequest, Long> {

    /**
     * Find all network requests for a session
     */
    List<NetworkRequest> findBySessionIdOrderByTimestampDesc(String sessionId);

    /**
     * Find network requests by browser session
     */
    List<NetworkRequest> findByBrowserSessionIdOrderByTimestampDesc(String browserSessionId);

    /**
     * Delete all network requests for a session
     */
    void deleteBySessionId(String sessionId);
}
