package ai.mymanus.repository;

import ai.mymanus.model.ConsoleLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsoleLogRepository extends JpaRepository<ConsoleLog, Long> {

    /**
     * Find all console logs for a session
     */
    List<ConsoleLog> findBySessionIdOrderByTimestampDesc(String sessionId);

    /**
     * Find console logs by browser session
     */
    List<ConsoleLog> findByBrowserSessionIdOrderByTimestampDesc(String browserSessionId);

    /**
     * Delete all console logs for a session
     */
    void deleteBySessionId(String sessionId);
}
