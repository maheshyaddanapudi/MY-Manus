package ai.mymanus.repository;

import ai.mymanus.model.AgentState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentStateRepository extends JpaRepository<AgentState, UUID> {
    Optional<AgentState> findBySessionId(String sessionId);
    void deleteBySessionId(String sessionId);
}
